package com.jin.subwayalarm.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.jin.subwayalarm.R
import com.jin.subwayalarm.alarm.AlarmContent
import com.jin.subwayalarm.alarm.AlarmController
import com.jin.subwayalarm.data.repository.BoardingCandidateRepository
import com.jin.subwayalarm.data.repository.JourneyPlanRepository
import com.jin.subwayalarm.data.repository.PositionFetchResult
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TrainPositionRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.AlarmSetting
import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import com.jin.subwayalarm.domain.usecase.AccelStopCounter
import com.jin.subwayalarm.domain.usecase.ConfirmBoardedTrainUseCase
import com.jin.subwayalarm.domain.usecase.HybridCoordinator
import com.jin.subwayalarm.domain.usecase.HybridInput
import com.jin.subwayalarm.domain.usecase.RecptnDtCorrector
import com.jin.subwayalarm.domain.usecase.TrackingStepInput
import com.jin.subwayalarm.domain.usecase.TrackingStepUseCase
import com.jin.subwayalarm.tracking.sensor.LinearAccelerationSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 추적 Foreground Service (FGS type = dataSync).
 * API 폴링 + (옵션) 가속도계 하이브리드 + 수동 재매칭 인텐트.
 */
@AndroidEntryPoint
class TrackingService : LifecycleService() {

    @Inject lateinit var stationRepository: StationRepository
    @Inject lateinit var tripSessionRepository: TripSessionRepository
    @Inject lateinit var journeyPlanRepository: JourneyPlanRepository
    @Inject lateinit var trainPositionRepository: TrainPositionRepository
    @Inject lateinit var boardingCandidateRepository: BoardingCandidateRepository
    @Inject lateinit var trackingStep: TrackingStepUseCase
    @Inject lateinit var confirmBoardedTrain: ConfirmBoardedTrainUseCase
    @Inject lateinit var alarmController: AlarmController
    @Inject lateinit var linearAccelerationSource: LinearAccelerationSource

    private var trackingJob: Job? = null
    private var sensorJob: Job? = null
    private var consecutiveFailures = 0
    private val accelCounter = AccelStopCounter()
    private var sensorEnabled = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_STOP -> {
                trackingJob?.cancel()
                stopSensor()
                lifecycleScope.launch {
                    tripSessionRepository.clear()
                    journeyPlanRepository.clear()
                    boardingCandidateRepository.clear()
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_CONFIRM_TRAIN -> {
                val trainNo = intent.getStringExtra(EXTRA_TRAIN_NO)
                if (!trainNo.isNullOrBlank()) {
                    lifecycleScope.launch { confirmTrain(trainNo) }
                }
                return START_STICKY
            }
            ACTION_REQUEST_REMATCH -> {
                lifecycleScope.launch { publishRematchCandidates() }
                return START_STICKY
            }
        }

        startForeground(TRACKING_NOTIFICATION_ID, buildNotification("추적 준비 중"))
        trackingJob?.cancel()
        trackingJob = lifecycleScope.launch { runTrackingLoop() }
        return START_REDELIVER_INTENT
    }

    private suspend fun confirmTrain(trainNo: String) {
        val session = tripSessionRepository.current() ?: return
        val updated = confirmBoardedTrain(session, trainNo, System.currentTimeMillis())
        tripSessionRepository.save(updated)
        boardingCandidateRepository.clear()
        accelCounter.resync()
    }

    private suspend fun publishRematchCandidates() {
        val session = tripSessionRepository.current() ?: return
        val graph = stationRepository.stationGraph(session.lineId) ?: return
        val lineMeta = stationRepository.lineMetas().firstOrNull { it.lineId == session.lineId } ?: return
        when (val fetch = trainPositionRepository.fetchRealtimePosition(lineMeta.lineName)) {
            is PositionFetchResult.Success -> {
                val candidates = trackingStep.listRematchCandidates(
                    TrackingStepInput(
                        graph = graph,
                        session = session,
                        positions = fetch.positions,
                        nowEpochMillis = System.currentTimeMillis(),
                        polarity = lineMeta.directionPolarity,
                        excludedTerminusNames = lineMeta.excludedTerminusNames,
                    ),
                )
                boardingCandidateRepository.publish(candidates)
            }
            else -> Unit
        }
    }

    private suspend fun runTrackingLoop() {
        while (lifecycleScope.isActive) {
            val session = tripSessionRepository.current()
            if (session == null || session.state == TripState.DONE) break

            val graph = stationRepository.stationGraph(session.lineId)
            val lineMeta = stationRepository.lineMetas().firstOrNull { it.lineId == session.lineId }
            if (graph == null || lineMeta == null) break

            when (val fetch = trainPositionRepository.fetchRealtimePosition(lineMeta.lineName)) {
                is PositionFetchResult.Success -> {
                    consecutiveFailures = 0
                    val now = System.currentTimeMillis()
                    val direction = graph.directionBetween(session.originStatnId, session.destStatnId)
                    val boarded = session.boardedTrainNo
                    val train = boarded?.let { no -> fetch.positions.firstOrNull { it.trainNo == no } }

                    val apiCorrectedId = if (train != null) {
                        RecptnDtCorrector.correct(
                            graph = graph,
                            rawStatnId = train.statnId,
                            direction = direction,
                            recptnDtEpochMillis = train.recptnDtEpochMillis,
                            nowEpochMillis = now,
                            destId = session.destStatnId,
                        ).statnId
                    } else {
                        null
                    }

                    val hybrid = if (session.boardedTrainNo != null) {
                        HybridCoordinator.coordinate(
                            HybridInput(
                                graph = graph,
                                direction = direction,
                                lastKnownStatnId = session.currentStatnId,
                                apiCorrectedStatnId = apiCorrectedId,
                                accelStopCountSinceResync = accelCounter.stopCount,
                                destStatnId = session.destStatnId,
                            ),
                        )
                    } else {
                        null
                    }

                    if (hybrid?.shouldResyncCounter == true) {
                        accelCounter.resync()
                    }
                    setSensorEnabled(hybrid?.pauseSensor == false && session.boardedTrainNo != null)

                    val accelEstimate = if (train == null && hybrid != null && hybrid.mode == TrackingMode.ACCEL) {
                        hybrid.estimatedStatnId
                    } else {
                        null
                    }

                    val result = trackingStep(
                        TrackingStepInput(
                            graph = graph,
                            session = session,
                            positions = fetch.positions,
                            nowEpochMillis = now,
                            polarity = lineMeta.directionPolarity,
                            excludedTerminusNames = lineMeta.excludedTerminusNames,
                            accelEstimatedStatnId = accelEstimate,
                        ),
                    )
                    tripSessionRepository.save(result.updatedSession)
                    boardingCandidateRepository.publish(result.boardingCandidates)

                    val remaining = graph.remainingStops(result.updatedSession.currentStatnId, session.destStatnId)
                    updateNotification(buildNotification(statusText(result.updatedSession, remaining)))

                    val alert = result.alert
                    if (alert != null) {
                        stopSensor()
                        val stationName = graph.station(alert.alertStationId)?.name.orEmpty()
                        val nextLeg = journeyPlanRepository.current()?.takeIf { it.hasNextLeg }
                            ?.let { it.legs[it.currentLegIndex + 1] }
                        val content = if (nextLeg != null) {
                            AlarmContent.Transfer(transferStationName = stationName, nextLineName = nextLeg.lineName)
                        } else {
                            AlarmContent.FinalDestination(destinationName = stationName)
                        }
                        alarmController.trigger(content, AlarmSetting())
                        break
                    }
                    val intervalSeconds = if (result.updatedSession.state == TripState.MATCHING) {
                        PollingIntervalPolicy.MATCHING_INTERVAL_SECONDS
                    } else {
                        PollingIntervalPolicy.intervalSeconds(remaining)
                    }
                    delay(intervalSeconds * 1000L)
                }
                is PositionFetchResult.RateLimited, is PositionFetchResult.Error -> {
                    consecutiveFailures++
                    // 네트워크 실패 시에도 센서 추정으로 한 틱 시도
                    val sessionNow = tripSessionRepository.current()
                    if (sessionNow?.boardedTrainNo != null) {
                        setSensorEnabled(true)
                        val graph2 = stationRepository.stationGraph(sessionNow.lineId)
                        if (graph2 != null) {
                            val direction = graph2.directionBetween(sessionNow.originStatnId, sessionNow.destStatnId)
                            val hybrid = HybridCoordinator.coordinate(
                                HybridInput(
                                    graph = graph2,
                                    direction = direction,
                                    lastKnownStatnId = sessionNow.currentStatnId,
                                    apiCorrectedStatnId = null,
                                    accelStopCountSinceResync = accelCounter.stopCount,
                                    destStatnId = sessionNow.destStatnId,
                                ),
                            )
                            val result = trackingStep(
                                TrackingStepInput(
                                    graph = graph2,
                                    session = sessionNow,
                                    positions = emptyList(),
                                    nowEpochMillis = System.currentTimeMillis(),
                                    polarity = lineMeta.directionPolarity,
                                    excludedTerminusNames = lineMeta.excludedTerminusNames,
                                    accelEstimatedStatnId = hybrid.estimatedStatnId,
                                ),
                            )
                            tripSessionRepository.save(result.updatedSession)
                            if (result.alert != null) {
                                stopSensor()
                                val stationName = graph2.station(result.alert.alertStationId)?.name.orEmpty()
                                alarmController.trigger(
                                    AlarmContent.FinalDestination(destinationName = stationName),
                                    AlarmSetting(),
                                )
                                break
                            }
                        }
                    }
                    updateNotification(buildNotification("갱신 지연 중…"))
                    delay(PollingIntervalPolicy.backoffSeconds(consecutiveFailures) * 1000L)
                }
            }
        }
        stopSensor()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun setSensorEnabled(enabled: Boolean) {
        if (enabled == sensorEnabled) return
        sensorEnabled = enabled
        sensorJob?.cancel()
        if (!enabled) return
        sensorJob = lifecycleScope.launch {
            linearAccelerationSource.samples(true).collectLatest { sample ->
                accelCounter.onSample(sample.magnitude, sample.epochMillis)
            }
        }
    }

    private fun stopSensor() {
        sensorEnabled = false
        sensorJob?.cancel()
        sensorJob = null
    }

    private fun statusText(session: TripSession, remaining: Int): String =
        "남은 역 ${remaining}개 · ${if (session.mode == TrackingMode.API) "실시간" else "추정"}"

    private fun buildNotification(text: String): Notification {
        ensureChannel()
        val stopIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, TRACKING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("하차알람 추적 중")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "정지", stopPendingIntent)
            .build()
    }

    private fun updateNotification(notification: Notification) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(TRACKING_NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(TRACKING_CHANNEL_ID, "추적 중", NotificationManager.IMPORTANCE_LOW).apply {
            description = "하차 알람 추적 상태 표시"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val ACTION_STOP = "com.jin.subwayalarm.action.STOP_TRACKING"
        const val ACTION_CONFIRM_TRAIN = "com.jin.subwayalarm.action.CONFIRM_TRAIN"
        const val ACTION_REQUEST_REMATCH = "com.jin.subwayalarm.action.REQUEST_REMATCH"
        const val EXTRA_TRAIN_NO = "trainNo"
        private const val TRACKING_CHANNEL_ID = "tracking"
        private const val TRACKING_NOTIFICATION_ID = 2001

        fun startIntent(context: Context): Intent = Intent(context, TrackingService::class.java)

        fun stopIntent(context: Context): Intent =
            Intent(context, TrackingService::class.java).apply { action = ACTION_STOP }

        fun confirmTrainIntent(context: Context, trainNo: String): Intent =
            Intent(context, TrackingService::class.java).apply {
                action = ACTION_CONFIRM_TRAIN
                putExtra(EXTRA_TRAIN_NO, trainNo)
            }

        fun rematchIntent(context: Context): Intent =
            Intent(context, TrackingService::class.java).apply { action = ACTION_REQUEST_REMATCH }
    }
}

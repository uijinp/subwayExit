package com.jin.subwayalarm.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.jin.subwayalarm.R
import com.jin.subwayalarm.alarm.AlarmContent
import com.jin.subwayalarm.alarm.AlarmController
import com.jin.subwayalarm.data.repository.AlarmSettingsRepository
import com.jin.subwayalarm.data.repository.BoardingCandidateRepository
import com.jin.subwayalarm.data.repository.JourneyPlanRepository
import com.jin.subwayalarm.data.repository.PositionFetchResult
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TrainPositionRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.AlarmSetting
import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.LineMeta
import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import com.jin.subwayalarm.domain.usecase.AccelStopCounter
import com.jin.subwayalarm.domain.usecase.ConfirmBoardedTrainUseCase
import com.jin.subwayalarm.domain.usecase.HybridCoordinator
import com.jin.subwayalarm.domain.usecase.HybridInput
import com.jin.subwayalarm.domain.usecase.NormalizePositionsUseCase
import com.jin.subwayalarm.domain.usecase.RecptnDtCorrector
import com.jin.subwayalarm.domain.usecase.TrackingStepInput
import com.jin.subwayalarm.domain.usecase.TrackingStepResult
import com.jin.subwayalarm.domain.usecase.TrackingStepUseCase
import com.jin.subwayalarm.tracking.sensor.LinearAccelerationSource
import com.jin.subwayalarm.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 추적 Foreground Service (FGS type = dataSync).
 * API 폴링 + (옵션) 가속도계 하이브리드 + 수동 재매칭 인텐트.
 *
 * 2026-10-12 재검토(HANDOFF §C) 반영:
 * - 추적 중엔 PARTIAL_WAKE_LOCK 을 잡는다. FGS 는 CPU 를 깨워두지 않아, 화면이 꺼지면
 *   delay() 가 멈춰 폴링이 늦어지고 알람을 놓쳤다.
 * - 틱마다 예외를 가둔다. 예전엔 한 번의 예외로 서비스가 죽고 START_REDELIVER_INTENT 로
 *   재시작될 때마다 다시 죽었다.
 * - 알람을 울린 뒤에도 확인할 때까지 포그라운드로 남아 진동이 끊기지 않게 프로세스를 지킨다.
 */
@AndroidEntryPoint
class TrackingService : LifecycleService() {

    @Inject lateinit var stationRepository: StationRepository
    @Inject lateinit var tripSessionRepository: TripSessionRepository
    @Inject lateinit var journeyPlanRepository: JourneyPlanRepository
    @Inject lateinit var trainPositionRepository: TrainPositionRepository
    @Inject lateinit var boardingCandidateRepository: BoardingCandidateRepository
    @Inject lateinit var alarmSettingsRepository: AlarmSettingsRepository
    @Inject lateinit var trackingStep: TrackingStepUseCase
    @Inject lateinit var normalizePositions: NormalizePositionsUseCase
    @Inject lateinit var confirmBoardedTrain: ConfirmBoardedTrainUseCase
    @Inject lateinit var alarmController: AlarmController
    @Inject lateinit var linearAccelerationSource: LinearAccelerationSource

    private var trackingJob: Job? = null
    private var sensorJob: Job? = null
    private var ringingTimeoutJob: Job? = null
    private var consecutiveFailures = 0
    private var consecutiveErrors = 0
    private val accelCounter = AccelStopCounter()

    /**
     * 가속도 추정의 기준점 — API 가 마지막으로 확인한(또는 탑승을 확정한) 열차 위치.
     * 예전엔 이미 추정으로 전진시킨 현재역에 누적 정차 횟수를 매 폴링마다 다시 더해서,
     * 정차 1번에 폴링 3번이면 3역을 전진시켜 조기 오알람이 났다.
     */
    private var accelAnchorStatnId: String? = null
    private var sensorEnabled = false
    private var wakeLock: PowerManager.WakeLock? = null

    private enum class LoopEnd { FINISHED, RINGING }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_STOP -> {
                stopTracking()
                return START_NOT_STICKY
            }
            ACTION_CONFIRM_TRAIN -> {
                ensureForegroundAndLoop()
                val trainNo = intent.getStringExtra(EXTRA_TRAIN_NO)
                if (!trainNo.isNullOrBlank()) {
                    lifecycleScope.launch { confirmTrain(trainNo) }
                }
                return START_STICKY
            }
            ACTION_REQUEST_REMATCH -> {
                ensureForegroundAndLoop()
                lifecycleScope.launch { publishRematchCandidates() }
                return START_STICKY
            }
        }

        startForeground(TRACKING_NOTIFICATION_ID, buildNotification("추적 준비 중"))
        startLoop()
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        trackingJob?.cancel()
        ringingTimeoutJob?.cancel()
        stopSensor()
        releaseWakeLock()
        super.onDestroy()
    }

    /**
     * 화면의 "이 열차예요"·"다른 열차예요" 버튼은 startForegroundService 로 온다. 서비스가 이미
     * 내려가 있었다면 startForeground 를 부르지 않으면 시스템이 앱을 죽이므로 반드시 부르고,
     * 추적 루프도 살린다.
     */
    private fun ensureForegroundAndLoop() {
        startForeground(TRACKING_NOTIFICATION_ID, buildNotification("추적 중"))
        if (trackingJob?.isActive != true) startLoop()
    }

    private fun startLoop() {
        trackingJob?.cancel()
        ringingTimeoutJob?.cancel()
        consecutiveFailures = 0
        consecutiveErrors = 0
        accelAnchorStatnId = null
        accelCounter.resync()
        acquireWakeLock()
        trackingJob = lifecycleScope.launch { runTrackingLoop() }
    }

    private fun stopTracking() {
        trackingJob?.cancel()
        ringingTimeoutJob?.cancel()
        stopSensor()
        releaseWakeLock()
        alarmController.dismiss()
        lifecycleScope.launch {
            // stopSelf 가 scope 를 취소하기 전에 정리가 끝나도록.
            withContext(NonCancellable) {
                tripSessionRepository.clear()
                journeyPlanRepository.clear()
                boardingCandidateRepository.clear()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun confirmTrain(trainNo: String) {
        val session = tripSessionRepository.current() ?: return
        val updated = confirmBoardedTrain(session, trainNo, System.currentTimeMillis())
        tripSessionRepository.save(updated)
        boardingCandidateRepository.clear()
        accelAnchorStatnId = updated.currentStatnId
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
                        positions = normalizePositions(fetch.positions, graph.stations),
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
        // 앱 업데이트 직후 화면보다 서비스가 먼저 살아난 경우에도 최신 시드(극성·순서)로 추적하도록.
        stationRepository.ensureSeeded()
        var end = LoopEnd.FINISHED
        while (currentCoroutineContext().isActive) {
            val session = tripSessionRepository.current()
            if (session == null || session.state == TripState.DONE) break
            if (session.state == TripState.ALERTED) {
                end = LoopEnd.RINGING
                break
            }
            val graph = stationRepository.stationGraph(session.lineId)
            val lineMeta = stationRepository.lineMetas().firstOrNull { it.lineId == session.lineId }
            if (graph == null || lineMeta == null || !graph.isValidTrip(session.originStatnId, session.destStatnId)) {
                abandonBrokenSession()
                break
            }

            val nextDelayMs = try {
                tick(session, graph, lineMeta)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                consecutiveErrors++
                Log.w(TAG, "추적 틱 실패 ($consecutiveErrors 회 연속)", e)
                if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                    abandonBrokenSession()
                    break
                }
                ERROR_RETRY_MS
            }
            if (nextDelayMs == null) {
                end = LoopEnd.RINGING
                break
            }
            delay(nextDelayMs)
        }
        // 여기는 루프가 스스로 끝났을 때만 온다(다른 루프로 교체되며 취소되면 오지 않음).
        stopSensor()
        releaseWakeLock()
        when (end) {
            LoopEnd.RINGING -> enterRingingState()
            LoopEnd.FINISHED -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    /** 한 틱. 다음 폴링까지 기다릴 ms, 알람을 울렸으면 null. */
    private suspend fun tick(session: TripSession, graph: StationGraph, lineMeta: LineMeta): Long? {
        val setting = alarmSettingsRepository.current()
        if (accelAnchorStatnId == null && session.boardedTrainNo != null) {
            accelAnchorStatnId = session.currentStatnId
            accelCounter.resync()
        }
        if (!lineMeta.supportsRealtime) return sensorOnlyTick(session, graph, lineMeta, setting)

        return when (val fetch = trainPositionRepository.fetchRealtimePosition(lineMeta.lineName)) {
            is PositionFetchResult.Success -> {
                consecutiveFailures = 0
                realtimeTick(session, graph, lineMeta, setting, fetch)
            }
            is PositionFetchResult.RateLimited, is PositionFetchResult.Error -> {
                consecutiveFailures++
                offlineTick(session, graph, lineMeta, setting)
            }
        }
    }

    private suspend fun realtimeTick(
        session: TripSession,
        graph: StationGraph,
        lineMeta: LineMeta,
        setting: AlarmSetting,
        fetch: PositionFetchResult.Success,
    ): Long? {
        val now = System.currentTimeMillis()
        // API statnId 가 시드와 다른 역이 있어(광역선) 경계에서 한 번 정규화한다.
        val positions = normalizePositions(fetch.positions, graph.stations)
        val direction = graph.directionBetween(session.originStatnId, session.destStatnId)
        val train = session.boardedTrainNo?.let { no -> positions.firstOrNull { it.trainNo == no } }

        val apiStatnId = train?.takeIf { graph.contains(it.statnId) }?.let {
            RecptnDtCorrector.correct(
                graph = graph,
                rawStatnId = it.statnId,
                direction = direction,
                recptnDtEpochMillis = it.recptnDtEpochMillis,
                nowEpochMillis = now,
                destId = session.destStatnId,
            ).statnId
        }
        if (apiStatnId != null) {
            accelAnchorStatnId = apiStatnId
            accelCounter.resync()
        }
        val trainLost = session.boardedTrainNo != null && train == null
        setSensorEnabled(trainLost)

        val result = trackingStep(
            TrackingStepInput(
                graph = graph,
                session = session,
                positions = positions,
                nowEpochMillis = now,
                polarity = lineMeta.directionPolarity,
                leadStations = setting.leadStations,
                excludedTerminusNames = lineMeta.excludedTerminusNames,
                accelEstimatedStatnId = if (trainLost) estimateByAccel(graph, direction, session) else null,
            ),
        )
        if (session.boardedTrainNo == null && result.updatedSession.boardedTrainNo != null) {
            // 방금 탑승 확정 — 가속도 추정 기준점을 출발역으로.
            accelAnchorStatnId = result.updatedSession.currentStatnId
            accelCounter.resync()
        }
        return finishTick(graph, result, setting)
    }

    /** 네트워크 실패 시 — 탑승 확정 상태면 센서 추정으로 한 틱 시도. */
    private suspend fun offlineTick(
        session: TripSession,
        graph: StationGraph,
        lineMeta: LineMeta,
        setting: AlarmSetting,
    ): Long? {
        updateNotification(buildNotification("갱신 지연 중… 잠시 후 다시 확인합니다"))
        if (session.boardedTrainNo != null) {
            setSensorEnabled(true)
            val direction = graph.directionBetween(session.originStatnId, session.destStatnId)
            val result = trackingStep(
                TrackingStepInput(
                    graph = graph,
                    session = session,
                    positions = emptyList(),
                    nowEpochMillis = System.currentTimeMillis(),
                    polarity = lineMeta.directionPolarity,
                    leadStations = setting.leadStations,
                    excludedTerminusNames = lineMeta.excludedTerminusNames,
                    accelEstimatedStatnId = estimateByAccel(graph, direction, session),
                ),
            )
            tripSessionRepository.save(result.updatedSession)
            result.alert?.let {
                fireAlarm(graph, result.updatedSession, it.alertStationId, setting)
                return null
            }
        }
        consecutiveErrors = 0
        return PollingIntervalPolicy.backoffSeconds(consecutiveFailures) * 1000L
    }

    /**
     * 인천1·2·김포·의정부·용인 등 실시간 위치를 주지 않는 노선. 열차를 특정할 방법이 없으니
     * 출발과 동시에 탑승한 것으로 보고 가속도 정차 횟수로만 위치를 추정한다.
     * 예전엔 탑승 확정이 영영 안 돼서 이 구간에선 알람이 울리지 않았고 다음 구간으로도 못 넘어갔다.
     */
    private suspend fun sensorOnlyTick(
        session: TripSession,
        graph: StationGraph,
        lineMeta: LineMeta,
        setting: AlarmSetting,
    ): Long? {
        var current = session
        if (current.boardedTrainNo == null) {
            current = current.copy(
                boardedTrainNo = SENSOR_ONLY_TRAIN_NO,
                state = TripState.TRACKING,
                mode = TrackingMode.ACCEL,
                currentStatnId = current.originStatnId,
                lastSyncAtEpochMillis = System.currentTimeMillis(),
            )
            tripSessionRepository.save(current)
            accelAnchorStatnId = current.originStatnId
            accelCounter.resync()
        }
        setSensorEnabled(true)
        val direction = graph.directionBetween(current.originStatnId, current.destStatnId)
        val result = trackingStep(
            TrackingStepInput(
                graph = graph,
                session = current,
                positions = emptyList(),
                nowEpochMillis = System.currentTimeMillis(),
                polarity = lineMeta.directionPolarity,
                leadStations = setting.leadStations,
                excludedTerminusNames = lineMeta.excludedTerminusNames,
                accelEstimatedStatnId = estimateByAccel(graph, direction, current),
            ),
        )
        return finishTick(graph, result, setting, statusOverride = "실시간 정보가 없는 구간 · 정차 횟수로 추정 중")
            ?.let { PollingIntervalPolicy.MATCHING_INTERVAL_SECONDS * 1000L }
    }

    private suspend fun finishTick(
        graph: StationGraph,
        result: TrackingStepResult,
        setting: AlarmSetting,
        statusOverride: String? = null,
    ): Long? {
        val updated = result.updatedSession
        tripSessionRepository.save(updated)
        boardingCandidateRepository.publish(result.boardingCandidates)
        val remaining = graph.remainingStops(updated.currentStatnId, updated.destStatnId)
        updateNotification(buildNotification(statusOverride ?: statusText(updated, remaining)))

        result.alert?.let {
            fireAlarm(graph, updated, it.alertStationId, setting)
            return null
        }
        consecutiveErrors = 0
        val seconds = if (updated.state == TripState.MATCHING) {
            PollingIntervalPolicy.MATCHING_INTERVAL_SECONDS
        } else {
            PollingIntervalPolicy.intervalSeconds(remaining)
        }
        return seconds * 1000L
    }

    private fun estimateByAccel(graph: StationGraph, direction: Direction, session: TripSession): String =
        HybridCoordinator.coordinate(
            HybridInput(
                graph = graph,
                direction = direction,
                lastKnownStatnId = accelAnchorStatnId ?: session.currentStatnId,
                apiCorrectedStatnId = null,
                accelStopCountSinceResync = accelCounter.stopCount,
                destStatnId = session.destStatnId,
            ),
        ).estimatedStatnId

    /** 내릴 역(환승역 또는 목적지) 이름으로 알람을 띄운다 — 알람이 울린 직전역 이름이 아니라. */
    private suspend fun fireAlarm(graph: StationGraph, session: TripSession, alertStationId: String, setting: AlarmSetting) {
        stopSensor()
        val plan = journeyPlanRepository.current()
        val nextLeg = plan?.takeIf { it.hasNextLeg }?.let { it.legs[it.currentLegIndex + 1] }
        val stationName = graph.station(session.destStatnId)?.name.orEmpty()
        val passedName = graph.station(alertStationId)?.name.orEmpty()
        val stopsAway = graph.remainingStops(alertStationId, session.destStatnId)
        val content = if (nextLeg != null) {
            AlarmContent.Transfer(stationName, passedName, stopsAway, nextLeg.lineName)
        } else {
            AlarmContent.FinalDestination(stationName, passedName, stopsAway)
        }
        alarmController.trigger(content, setting)
    }

    /** 확인을 기다리는 동안 포그라운드로 남아 진동·소리가 프로세스와 함께 끊기지 않게 한다. */
    private fun enterRingingState() {
        updateNotification(buildNotification("알람이 울렸어요 · 확인하면 다음으로 넘어가요"))
        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = lifecycleScope.launch {
            delay(RINGING_KEEPALIVE_MS)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /** 출발=도착 등 진행할 수 없는 세션 — 재시작해도 같은 자리에서 죽지 않게 지우고 멈춘다. */
    private suspend fun abandonBrokenSession() {
        Log.w(TAG, "진행할 수 없는 여정이라 추적을 멈춥니다")
        withContext(NonCancellable) {
            tripSessionRepository.clear()
            journeyPlanRepository.clear()
            boardingCandidateRepository.clear()
        }
    }

    private fun acquireWakeLock() {
        val lock = wakeLock ?: getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            ?.apply { setReferenceCounted(false) }
            ?.also { wakeLock = it }
        lock?.acquire(MAX_TRACKING_WAKE_MS)
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
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

    private fun statusText(session: TripSession, remaining: Int): String = when (session.state) {
        TripState.MATCHING -> "탑승할 열차를 확인하고 있어요"
        else -> "남은 정거장 ${remaining} · ${if (session.mode == TrackingMode.API) "실시간" else "추정"}"
    }

    private fun buildNotification(text: String): Notification {
        ensureChannel()
        val stopIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // 알림을 누르면 앱이 열리고, 진행 중인 여정이 있으면 추적 화면으로 간다.
        val openIntent = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, TRACKING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("다왔다 · 추적 중")
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "추적 그만하기", stopPendingIntent)
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
        private const val TAG = "TrackingService"
        private const val ACTION_STOP = "com.jin.subwayalarm.action.STOP_TRACKING"
        const val ACTION_CONFIRM_TRAIN = "com.jin.subwayalarm.action.CONFIRM_TRAIN"
        const val ACTION_REQUEST_REMATCH = "com.jin.subwayalarm.action.REQUEST_REMATCH"
        const val EXTRA_TRAIN_NO = "trainNo"
        private const val TRACKING_CHANNEL_ID = "tracking"
        private const val TRACKING_NOTIFICATION_ID = 2001

        /** 실시간 미지원 노선에서 "탑승한 것으로 간주" 표시. 화면은 이 값을 열차번호 대신 "센서 추정"으로 보여준다. */
        const val SENSOR_ONLY_TRAIN_NO = "SENSOR"

        private const val WAKE_LOCK_TAG = "dawatda:tracking"
        /** 한 여정이 이보다 길 일은 없다 — 잊힌 웨이크락이 배터리를 계속 쓰지 않게 하는 안전 상한. */
        private const val MAX_TRACKING_WAKE_MS = 3 * 60 * 60 * 1000L
        /** 알람 뒤 확인을 기다리며 포그라운드로 남는 최대 시간. */
        private const val RINGING_KEEPALIVE_MS = 10 * 60 * 1000L
        private const val MAX_CONSECUTIVE_ERRORS = 3
        private const val ERROR_RETRY_MS = 15_000L

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

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
import com.jin.subwayalarm.data.repository.JourneyPlanRepository
import com.jin.subwayalarm.data.repository.PositionFetchResult
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.TrainPositionRepository
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.domain.model.AlarmSetting
import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import com.jin.subwayalarm.domain.usecase.TrackingStepInput
import com.jin.subwayalarm.domain.usecase.TrackingStepUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 추적 Foreground Service (FGS type = dataSync, 4-1). API 단독 추적(작업순서 7, "MVP 핵심").
 * 2차(가속도계·HybridCoordinator)는 작업순서 11에서 합류.
 */
@AndroidEntryPoint
class TrackingService : LifecycleService() {

    @Inject lateinit var stationRepository: StationRepository
    @Inject lateinit var tripSessionRepository: TripSessionRepository
    @Inject lateinit var journeyPlanRepository: JourneyPlanRepository
    @Inject lateinit var trainPositionRepository: TrainPositionRepository
    @Inject lateinit var trackingStep: TrackingStepUseCase
    @Inject lateinit var alarmController: AlarmController

    private var trackingJob: Job? = null
    private var consecutiveFailures = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            trackingJob?.cancel()
            lifecycleScope.launch {
                tripSessionRepository.clear()
                journeyPlanRepository.clear()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(TRACKING_NOTIFICATION_ID, buildNotification("추적 준비 중"))

        // 여정 생성은 UI(HomeViewModel)의 책임 — 서비스 시작 전에 TripSession 이 이미 저장돼 있어야 한다.
        // 서비스는 항상 Room 에 있는 세션을 그대로 이어받아 추적만 한다(레이스 컨디션 방지, 재시작 시에도 동일 경로).
        trackingJob?.cancel()
        trackingJob = lifecycleScope.launch { runTrackingLoop() }

        // 프로세스가 죽어도 마지막 Intent 로 재전달 — TripSession(영속화됨)으로 이어받아 재개(4-1).
        return START_REDELIVER_INTENT
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
                    val result = trackingStep(
                        TrackingStepInput(
                            graph = graph,
                            session = session,
                            positions = fetch.positions,
                            nowEpochMillis = System.currentTimeMillis(),
                            polarity = lineMeta.directionPolarity,
                        ),
                    )
                    tripSessionRepository.save(result.updatedSession)

                    val remaining = graph.remainingStops(result.updatedSession.currentStatnId, session.destStatnId)
                    updateNotification(buildNotification(statusText(result.updatedSession, remaining)))

                    val alert = result.alert
                    if (alert != null) {
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
                    // 탑승 확인 중엔 출발역 정차 창(~30초)을 놓치지 않게 거리와 무관하게 짧은 주기.
                    val intervalSeconds = if (result.updatedSession.state == TripState.MATCHING) {
                        PollingIntervalPolicy.MATCHING_INTERVAL_SECONDS
                    } else {
                        PollingIntervalPolicy.intervalSeconds(remaining)
                    }
                    delay(intervalSeconds * 1000L)
                }
                is PositionFetchResult.RateLimited, is PositionFetchResult.Error -> {
                    consecutiveFailures++
                    updateNotification(buildNotification("갱신 지연 중…"))
                    delay(PollingIntervalPolicy.backoffSeconds(consecutiveFailures) * 1000L)
                }
            }
        }
        // 자연 종료(알람 발화·세션 없음·DONE·그래프 없음) 시 서비스도 함께 정리 — 좀비 FGS 방지.
        // ACTION_STOP 경로는 trackingJob.cancel() 로 여기 오지 않고 onStartCommand 에서 직접 정리한다.
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        private const val TRACKING_CHANNEL_ID = "tracking"
        private const val TRACKING_NOTIFICATION_ID = 2001

        /** 호출 전 TripSessionRepository.save() 로 여정을 먼저 저장해 둘 것(HomeViewModel 참조). */
        fun startIntent(context: Context): Intent = Intent(context, TrackingService::class.java)

        fun stopIntent(context: Context): Intent =
            Intent(context, TrackingService::class.java).apply { action = ACTION_STOP }
    }
}

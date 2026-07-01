package com.jin.subwayalarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jin.subwayalarm.R
import com.jin.subwayalarm.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 직전역 도달 알림 (4-3). 기본은 하이프라이어티 알림 + 알람용 채널 소리.
 * 풀스크린 인텐트는 best-effort — Android 14+ 에서 USE_FULL_SCREEN_INTENT 가
 * 제한될 수 있으나, 실패해도 하이프라이어티 알림 자체는 항상 뜬다(R-5).
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val ALARM_CHANNEL_ID = "alarm"
        const val ALARM_NOTIFICATION_ID = 1001
        const val EXTRA_SHOW_ALARM = "com.jin.subwayalarm.extra.SHOW_ALARM"
        const val ACTION_DISMISS_ALARM = "com.jin.subwayalarm.action.DISMISS_ALARM"
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(ALARM_CHANNEL_ID, "하차 알람", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "목적지 한 역 전 알람"
            enableVibration(false) // 진동은 VibratorController 가 파형으로 직접 제어
            setSound(alarmSound, audioAttributes)
            setBypassDnd(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun showAlertNotification(content: AlarmContent) {
        ensureChannel()
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_SHOW_ALARM, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dismissIntent = Intent(context, AlarmDismissReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, 0, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val (title, text) = when (content) {
            is AlarmContent.Transfer ->
                "환승하세요" to "${content.transferStationName}역에서 내려 ${content.nextLineName}으로 갈아타세요"
            is AlarmContent.FinalDestination ->
                "하차 준비" to "${content.destinationName} 다음 역에서 내리세요"
        }

        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, "해제", dismissPendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(ALARM_NOTIFICATION_ID, notification)
        }
    }

    fun cancelAlertNotification() {
        NotificationManagerCompat.from(context).cancel(ALARM_NOTIFICATION_ID)
    }
}

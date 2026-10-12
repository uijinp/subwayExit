package com.jin.subwayalarm.alarm

import android.app.Notification
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
import com.jin.subwayalarm.domain.model.AlarmSetting
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
        /** 진동만 모드용. 안드로이드 채널의 소리는 만든 뒤 앱에서 바꿀 수 없어 채널을 따로 둔다. */
        const val ALARM_SILENT_CHANNEL_ID = "alarm_silent"
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
            description = "내릴 역이 가까워지면 소리와 진동으로 알림"
            enableVibration(false) // 진동은 VibratorController 가 파형으로 직접 제어
            setSound(alarmSound, audioAttributes)
            setBypassDnd(true)
        }
        val silent = NotificationChannel(ALARM_SILENT_CHANNEL_ID, "하차 알람 (진동만)", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "내릴 역이 가까워지면 진동으로만 알림"
            enableVibration(false)
            setSound(null, null)
            setBypassDnd(true)
        }
        manager.createNotificationChannels(listOf(channel, silent))
    }

    fun showAlertNotification(content: AlarmContent, setting: AlarmSetting) {
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

        val channelId = if (setting.soundEnabled) ALARM_CHANNEL_ID else ALARM_SILENT_CHANNEL_ID
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, "확인했어요", dismissPendingIntent)
            // 밀어서 지우면 진동만 계속 울리고 여정도 안 넘어갔다 — 알람은 "확인"으로만 끈다.
            .setOngoing(true)
            .setAutoCancel(false)
        if (setting.showOnLockScreen) builder.setFullScreenIntent(fullScreenPendingIntent, true)
        val notification = builder.build().apply {
            // 확인할 때까지 알림 소리를 반복(채널 소리가 한 번만 나던 문제).
            if (setting.repeatUntilDismissed) flags = flags or Notification.FLAG_INSISTENT
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(ALARM_NOTIFICATION_ID, notification)
        }
    }

    fun cancelAlertNotification() {
        NotificationManagerCompat.from(context).cancel(ALARM_NOTIFICATION_ID)
    }
}

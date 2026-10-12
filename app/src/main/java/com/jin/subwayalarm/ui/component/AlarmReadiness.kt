package com.jin.subwayalarm.ui.component

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.jin.subwayalarm.alarm.NotificationHelper

/**
 * 알람이 실제로 울리는 데 필요한 시스템 상태. 사용자가 설정 앱에 다녀오면 바뀌므로 화면 복귀(ON_RESUME)마다 다시 읽는다.
 *
 * @param notificationsOn 앱 알림 + 알람 채널(소리/진동만 둘 다)이 켜져 있음
 * @param batteryUnrestricted 배터리 최적화 예외 — 꺼져 있으면 화면이 꺼진 동안 폴링이 늦어질 수 있다
 * @param fullScreenAllowed 잠금화면 전체화면 알람 허용 (Android 14+ 에서 사용자가 끌 수 있음)
 */
data class AlarmReadiness(
    val notificationsOn: Boolean,
    val batteryUnrestricted: Boolean,
    val fullScreenAllowed: Boolean,
) {
    companion object {
        fun read(context: Context): AlarmReadiness {
            val compat = NotificationManagerCompat.from(context)
            val channelsOn = listOf(NotificationHelper.ALARM_CHANNEL_ID, NotificationHelper.ALARM_SILENT_CHANNEL_ID)
                .all { id -> compat.getNotificationChannelCompat(id)?.importance != NotificationManagerCompat.IMPORTANCE_NONE }
            val power = context.getSystemService(PowerManager::class.java)
            val fullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
            } else {
                true
            }
            return AlarmReadiness(
                notificationsOn = compat.areNotificationsEnabled() && channelsOn,
                batteryUnrestricted = power?.isIgnoringBatteryOptimizations(context.packageName) == true,
                fullScreenAllowed = fullScreen,
            )
        }

        fun notificationSettingsIntent(context: Context): Intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

        /** 배터리 최적화 목록 화면. 직접 예외 요청(REQUEST_IGNORE_…)은 별도 권한이 필요해 쓰지 않는다. */
        fun batterySettingsIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

        fun fullScreenSettingsIntent(context: Context): Intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
            } else {
                notificationSettingsIntent(context)
            }
    }
}

/** 화면이 다시 보일 때마다 갱신되는 [AlarmReadiness]. */
@Composable
fun rememberAlarmReadiness(): AlarmReadiness {
    val context = LocalContext.current
    var readiness by remember { mutableStateOf(AlarmReadiness.read(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { readiness = AlarmReadiness.read(context) }
    return readiness
}

/** 설정 화면 열기 — 기기에 해당 화면이 없으면(일부 제조사) 앱 상세 설정으로. */
fun Context.openSettings(intent: Intent) {
    val launched = runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    if (!launched) {
        runCatching {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

package com.jin.subwayalarm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jin.subwayalarm.ui.screen.alarm.AlarmScreen
import com.jin.subwayalarm.ui.screen.home.HomeScreen
import com.jin.subwayalarm.ui.screen.linemap.LineMapScreen
import com.jin.subwayalarm.ui.screen.settings.SettingsScreen
import com.jin.subwayalarm.ui.screen.tracking.TrackingScreen

private object Routes {
    const val HOME = "home"
    const val TRACKING = "tracking"
    const val ALARM = "alarm"
    const val LINE_MAP = "line_map"
    const val SETTINGS = "settings"
}

/**
 * 화면 흐름: 홈 → 추적 중(탑승 열차 선택 포함) → 알람 → (환승이면) 다음 구간 추적 / (끝이면) 홈.
 * [alarmTrigger] 는 풀스크린 알림 인텐트로 앱이 열렸을 때(MainActivity) 값이 증가해 알람 화면으로 이동시킨다.
 * [onAlarmFinished] 는 알람 화면을 떠날 때 잠금화면 위 표시를 끄도록 액티비티에 알린다.
 */
@Composable
fun SubwayAlarmApp(alarmTrigger: Int, onAlarmFinished: () -> Unit) {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()

    LaunchedEffect(alarmTrigger) {
        if (alarmTrigger > 0) navController.navigate(Routes.ALARM) { launchSingleTop = true }
    }

    // 앱을 열었는데 여정이 진행 중이면 홈 대신 추적 화면부터(한 번만 — 홈으로 돌아온 뒤 회전해도 다시 끌고 가지 않게).
    var checkedActiveTrip by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (checkedActiveTrip) return@LaunchedEffect
        checkedActiveTrip = true
        if (appViewModel.hasActiveTrip() && navController.currentDestination?.route == Routes.HOME) {
            navController.navigate(Routes.TRACKING) { launchSingleTop = true }
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onTripStarted = { navController.navigate(Routes.TRACKING) { launchSingleTop = true } },
                onResumeTrip = { navController.navigate(Routes.TRACKING) { launchSingleTop = true } },
                onOpenLineMap = { navController.navigate(Routes.LINE_MAP) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.LINE_MAP) {
            LineMapScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TRACKING) {
            TrackingScreen(
                onSessionEnded = { navController.backToHome() },
                onBackHome = { navController.backToHome() },
                onAlarm = { navController.navigate(Routes.ALARM) { launchSingleTop = true } },
            )
        }
        composable(Routes.ALARM) {
            DisposableEffect(Unit) { onDispose { onAlarmFinished() } }
            AlarmScreen(
                onDismissed = { tripContinues ->
                    if (tripContinues) {
                        // 다음 구간이 이어짐 — 추적 화면으로(홈으로 리셋하지 않음).
                        navController.navigate(Routes.TRACKING) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    } else {
                        navController.backToHome()
                    }
                },
            )
        }
    }
}

/** 홈이 백스택에 있으면 거기까지 되돌리고, 없으면(알람으로 바로 열린 경우 등) 홈을 새로 연다. */
private fun NavHostController.backToHome() {
    if (!popBackStack(Routes.HOME, inclusive = false)) {
        navigate(Routes.HOME) {
            popUpTo(graph.id) { inclusive = true }
        }
    }
}

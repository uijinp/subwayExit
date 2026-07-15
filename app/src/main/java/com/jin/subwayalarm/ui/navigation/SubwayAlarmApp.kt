package com.jin.subwayalarm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jin.subwayalarm.ui.screen.alarm.AlarmScreen
import com.jin.subwayalarm.ui.screen.home.HomeScreen
import com.jin.subwayalarm.ui.screen.linemap.LineMapScreen
import com.jin.subwayalarm.ui.screen.tracking.TrackingScreen

private object Routes {
    const val HOME = "home"
    const val TRACKING = "tracking"
    const val ALARM = "alarm"
    const val LINE_MAP = "line_map"
}

/**
 * 화면 흐름(6절): 홈 → (탑승 확인, MVP 는 TrackingScreen 내부 상태로 표현) → 추적 중 → 알람.
 * [alarmTrigger] 는 풀스크린 알림 인텐트로 앱이 열렸을 때(MainActivity) 값이 증가해 알람 화면으로 이동시킨다.
 */
@Composable
fun SubwayAlarmApp(alarmTrigger: Int) {
    val navController = rememberNavController()

    LaunchedEffect(alarmTrigger) {
        if (alarmTrigger > 0) {
            navController.navigate(Routes.ALARM) { launchSingleTop = true }
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onTripStarted = {
                    navController.navigate(Routes.TRACKING)
                },
                onOpenLineMap = {
                    navController.navigate(Routes.LINE_MAP)
                },
            )
        }
        composable(Routes.LINE_MAP) {
            LineMapScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.TRACKING) {
            TrackingScreen(
                onSessionEnded = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }
        composable(Routes.ALARM) {
            AlarmScreen(
                onDismissed = { isTransfer ->
                    if (isTransfer) {
                        // 다음 구간이 이어짐 — 추적 화면으로(홈으로 리셋하지 않음).
                        navController.navigate(Routes.TRACKING) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    } else {
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
            )
        }
    }
}

package com.jin.subwayalarm.ui.screen.tracking

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jin.subwayalarm.domain.Josa
import com.jin.subwayalarm.domain.model.BoardingCandidate
import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TrainStatus
import com.jin.subwayalarm.domain.model.TripState
import com.jin.subwayalarm.tracking.TrackingService
import com.jin.subwayalarm.ui.component.Eyebrow
import com.jin.subwayalarm.ui.component.LineBadge
import com.jin.subwayalarm.ui.component.OutlinePill
import com.jin.subwayalarm.ui.component.PrimaryPill
import com.jin.subwayalarm.ui.component.TextLink
import com.jin.subwayalarm.ui.theme.Hls
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 추적 중 — 상단 잉크 띠에 남은 정거장을 크게, 아래에 "다음 할 일"·남은 경로·탄 열차.
 * 알람이 울리면(ALERTED) [onAlarm] 으로 전체화면 알람으로 넘긴다.
 */
@Composable
fun TrackingScreen(
    onSessionEnded: () -> Unit,
    onBackHome: () -> Unit,
    onAlarm: () -> Unit,
    viewModel: TrackingViewModel = hiltViewModel(),
) {
    val loadedState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(loadedState?.isActive) {
        val state = loadedState
        if (state != null && !state.isActive) onSessionEnded()
        // 서비스가 (업데이트·프로세스 종료로) 사라졌어도 저장된 여정이 있으면 추적을 이어간다. 이미 돌고 있으면 무시됨.
        if (state?.isActive == true) {
            runCatching { ContextCompat.startForegroundService(context, TrackingService.resumeIntent(context)) }
        }
    }
    LaunchedEffect(loadedState?.state) {
        if (loadedState?.state == TripState.ALERTED) onAlarm()
    }

    val uiState = loadedState ?: return

    Column(modifier = Modifier.fillMaxSize().background(Hls.Paper)) {
        Hero(uiState, onBackHome = onBackHome)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 12.dp),
        ) {
            item(key = "next") {
                if (uiState.state == TripState.ALERTED) {
                    AlarmBanner(uiState, onAcknowledge = viewModel::acknowledgeAlarm)
                } else {
                    NextActionPanel(uiState)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 탑승 확인 중이거나, 추적 중 "다른 열차예요" 를 눌러 바꿀 만한 다른 열차가 올라온 경우.
            // (자동 확정 직후엔 지금 탄 열차 하나만 후보로 남아 있을 수 있어 그건 제외)
            val choosing = uiState.state == TripState.MATCHING ||
                (uiState.state == TripState.TRACKING && uiState.candidates.any { it.trainNo != uiState.boardedTrainNo })
            if (choosing) {
                item(key = "candidates") {
                    CandidatePanel(
                        candidates = uiState.candidates,
                        boardedTrainNo = uiState.boardedTrainNo,
                        onSelect = { trainNo ->
                            ContextCompat.startForegroundService(context, TrackingService.confirmTrainIntent(context, trainNo))
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            items(uiState.route, key = { it.statnId }) { station ->
                RouteStationRow(
                    station = station,
                    isFirstShown = station == uiState.route.first(),
                    transferLabel = if (uiState.isTransferLeg) "${uiState.nextLineName} 환승" else null,
                )
            }

            if (uiState.state != TripState.MATCHING && uiState.boardedTrainNo != null) {
                item(key = "train") {
                    Spacer(modifier = Modifier.height(16.dp))
                    TrainRow(
                        uiState = uiState,
                        onRematch = {
                            ContextCompat.startForegroundService(context, TrackingService.rematchIntent(context))
                        },
                    )
                }
            }
        }

        StopButton(
            onStop = { context.startService(TrackingService.stopIntent(context)) },
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 22.dp, end = 22.dp, bottom = 16.dp, top = 4.dp),
        )
    }
}

@Composable
private fun Hero(uiState: TrackingViewModel.UiState, onBackHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Hls.Ink)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 12.dp, end = 22.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextLink("← 홈", onClick = onBackHome, color = Hls.OnDarkSub)
            Spacer(modifier = Modifier.weight(1f))
            val (status, live) = when {
                uiState.state == TripState.MATCHING -> "탑승 열차 찾는 중" to true
                uiState.state == TripState.ALERTED -> "알람 울리는 중" to true
                uiState.isSensorOnly || uiState.mode == TrackingMode.ACCEL -> "추정 추적 중" to false
                else -> "실시간 추적 중" to true
            }
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape)
                    .background(if (live) Hls.AccentLight else Hls.OnDarkSub2),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(status, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (live) Hls.AccentLight else Hls.OnDarkSub)
            uiState.lastSyncAtMillis?.let {
                Text(" · ${formatTime(it)} 갱신", fontSize = 12.sp, color = Hls.OnDarkSub2)
            }
        }

        Column(modifier = Modifier.padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.lineName.isNotEmpty()) LineBadge(uiState.lineName, size = 24.dp)
                val meta = buildList {
                    add(uiState.lineName)
                    if (uiState.totalLegs > 1) add("${uiState.currentLegNumber}/${uiState.totalLegs}구간")
                    uiState.terminusName?.let { add("${it}행") }
                }.filter { it.isNotEmpty() }.joinToString(" · ")
                Text(meta, fontSize = 14.sp, color = Hls.OnDarkSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${uiState.remainingStops}",
                    style = TextStyle(fontSize = 72.sp, lineHeight = 72.sp, fontWeight = FontWeight.ExtraBold, color = Hls.OnDark),
                )
                Text(
                    "정거장 남음",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Hls.OnDark,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Hls.DarkLine))
            Row {
                InfoCell("지금", uiState.currentStationName.ifEmpty { "–" })
                InfoCell("알람", if (uiState.alertStationName.isEmpty()) "–" else "${uiState.alertStationName} 도착 시")
                val eta = uiState.etaMinutes?.let {
                    if (uiState.state == TripState.MATCHING) "탑승 후 ${it}분" else "약 ${it}분"
                } ?: "–"
                InfoCell("도착 예상", eta)
            }
        }
    }
}

@Composable
private fun RowScope.InfoCell(label: String, value: String) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 11.5.sp, color = Hls.OnDarkSub2)
        // "교대(법원.검찰청) 도착 시" 처럼 긴 역명은 두 줄까지.
        Text(
            value,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Bold,
            color = Hls.OnDark,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}

@Composable
private fun NextActionPanel(uiState: TrackingViewModel.UiState) {
    val dest = uiState.destinationName
    val nextLine = uiState.nextLineName
    val (eyebrow, title, body) = when {
        uiState.state == TripState.MATCHING -> Triple(
            "다음 할 일 · 탑승",
            "${uiState.originName}에서 열차에 타세요",
            if (uiState.candidates.isEmpty()) "출발역 근처 열차를 찾는 중이에요." else "아래 목록에서 탄 열차를 고르면 바로 추적을 시작해요.",
        )
        uiState.isTransferLeg -> Triple(
            "다음 할 일 · 환승",
            if (uiState.isWalkingTransfer) {
                "${dest}에서 내려 ${uiState.nextBoardName}까지 걸어서 환승"
            } else {
                "${dest}에서 내려 ${Josa.ro(nextLine)} 갈아타기"
            },
            alarmWhen(uiState) + " 확인하면 $nextLine 추적이 이어집니다.",
        )
        else -> Triple("다음 할 일 · 하차", "${dest}에서 내리기", alarmWhen(uiState))
    }
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Hls.AccentTint)
            .border(1.dp, Hls.AccentSoft, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Eyebrow(eyebrow)
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Hls.Ink)
        Text(body, fontSize = 13.5.sp, color = Hls.InkSoft)
    }
}

private fun alarmWhen(uiState: TrackingViewModel.UiState): String = when {
    uiState.alertStationName.isEmpty() -> "내릴 역이 가까워지면 알람이 울립니다."
    uiState.alertStationName == uiState.originName -> "가까운 거리라 출발하면 바로 알람이 울립니다."
    else -> "${uiState.alertStationName}에 들어서면 알람이 울립니다."
}

/** 앱을 보고 있는 중에 알람이 울린 경우 — 전체화면 알람으로 넘어가기 전 잠깐, 또는 돌아왔을 때 보인다. */
@Composable
private fun AlarmBanner(uiState: TrackingViewModel.UiState, onAcknowledge: () -> Unit) {
    val stops = uiState.remainingStops
    val whenLabel = if (stops <= 1) "다음 정차역" else "${stops}정거장 뒤"
    val title = if (uiState.isTransferLeg) "${uiState.destinationName}에서 환승하세요" else "${uiState.destinationName}에서 내리세요"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Hls.Accent)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("${uiState.currentStationName} 통과 · $whenLabel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Hls.OnAccent)
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Hls.OnAccent)
        if (uiState.isTransferLeg) {
            Text("내려서 ${Josa.ro(uiState.nextLineName)} 갈아타세요", fontSize = 14.sp, color = Hls.OnAccent)
        }
        Spacer(modifier = Modifier.height(6.dp))
        PrimaryPill(
            "확인했어요",
            onClick = onAcknowledge,
            modifier = Modifier.fillMaxWidth(),
            container = Hls.Paper,
            content = Hls.Ink,
        )
    }
}

@Composable
private fun CandidatePanel(
    candidates: List<BoardingCandidate>,
    boardedTrainNo: String?,
    onSelect: (String) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, Hls.Line, shape)) {
        Eyebrow(
            if (boardedTrainNo == null) "탄 열차 고르기" else "다른 열차로 바꾸기",
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 4.dp),
        )
        if (candidates.isEmpty()) {
            Text(
                "출발역에 들어오는 열차를 찾는 중…",
                fontSize = 14.sp,
                color = Hls.Muted,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 4.dp),
            )
        }
        candidates.forEachIndexed { index, c ->
            if (index > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Hls.LineSoft))
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 14.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        buildString {
                            append(c.trainNo)
                            c.terminusName?.let { append(" · ${it}행") }
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Hls.Ink,
                    )
                    Text(
                        buildString {
                            append("${c.statnName} ${statusLabel(c.status)}")
                            if (c.isExpress) append(" · 급행")
                        },
                        fontSize = 12.5.sp,
                        color = Hls.Muted,
                    )
                }
                val current = c.trainNo == boardedTrainNo
                OutlinePill(
                    if (current) "타는 중" else "탔어요",
                    onClick = { onSelect(c.trainNo) },
                    enabled = !current,
                    textColor = Hls.AccentDeep,
                )
            }
        }
    }
}

private fun statusLabel(status: TrainStatus): String = when (status) {
    TrainStatus.APPROACHING -> "진입"
    TrainStatus.ARRIVED -> "도착"
    TrainStatus.DEPARTED -> "출발"
    TrainStatus.DEPARTED_PREVIOUS_STATION -> "전역 출발"
}

@Composable
private fun TrainRow(uiState: TrackingViewModel.UiState, onRematch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Hls.Line, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("탄 열차", fontSize = 10.5.sp, color = Hls.Muted)
            val label = if (uiState.isSensorOnly) {
                "센서로 위치 추정 중"
            } else {
                buildString {
                    append(uiState.boardedTrainNo)
                    uiState.terminusName?.let { append(" · ${it}행") }
                    append(" ✓")
                }
            }
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Hls.Ink, maxLines = 1)
        }
        if (uiState.state == TripState.TRACKING) {
            TextLink("다른 열차예요 →", onClick = onRematch, color = Hls.AccentDeep)
        }
    }
}

@Composable
private fun RouteStationRow(
    station: TrackingViewModel.RouteStation,
    isFirstShown: Boolean,
    transferLabel: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(if (station.isCurrent) 48.dp else 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RailMarker(station = station, drawTop = !isFirstShown, modifier = Modifier.width(20.dp).fillMaxHeight())
        Text(
            station.name,
            fontSize = if (station.isCurrent) 17.sp else 15.sp,
            fontWeight = if (station.isCurrent || station.isAlert || station.isDestination) FontWeight.Bold else FontWeight.Normal,
            color = when {
                station.isPassed -> Hls.Faint
                station.isCurrent || station.isAlert || station.isDestination -> Hls.Ink
                else -> Hls.InkSoft
            },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        when {
            station.isCurrent -> Text("현재 위치", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Hls.AccentDeep)
            station.isPassed -> Text("지남", fontSize = 12.sp, color = Hls.Faint)
            station.isDestination -> Tag(transferLabel ?: "도착", dark = true)
            station.isAlert -> Tag("알람", dark = false)
        }
    }
}

@Composable
private fun Tag(text: String, dark: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .height(22.dp)
            .clip(shape)
            .background(if (dark) Hls.Ink else Hls.AccentTint)
            .then(if (dark) Modifier else Modifier.border(1.dp, Hls.AccentSoft, shape))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (dark) Hls.OnDark else Hls.AccentDeep)
    }
}

/** 레일 — 지난 구간은 옅은 파랑, 현재역은 굵은 파란 점, 알람역은 파란 테 원, 도착역은 잉크 원. */
@Composable
private fun RailMarker(
    station: TrackingViewModel.RouteStation,
    drawTop: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val stroke = 2.dp.toPx()
        val passedColor = Hls.Accent.copy(alpha = 0.35f)
        val topColor = when {
            station.isPassed -> passedColor
            station.isCurrent -> Hls.Accent
            else -> Hls.Line
        }
        val bottomColor = if (station.isPassed) passedColor else Hls.Line
        if (drawTop) drawLine(topColor, Offset(cx, 0f), Offset(cx, cy), stroke)
        if (!station.isDestination) drawLine(bottomColor, Offset(cx, cy), Offset(cx, size.height), stroke)

        val c = Offset(cx, cy)
        when {
            station.isCurrent -> {
                drawCircle(Hls.AccentSoft, radius = 8.dp.toPx(), center = c)
                drawCircle(Hls.Accent, radius = 4.dp.toPx(), center = c)
            }
            station.isPassed -> drawCircle(passedColor, radius = 4.dp.toPx(), center = c)
            station.isDestination -> {
                drawCircle(Hls.Ink, radius = 9.dp.toPx(), center = c)
                drawCircle(Color.White, radius = 6.dp.toPx(), center = c)
                drawCircle(Hls.Ink, radius = 4.dp.toPx(), center = c)
            }
            station.isAlert -> {
                drawCircle(Hls.Paper, radius = 5.dp.toPx(), center = c)
                drawCircle(Hls.Accent, radius = 4.dp.toPx(), center = c, style = Stroke(width = 2.dp.toPx()))
            }
            else -> {
                drawCircle(Hls.Paper, radius = 4.dp.toPx(), center = c)
                drawCircle(Hls.Line, radius = 3.dp.toPx(), center = c, style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

/** 실수로 눌러 추적이 끊기지 않게 두 번 눌러야 멈춘다(3초 안에). */
@Composable
private fun StopButton(onStop: () -> Unit, modifier: Modifier = Modifier) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(3_000)
            armed = false
        }
    }
    OutlinePill(
        if (armed) "한 번 더 누르면 추적을 멈춥니다" else "추적 그만하기",
        onClick = { if (armed) onStop() else armed = true },
        modifier = modifier.fillMaxWidth(),
        height = 52.dp,
        fontSize = 16.sp,
        textColor = if (armed) Hls.ErrorText else Hls.Ink,
        borderColor = if (armed) Hls.ErrorText else Hls.Line,
    )
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime().format(TIME_FORMAT)

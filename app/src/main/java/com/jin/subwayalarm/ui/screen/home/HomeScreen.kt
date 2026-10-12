package com.jin.subwayalarm.ui.screen.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jin.subwayalarm.domain.Josa
import com.jin.subwayalarm.domain.model.FavoriteRoute
import com.jin.subwayalarm.tracking.TrackingService
import com.jin.subwayalarm.ui.component.AlarmReadiness
import com.jin.subwayalarm.ui.component.Eyebrow
import com.jin.subwayalarm.ui.component.LineBadge
import com.jin.subwayalarm.ui.component.OutlinePill
import com.jin.subwayalarm.ui.component.PrimaryPill
import com.jin.subwayalarm.ui.component.TextLink
import com.jin.subwayalarm.ui.component.compactLineNames
import com.jin.subwayalarm.ui.component.openSettings
import com.jin.subwayalarm.ui.component.rememberAlarmReadiness
import com.jin.subwayalarm.ui.screen.home.HomeViewModel.LegInfo
import com.jin.subwayalarm.ui.screen.home.HomeViewModel.RouteSummary
import com.jin.subwayalarm.ui.screen.home.HomeViewModel.StationOption
import com.jin.subwayalarm.ui.theme.Hls
import kotlinx.coroutines.launch

/**
 * 홈 — 자주 가는 경로는 카드의 "출발 →" 한 번으로 바로 추적, 새 경로는 출발·도착을 골라 미리보기 후 시작.
 */
@Composable
fun HomeScreen(
    onTripStarted: () -> Unit,
    onResumeTrip: () -> Unit,
    onOpenLineMap: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val readiness = rememberAlarmReadiness()

    // 알림 권한은 처음 추적을 시작할 때 한 번만 묻는다(매번 묻지 않음). 거절하면 상단 경고로 안내.
    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.detectOrigin() }

    fun askNotificationsOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || askedNotificationPermission) return
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            askedNotificationPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun launchTracking() {
        ContextCompat.startForegroundService(context, TrackingService.startIntent(context))
        onTripStarted()
    }

    Scaffold(containerColor = Hls.Paper) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Header(onOpenLineMap = onOpenLineMap, onOpenSettings = onOpenSettings)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                uiState.activeTripLabel?.let { label ->
                    ActiveTripCard(label = label, onResume = onResumeTrip)
                }

                if (!readiness.notificationsOn) {
                    NotificationWarning(onOpen = { context.openSettings(AlarmReadiness.notificationSettingsIntent(context)) })
                }

                if (uiState.favorites.isNotEmpty()) {
                    FavoritesSection(
                        uiState = uiState,
                        onToggleEdit = viewModel::toggleEditFavorites,
                        onApply = viewModel::applyFavorite,
                        onRemove = viewModel::removeFavorite,
                        onStart = { route ->
                            askNotificationsOnce()
                            scope.launch { if (viewModel.startFavorite(route)) launchTracking() }
                        },
                    )
                }

                NewRouteSection(
                    uiState = uiState,
                    onSelectOrigin = viewModel::selectOrigin,
                    onSelectDestination = viewModel::selectDestination,
                    onSwap = viewModel::swapStations,
                    onDetect = {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) viewModel.detectOrigin() else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    },
                    onStart = {
                        askNotificationsOnce()
                        scope.launch { if (viewModel.startTrip()) launchTracking() }
                    },
                    onToggleFavorite = viewModel::toggleFavorite,
                )
            }
        }
    }
}

@Composable
private fun Header(onOpenLineMap: () -> Unit, onOpenSettings: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(start = 22.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                buildAnnotatedString {
                    append("다")
                    withStyle(SpanStyle(color = Hls.Accent)) { append("왔다") }
                },
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = Hls.Ink,
                modifier = Modifier.semantics { contentDescription = "다왔다" },
            )
            Spacer(modifier = Modifier.weight(1f))
            TextLink("노선 현황", onClick = onOpenLineMap)
            TextLink("설정", onClick = onOpenSettings)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Hls.LineSoft))
    }
}

@Composable
private fun ActiveTripCard(label: String, onResume: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Hls.Ink)
            .clickable(onClick = onResume)
            .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Eyebrow("추적 중", color = Hls.AccentLight)
            Text(label, color = Hls.OnDark, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        PrimaryPill("돌아가기 →", onClick = onResume, height = 44.dp, fontSize = 15.sp)
    }
}

@Composable
private fun NotificationWarning(onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Hls.Line, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("알림이 꺼져 있어요", color = Hls.ErrorText, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
            Text("이대로는 내릴 역에서 알람이 울리지 않습니다", color = Hls.Muted, fontSize = 12.5.sp)
        }
        TextLink("설정 열기 ↗", onClick = onOpen, color = Hls.AccentDeep, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
    }
}

@Composable
private fun FavoritesSection(
    uiState: HomeViewModel.UiState,
    onToggleEdit: () -> Unit,
    onApply: (FavoriteRoute) -> Unit,
    onRemove: (FavoriteRoute) -> Unit,
    onStart: (FavoriteRoute) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("자주 가는 경로 · 바로 출발", modifier = Modifier.weight(1f))
            TextLink(
                if (uiState.editingFavorites) "완료" else "편집",
                onClick = onToggleEdit,
                color = if (uiState.editingFavorites) Hls.AccentDeep else Hls.Muted,
                fontSize = 13.sp,
            )
        }
        uiState.favorites.forEach { route ->
            FavoriteCard(
                route = route,
                summary = uiState.favoriteSummaries[route],
                editing = uiState.editingFavorites,
                starting = uiState.startingFavorite == route,
                startEnabled = uiState.startingFavorite == null,
                onApply = { onApply(route) },
                onRemove = { onRemove(route) },
                onStart = { onStart(route) },
            )
        }
    }
}

@Composable
private fun FavoriteCard(
    route: FavoriteRoute,
    summary: RouteSummary?,
    editing: Boolean,
    starting: Boolean,
    startEnabled: Boolean,
    onApply: () -> Unit,
    onRemove: () -> Unit,
    onStart: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Hls.Line, shape)
            .clickable(onClick = onApply)
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                summary?.lineNames?.distinct()?.forEach { LineBadge(it) }
                Text(
                    "${route.originName} → ${route.destinationName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Hls.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val sub = when {
                summary == null -> "경로 확인 중…"
                !summary.found -> "지금은 경로를 찾을 수 없어요"
                else -> buildString {
                    append(summary.transferText)
                    summary.totalStops?.let { append(" · ${it}정거장") }
                }
            }
            Text(sub, fontSize = 13.sp, color = Hls.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (editing) {
            OutlinePill("삭제", onClick = onRemove, textColor = Hls.InkSoft)
        } else {
            PrimaryPill(
                if (starting) "준비 중…" else "출발 →",
                onClick = onStart,
                enabled = startEnabled && summary?.found != false,
                height = 44.dp,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun NewRouteSection(
    uiState: HomeViewModel.UiState,
    onSelectOrigin: (String) -> Unit,
    onSelectDestination: (String) -> Unit,
    onSwap: () -> Unit,
    onDetect: () -> Unit,
    onStart: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow("새 경로")

        val cardShape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier.fillMaxWidth().border(1.dp, Hls.Line, cardShape).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("출발")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StationField(
                        options = uiState.allStations,
                        selectedName = uiState.originName,
                        placeholder = "출발역 검색",
                        onSelect = onSelectOrigin,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinePill(
                        if (uiState.detecting) "찾는 중…" else "현재 위치",
                        onClick = onDetect,
                        enabled = !uiState.detecting,
                        textColor = Hls.AccentDeep,
                        height = 48.dp,
                    )
                }
                uiState.detectMessage?.let {
                    Text(it, fontSize = 12.5.sp, color = Hls.AccentDeep, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Hls.LineSoft))
                OutlinePill(
                    "↻ 바꾸기",
                    onClick = onSwap,
                    enabled = uiState.originName != null || uiState.destinationName != null,
                    height = 36.dp,
                    fontSize = 13.sp,
                    textColor = Hls.InkSoft,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.semantics { contentDescription = "출발과 도착 바꾸기" },
                )
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Hls.LineSoft))
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("도착")
                StationField(
                    options = uiState.allStations,
                    selectedName = uiState.destinationName,
                    placeholder = "도착역 검색",
                    onSelect = onSelectDestination,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        when {
            uiState.planning -> Text("경로 계산 중…", fontSize = 13.sp, color = Hls.Muted)
            uiState.routeError != null -> Text(uiState.routeError, fontSize = 13.sp, color = Hls.ErrorText)
            uiState.legs.isNotEmpty() -> {
                RoutePreview(uiState.legs)
                uiState.realtimeWarning?.let { Text(it, fontSize = 12.5.sp, color = Hls.Muted) }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryPill(
                "추적 시작 →",
                onClick = onStart,
                enabled = uiState.canStart && uiState.startingFavorite == null,
                modifier = Modifier.weight(1f),
            )
            val saved = uiState.isCurrentRouteFavorite
            if (saved) {
                OutlinePill(
                    "✓ 저장됨",
                    onClick = onToggleFavorite,
                    height = 52.dp,
                    fontSize = 15.sp,
                    textColor = Hls.AccentDeep,
                    borderColor = Hls.AccentSoft,
                    container = Hls.AccentTint,
                    contentPadding = PaddingValues(horizontal = 18.dp),
                )
            } else {
                OutlinePill(
                    "저장",
                    onClick = onToggleFavorite,
                    enabled = uiState.currentRoute != null,
                    height = 52.dp,
                    fontSize = 15.sp,
                    textColor = Hls.InkSoft,
                    contentPadding = PaddingValues(horizontal = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Hls.Ink)
}

/** 계산된 여정 미리보기 — 구간마다 노선·정거장 수, 사이에 "↓ 약수에서 6호선으로 환승". */
@Composable
private fun RoutePreview(legs: List<LegInfo>) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Hls.AccentTint)
            .border(1.dp, Hls.AccentSoft, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        legs.forEachIndexed { index, info ->
            if (index > 0) {
                val prev = legs[index - 1].leg
                val next = info.leg
                val text = if (prev.alightStationName == next.boardStationName) {
                    "↓ ${prev.alightStationName}에서 ${Josa.ro(next.lineName)} 환승"
                } else {
                    "↓ ${prev.alightStationName}에서 내려 ${next.boardStationName}까지 걸어서 환승"
                }
                Text(
                    text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Hls.AccentDeep,
                    modifier = Modifier.padding(start = 30.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LineBadge(info.leg.lineName)
                Text(
                    "${info.leg.boardStationName} → ${info.leg.alightStationName}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Hls.Ink,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                info.stops?.let { Text("${it}정거장", fontSize = 13.sp, color = Hls.InkSoft) }
            }
        }
    }
}

/**
 * 역 검색 필드. 입력 텍스트로 역 목록을 실시간 필터링해 드롭다운으로 보여준다.
 *
 * 이미 고른 역을 바꿀 때 글자를 다 지워야 했던 문제 때문에, 필드를 누르면(=목록을 열면)
 * 검색어를 비워 **전체 목록부터 보여준다.** 고르지 않고 닫으면 원래 선택값이 그대로 복원된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationField(
    options: List<StationOption>,
    selectedName: String?,
    placeholder: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    /** 사용자가 직접 입력 중인 검색어. null = 선택된 값을 그대로 보여주는 상태. */
    var typed by remember(selectedName) { mutableStateOf<String?>(null) }

    val text = typed ?: selectedName.orEmpty()
    val query = typed?.trim().orEmpty()
    val filtered = if (query.isEmpty()) {
        options
    } else {
        options.filter { it.name.contains(query, ignoreCase = true) }
            .sortedByDescending { it.name.startsWith(query, ignoreCase = true) }
    }
    val lineHint = options.firstOrNull { it.name == selectedName }?.lineNames?.let(::compactLineNames)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { wantOpen ->
            expanded = wantOpen
            // 열 때: 검색어를 비워 전체 목록을 보여준다. 닫을 때: 선택값 복원.
            typed = if (wantOpen) "" else null
        },
        modifier = modifier,
    ) {
        val shape = RoundedCornerShape(8.dp)
        BasicTextField(
            value = text,
            onValueChange = {
                typed = it
                expanded = true
            },
            singleLine = true,
            textStyle = TextStyle(fontSize = 16.sp, color = Hls.Ink),
            cursorBrush = SolidColor(Hls.Accent),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, if (expanded) Hls.Accent else Hls.Line, shape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (text.isEmpty()) {
                            Text(
                                if (expanded && selectedName != null) selectedName else placeholder,
                                fontSize = 16.sp,
                                color = Hls.Faint,
                                maxLines = 1,
                            )
                        }
                        inner()
                    }
                    if (typed == null && lineHint != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(lineHint, fontSize = 12.sp, color = Hls.Muted, maxLines = 1)
                    }
                }
            },
        )
        ExposedDropdownMenu(
            expanded = expanded && filtered.isNotEmpty(),
            onDismissRequest = {
                expanded = false
                typed = null
            },
            containerColor = Hls.Paper,
        ) {
            filtered.take(60).forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(option.name, fontSize = 16.sp, color = Hls.Ink, modifier = Modifier.weight(1f, fill = false))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(compactLineNames(option.lineNames), fontSize = 12.sp, color = Hls.Muted, maxLines = 1)
                        }
                    },
                    onClick = {
                        onSelect(option.name)
                        typed = null
                        expanded = false
                        // 고르면 키보드를 내려 아래 경로 미리보기·시작 버튼이 보이게.
                        focusManager.clearFocus()
                    },
                )
            }
        }
    }
}

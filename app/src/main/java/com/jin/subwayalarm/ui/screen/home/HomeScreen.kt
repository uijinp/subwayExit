package com.jin.subwayalarm.ui.screen.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jin.subwayalarm.domain.model.Leg
import com.jin.subwayalarm.tracking.TrackingService
import com.jin.subwayalarm.ui.theme.DawatColors
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onTripStarted: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.detectOrigin() }

    Scaffold(containerColor = DawatColors.LightBg) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 로고 헤더
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.width(40.dp).height(40.dp)
                        .clip(RoundedCornerShape(12.dp)).background(DawatColors.HeroCard),
                    contentAlignment = Alignment.Center,
                ) { Text("다", color = DawatColors.OnDark, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                Spacer(modifier = Modifier.width(10.dp))
                Text("다왔다", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            // 경로 입력 카드 (노선은 자동 결정 — 환승 필요하면 자동으로 계산됨)
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)).background(DawatColors.Card)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SearchableDropdown(
                        label = "출발역",
                        options = uiState.allStations.map { it.name to "${it.name} · ${it.lineNames.joinToString("·")}" },
                        selectedId = uiState.originName,
                        onSelect = viewModel::selectOrigin,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        enabled = !uiState.detecting,
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) viewModel.detectOrigin() else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        },
                    ) { Text(if (uiState.detecting) "…" else "📍") }
                }
                uiState.detectMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = DawatColors.Green, fontWeight = FontWeight.Bold)
                }
                SearchableDropdown(
                    label = "목적지",
                    options = uiState.allStations.map { it.name to "${it.name} · ${it.lineNames.joinToString("·")}" },
                    selectedId = uiState.destinationName,
                    onSelect = viewModel::selectDestination,
                )
            }

            when {
                uiState.planning -> Text("경로 계산 중…", style = MaterialTheme.typography.bodyMedium, color = DawatColors.TextMuted)
                uiState.routeError != null -> Text(
                    uiState.routeError.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawatColors.Coral,
                )
                uiState.legs.isNotEmpty() -> RoutePreview(uiState.legs)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                enabled = uiState.canStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DawatColors.Green,
                    contentColor = DawatColors.OnGreen,
                    disabledContainerColor = DawatColors.Divider,
                ),
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    scope.launch {
                        if (viewModel.startTrip()) {
                            ContextCompat.startForegroundService(context, TrackingService.startIntent(context))
                            onTripStarted()
                        }
                    }
                },
            ) {
                Text("▶ 추적 시작", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

/** 계산된 여정 구간을 카드로 나열. 환승이 있으면 구간 사이에 "환승" 구분선 표시. */
@Composable
private fun RoutePreview(legs: List<Leg>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        legs.forEachIndexed { index, leg ->
            if (index > 0) {
                Text(
                    "↓ 환승",
                    style = MaterialTheme.typography.labelMedium,
                    color = DawatColors.TextMuted,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)).background(DawatColors.GreenTint)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(DawatColors.Green)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) { Text(leg.lineName, color = DawatColors.OnGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "${leg.boardStationName} → ${leg.alightStationName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * 검색 가능한 선택 필드. 입력 텍스트로 [options] 를 실시간 필터링해 드롭다운으로 보여준다.
 * options 는 (id, 표시명) 목록.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchableDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(selectedId, options) {
        mutableStateOf(options.firstOrNull { it.first == selectedId }?.second.orEmpty())
    }
    val filtered = if (query.isBlank()) {
        options
    } else {
        options.filter { it.second.contains(query.trim(), ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded && filtered.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            filtered.take(60).forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        query = name
                        expanded = false
                    },
                )
            }
        }
    }
}

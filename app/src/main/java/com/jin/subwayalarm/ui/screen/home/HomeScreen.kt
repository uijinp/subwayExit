package com.jin.subwayalarm.ui.screen.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("하차알람", style = MaterialTheme.typography.headlineMedium)

            SearchableDropdown(
                label = "노선",
                options = uiState.lines.map { it.lineId to it.lineName },
                selectedId = uiState.selectedLineId,
                onSelect = viewModel::selectLine,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                SearchableDropdown(
                    label = "출발역",
                    options = uiState.stations.map { it.statnId to it.name },
                    selectedId = uiState.originStatnId,
                    onSelect = viewModel::selectOrigin,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    enabled = !uiState.detecting,
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) viewModel.detectOrigin() else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    },
                ) {
                    Text(if (uiState.detecting) "…" else "📍")
                }
            }
            uiState.detectMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            SearchableDropdown(
                label = "목적지",
                options = uiState.stations.map { it.statnId to it.name },
                selectedId = uiState.destStatnId,
                onSelect = viewModel::selectDestination,
            )

            uiState.remainingStops?.let { remaining ->
                Text(
                    "목적지까지 $remaining 정거장",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                enabled = uiState.canStart,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    scope.launch {
                        viewModel.startTrip()
                        ContextCompat.startForegroundService(
                            context,
                            com.jin.subwayalarm.tracking.TrackingService.startIntent(context),
                        )
                        onTripStarted()
                    }
                },
            ) {
                Text("추적 시작")
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
    // selectedId 가 외부에서 바뀌면(자동감지·노선변경) 입력 텍스트를 해당 역명으로 재동기화.
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

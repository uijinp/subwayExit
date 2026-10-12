package com.jin.subwayalarm.ui.screen.linemap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.usecase.LineBoardRow
import com.jin.subwayalarm.domain.usecase.LineTrainMarker
import com.jin.subwayalarm.ui.theme.Hls
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 노선 실시간 현황판 — 선택한 노선의 역을 전부 나열하고 지금 열차가 어느 역에 있는지 표시.
 * ▼ = 순번 증가 방향(리스트 아래쪽으로 진행), ▲ = 순번 감소 방향.
 */
@Composable
fun LineMapScreen(
    onBack: () -> Unit,
    viewModel: LineMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(containerColor = Hls.Paper) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            // 헤더
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(Hls.CardAlt).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Text("←", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.width(10.dp))
                Text("실시간 노선 현황", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            // 노선 선택 칩
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.lines.forEach { line ->
                    val selected = line.lineId == uiState.selectedLineId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) Hls.Accent else Hls.CardAlt)
                            .clickable { viewModel.selectLine(line.lineId) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            line.displayName.ifBlank { line.lineName },
                            color = if (selected) Hls.OnAccent else Hls.Muted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            // 상태줄
            StatusLine(uiState)

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(uiState.rows, key = { _, row -> row.station.statnId }) { index, row ->
                    BoardStationRow(
                        row = row,
                        isFirst = index == 0,
                        isLast = index == uiState.rows.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusLine(uiState: LineMapViewModel.UiState) {
    val updatedText = uiState.updatedAtMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(TIME_FORMAT)
    }
    val text = when {
        uiState.loading && uiState.rows.isEmpty() -> "불러오는 중…"
        updatedText != null -> buildString {
            append("열차 ${uiState.shownTrainCount}대")
            if (uiState.hiddenTrainCount > 0) append(" (표시 구간 밖 ${uiState.hiddenTrainCount}대 제외)")
            append(" · $updatedText 갱신 · 15초마다 자동 갱신")
        }
        else -> ""
    }
    if (text.isNotEmpty()) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = Hls.Muted, modifier = Modifier.padding(bottom = 8.dp))
    }
    uiState.error?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = Hls.ErrorText, modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun BoardStationRow(row: LineBoardRow, isFirst: Boolean, isLast: Boolean) {
    val hasTrain = row.trains.isNotEmpty()
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 레일 + 열차 위치 점
        Box(modifier = Modifier.width(44.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            RailMarker(isFirst = isFirst, isLast = isLast, hasTrain = hasTrain, modifier = Modifier.fillMaxSize())
        }
        // 열차번호 배지(방향·급행 색)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row.trains.forEach { TrainNumberBadge(it) }
        }
        if (hasTrain) Spacer(modifier = Modifier.width(8.dp)) else Spacer(modifier = Modifier.width(4.dp))
        Text(
            row.station.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (hasTrain) FontWeight.Bold else FontWeight.Normal,
            color = if (hasTrain) Hls.Ink else Hls.Muted,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TrainNumberBadge(train: LineTrainMarker) {
    val arrow = when (train.direction) {
        Direction.ASCENDING -> "▼"
        Direction.DESCENDING -> "▲"
        null -> ""
    }
    val bg = when {
        train.isExpress -> Hls.AccentDeep
        train.direction == Direction.DESCENDING -> Hls.Ink
        else -> Hls.Accent
    }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text = buildString {
                append(arrow)
                append(train.trainNo)
                if (train.isExpress) append(" 급")
            },
            color = Hls.OnAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RailMarker(isFirst: Boolean, isLast: Boolean, hasTrain: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        if (!isFirst) drawLine(Hls.Line, Offset(cx, 0f), Offset(cx, cy), 4f)
        if (!isLast) drawLine(Hls.Line, Offset(cx, cy), Offset(cx, size.height), 4f)
        if (hasTrain) {
            // 열차가 있는 역: 파란 링 + 흰 심
            drawCircle(Hls.Accent, radius = 16f, center = Offset(cx, cy))
            drawCircle(Hls.Paper, radius = 6f, center = Offset(cx, cy))
        } else {
            drawCircle(Hls.Paper, radius = 8f, center = Offset(cx, cy))
            drawCircle(Hls.Line, radius = 8f, center = Offset(cx, cy), style = Stroke(width = 3f))
        }
    }
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

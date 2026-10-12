package com.jin.subwayalarm.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jin.subwayalarm.domain.model.AlarmSetting
import com.jin.subwayalarm.ui.component.AlarmReadiness
import com.jin.subwayalarm.ui.component.HlsSwitch
import com.jin.subwayalarm.ui.component.PillShape
import com.jin.subwayalarm.ui.component.TextLink
import com.jin.subwayalarm.ui.component.openSettings
import com.jin.subwayalarm.ui.component.rememberAlarmReadiness
import com.jin.subwayalarm.ui.theme.Hls

/** 알람 설정 — 몇 정거장 전에, 어떻게 울릴지 + 알람이 확실히 울리는지 시스템 상태 점검. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val setting = uiState.setting
    val context = LocalContext.current
    val readiness = rememberAlarmReadiness()

    Scaffold(containerColor = Hls.Paper) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TextLink("← 홈", onClick = onBack, modifier = Modifier.padding(end = 0.dp))
                Text(
                    "알람 설정",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Hls.Ink,
                    modifier = Modifier.semantics { heading() },
                )
            }

            Section("알람 시점") {
                LeadSelector(selected = setting.leadStations, onSelect = viewModel::setLeadStations)
                Text(
                    buildAnnotatedString {
                        val station = uiState.previewStation
                        if (station != null) {
                            append("지금 경로라면 ")
                            withStyle(SpanStyle(color = Hls.Ink, fontWeight = FontWeight.Bold)) { append(station) }
                            append("에 들어설 때 울립니다. ")
                        } else {
                            append("내릴 역 ${setting.leadStations}정거장 전 역에 들어설 때 울립니다. ")
                        }
                        append("짐이 많거나 출입문이 먼 칸이라면 2정거장 전이 여유롭습니다.")
                    },
                    fontSize = 13.5.sp,
                    lineHeight = 21.sp,
                    color = Hls.Muted,
                )
            }

            Section("알림 방식") {
                ModeSelector(soundEnabled = setting.soundEnabled, onSelect = viewModel::setSoundEnabled)
                BorderedList {
                    SwitchRow(
                        label = "확인할 때까지 계속 울리기",
                        checked = setting.repeatUntilDismissed,
                        onCheckedChange = viewModel::setRepeatUntilDismissed,
                        divider = true,
                    )
                    SwitchRow(
                        label = "잠금화면에 크게 띄우기",
                        checked = setting.showOnLockScreen,
                        onCheckedChange = viewModel::setShowOnLockScreen,
                        divider = false,
                    )
                }
            }

            Section("알람이 확실히 울리려면") {
                BorderedList {
                    CheckRow(
                        label = "알림 허용",
                        ok = readiness.notificationsOn,
                        okText = "✓ 켜짐",
                        hint = "꺼져 있으면 알람이 울리지 않습니다",
                        onFix = { context.openSettings(AlarmReadiness.notificationSettingsIntent(context)) },
                        divider = true,
                    )
                    CheckRow(
                        label = "배터리 최적화 예외",
                        ok = readiness.batteryUnrestricted,
                        okText = "✓ 예외됨",
                        hint = "꺼져 있으면 화면이 꺼진 동안 확인이 늦어집니다",
                        onFix = { context.openSettings(AlarmReadiness.batterySettingsIntent()) },
                        divider = true,
                    )
                    CheckRow(
                        label = "전체화면 알림",
                        ok = readiness.fullScreenAllowed,
                        okText = "✓ 허용됨",
                        hint = "꺼져 있으면 잠금화면에 알람이 크게 뜨지 않습니다",
                        onFix = { context.openSettings(AlarmReadiness.fullScreenSettingsIntent(context)) },
                        divider = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Hls.Accent)) { append("§ ") }
                append(title)
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Hls.Ink,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/** 1·2·3정거장 전 — 세그먼트(선택 = 잉크 채움). */
@Composable
private fun LeadSelector(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Hls.Line, PillShape)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AlarmSetting.LEAD_STATION_CHOICES.forEach { n ->
            val on = n == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(PillShape)
                    .background(if (on) Hls.Ink else Hls.Paper)
                    .selectable(selected = on, role = Role.RadioButton) { onSelect(n) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${n}정거장 전",
                    fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) Hls.OnAccent else Hls.InkSoft,
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(soundEnabled: Boolean, onSelect: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Hls.Line, shape)
            .selectableGroup(),
    ) {
        ModeRow(
            label = "소리와 진동",
            sub = "알람 소리와 강한 진동을 함께",
            selected = soundEnabled,
            onClick = { onSelect(true) },
            divider = true,
        )
        ModeRow(
            label = "진동만",
            sub = "조용한 객차에서, 주머니 속 진동으로",
            selected = !soundEnabled,
            onClick = { onSelect(false) },
            divider = false,
        )
    }
}

@Composable
private fun ModeRow(label: String, sub: String, selected: Boolean, onClick: () -> Unit, divider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .background(if (selected) Hls.AccentTint else Hls.Paper)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Hls.Ink)
                Text(sub, fontSize = 12.5.sp, color = Hls.Muted)
            }
            if (selected) Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Hls.AccentDeep)
        }
        if (divider) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Hls.LineSoft))
    }
}

@Composable
private fun BorderedList(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(modifier = Modifier.fillMaxWidth().clip(shape).border(1.dp, Hls.Line, shape), content = content)
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, divider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(role = Role.Switch) { onCheckedChange(!checked) }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Hls.Ink, modifier = Modifier.weight(1f))
            HlsSwitch(checked = checked, onCheckedChange = onCheckedChange, label = label)
        }
        if (divider) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Hls.LineSoft))
    }
}

/** 시스템 상태 한 줄 — 괜찮으면 "✓ 켜짐", 아니면 이유와 "설정 열기 ↗". */
@Composable
private fun CheckRow(
    label: String,
    ok: Boolean,
    okText: String,
    hint: String,
    onFix: () -> Unit,
    divider: Boolean,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(start = 14.dp, end = if (ok) 14.dp else 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, fontSize = 14.5.sp, color = Hls.Ink)
                if (!ok) Text(hint, fontSize = 12.sp, color = Hls.Muted)
            }
            if (ok) {
                Text(okText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Hls.AccentDeep)
            } else {
                TextLink("설정 열기 ↗", onClick = onFix, color = Hls.AccentDeep, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }
        if (divider) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Hls.LineSoft))
    }
}

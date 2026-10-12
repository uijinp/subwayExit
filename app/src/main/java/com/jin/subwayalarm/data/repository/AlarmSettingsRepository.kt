package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.domain.model.AlarmSetting
import kotlinx.coroutines.flow.StateFlow

interface AlarmSettingsRepository {
    val settings: StateFlow<AlarmSetting>
    fun current(): AlarmSetting = settings.value
    fun update(transform: (AlarmSetting) -> AlarmSetting)
}

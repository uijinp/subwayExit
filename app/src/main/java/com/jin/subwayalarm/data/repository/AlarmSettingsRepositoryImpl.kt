package com.jin.subwayalarm.data.repository

import android.content.Context
import com.jin.subwayalarm.domain.model.AlarmSetting
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** 알람 설정을 SharedPreferences 에 저장. 값이 몇 개뿐이고 즉시 읽혀야 해서(서비스 틱) Room 대신 사용. */
@Singleton
class AlarmSettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : AlarmSettingsRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())

    override val settings: StateFlow<AlarmSetting> = state.asStateFlow()

    override fun update(transform: (AlarmSetting) -> AlarmSetting) {
        state.update { old ->
            val new = transform(old).let {
                it.copy(leadStations = it.leadStations.coerceIn(AlarmSetting.LEAD_STATION_CHOICES))
            }
            prefs.edit()
                .putBoolean(KEY_SOUND, new.soundEnabled)
                .putBoolean(KEY_REPEAT, new.repeatUntilDismissed)
                .putInt(KEY_LEAD, new.leadStations)
                .putBoolean(KEY_LOCK, new.showOnLockScreen)
                .apply()
            new
        }
    }

    private fun load(): AlarmSetting {
        val defaults = AlarmSetting()
        return AlarmSetting(
            soundEnabled = prefs.getBoolean(KEY_SOUND, defaults.soundEnabled),
            repeatUntilDismissed = prefs.getBoolean(KEY_REPEAT, defaults.repeatUntilDismissed),
            leadStations = prefs.getInt(KEY_LEAD, defaults.leadStations).coerceIn(AlarmSetting.LEAD_STATION_CHOICES),
            showOnLockScreen = prefs.getBoolean(KEY_LOCK, defaults.showOnLockScreen),
        )
    }

    private companion object {
        const val PREFS_NAME = "alarm_settings"
        const val KEY_SOUND = "sound"
        const val KEY_REPEAT = "repeat"
        const val KEY_LEAD = "lead_stations"
        const val KEY_LOCK = "show_on_lock_screen"
    }
}

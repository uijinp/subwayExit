package com.jin.subwayalarm.alarm

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService
import com.jin.subwayalarm.domain.model.VibrationPattern
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 시스템 Vibrator 얇은 래퍼 (4-3). 파형 계산은 [VibrationWaveforms] 순수 로직에 위임.
 */
@Singleton
class VibratorController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService<VibratorManager>()
            manager?.defaultVibrator ?: error("VibratorManager 를 가져올 수 없습니다.")
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>() ?: error("Vibrator 를 가져올 수 없습니다.")
        }
    }

    fun start(pattern: VibrationPattern, repeatUntilDismissed: Boolean) {
        if (!vibrator.hasVibrator()) return
        val waveform = VibrationWaveforms.forPattern(pattern, repeatUntilDismissed)
        val effect = VibrationEffect.createWaveform(
            waveform.timings.toLongArray(),
            waveform.amplitudes.toIntArray(),
            waveform.repeatFromIndex,
        )
        vibrator.vibrate(effect)
    }

    fun stop() {
        vibrator.cancel()
    }
}

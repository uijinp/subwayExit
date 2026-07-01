package com.jin.subwayalarm.alarm

import com.jin.subwayalarm.domain.model.VibrationPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VibrationWaveformsTest {

    @Test
    fun `STRONG_PULSE 반복재생 기본값은 repeatFromIndex 가 1`() {
        val w = VibrationWaveforms.forPattern(VibrationPattern.STRONG_PULSE, repeatUntilDismissed = true)
        assertEquals(1, w.repeatFromIndex)
        assertEquals(w.timings.size, w.amplitudes.size)
    }

    @Test
    fun `repeatUntilDismissed false 면 1회성(repeatFromIndex -1)으로 강제`() {
        val w = VibrationWaveforms.forPattern(VibrationPattern.STRONG_PULSE, repeatUntilDismissed = false)
        assertEquals(-1, w.repeatFromIndex)
    }

    @Test
    fun `첫 timing 0 첫 amplitude 0 (즉시 시작 규약)`() {
        val w = VibrationWaveforms.forPattern(VibrationPattern.STRONG_PULSE, repeatUntilDismissed = true)
        assertEquals(0L, w.timings.first())
        assertEquals(0, w.amplitudes.first())
    }

    @Test
    fun `timings amplitudes 길이가 다르면 예외`() {
        assertThrows(IllegalArgumentException::class.java) {
            Waveform(timings = listOf(0L, 100L), amplitudes = listOf(0), repeatFromIndex = -1)
        }
    }

    @Test
    fun `repeatFromIndex 가 범위를 벗어나면 예외`() {
        assertThrows(IllegalArgumentException::class.java) {
            Waveform(timings = listOf(0L, 100L), amplitudes = listOf(0, 255), repeatFromIndex = 5)
        }
    }

    @Test
    fun `timings 가 비어있으면 예외`() {
        assertThrows(IllegalArgumentException::class.java) {
            Waveform(timings = emptyList(), amplitudes = emptyList(), repeatFromIndex = -1)
        }
    }

    @Test
    fun `repeatFromIndex -1 은 허용(1회성)`() {
        val w = Waveform(timings = listOf(0L, 100L), amplitudes = listOf(0, 255), repeatFromIndex = -1)
        assertEquals(-1, w.repeatFromIndex)
    }
}

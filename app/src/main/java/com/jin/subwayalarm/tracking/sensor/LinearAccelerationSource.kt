package com.jin.subwayalarm.tracking.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * TYPE_LINEAR_ACCELERATION 샘플 Flow (약 50Hz 요청).
 * 센서 미지원 시 empty flow.
 */
@Singleton
class LinearAccelerationSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class Sample(val magnitude: Float, val epochMillis: Long)

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    fun samples(enabled: Boolean): Flow<Sample> = callbackFlow {
        if (!enabled) {
            close()
            return@callbackFlow
        }
        val manager = sensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (manager == null || sensor == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values.getOrElse(0) { 0f }
                val y = event.values.getOrElse(1) { 0f }
                val z = event.values.getOrElse(2) { 0f }
                val mag = sqrt(x * x + y * y + z * z)
                trySend(Sample(mag, System.currentTimeMillis()))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        // SENSOR_DELAY_GAME ≈ 50Hz
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { manager.unregisterListener(listener) }
    }
}

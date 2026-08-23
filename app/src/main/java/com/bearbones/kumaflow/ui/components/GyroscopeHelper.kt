package com.bearbones.kumaflow.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

data class TiltState(
    val x: Float = 0f, // -1f (left) to 1f (right)
    val y: Float = 0f  // -1f (forward/up) to 1f (backward/down)
)

@Composable
fun rememberTiltState(): State<TiltState> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(TiltState()) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            private var prevX = 0f
            private var prevY = 0f
            private val smoothing = 0.15f // Lower = smoother

            override fun onSensorChanged(event: SensorEvent) {
                // event.values[0] = x-axis (tilt left/right)
                // event.values[1] = y-axis (tilt forward/backward)
                // Gravity is ~9.8, so divide by 9.8 to normalize to -1..1
                val rawX = (event.values[0] / 9.8f).coerceIn(-1f, 1f)
                val rawY = (event.values[1] / 9.8f).coerceIn(-1f, 1f)

                // Smooth with exponential moving average
                prevX = prevX + (rawX - prevX) * smoothing
                prevY = prevY + (rawY - prevY) * smoothing

                tiltState.value = TiltState(x = prevX, y = prevY)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (gravitySensor != null) {
            sensorManager.registerListener(
                listener,
                gravitySensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return tiltState
}

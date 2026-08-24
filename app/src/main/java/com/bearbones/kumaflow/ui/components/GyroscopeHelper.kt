package com.bearbones.kumaflow.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

data class TiltState(
    val x: Float = 0f, // -1f (left) to 1f (right) — roll delta from baseline
    val y: Float = 0f  // -1f (forward/up) to 1f (backward/down) — pitch delta from baseline
)

@Composable
fun rememberTiltState(onShake: (() -> Unit)? = null): State<TiltState> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(TiltState()) }

    // Check if user has disabled animations (accessibility)
    val animationsEnabled = remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) > 0f
        } catch (_: Exception) { true }
    }

    DisposableEffect(animationsEnabled) {
        if (!animationsEnabled) {
            tiltState.value = TiltState()
            return@DisposableEffect onDispose {}
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Prefer GAME_ROTATION_VECTOR (no magnetometer interference)
        // Fall back to ROTATION_VECTOR
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        
        // Always get accelerometer for shake detection
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Reusable arrays — no allocations in onSensorChanged
        val rotMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            // Baseline calibration: first reading becomes "zero"
            private var baselinePitch = Float.NaN
            private var baselineRoll = Float.NaN

            // Smoothed output
            private var smoothX = 0f
            private var smoothY = 0f
            private val alpha = 0.12f // Lower = smoother, less jitter

            // Gravity fallback values
            private var gravX = 0f
            private var gravY = 0f

            private var lastShakeTime = 0L

            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                        SensorManager.getOrientation(rotMatrix, orientation)

                        val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat() // forward/back
                        val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()   // left/right

                        // Set baseline on first reading
                        if (baselinePitch.isNaN()) {
                            baselinePitch = pitch
                            baselineRoll = roll
                        }

                        // Delta from baseline, clamped to ±12°
                        val deltaPitch = (pitch - baselinePitch).coerceIn(-12f, 12f)
                        val deltaRoll = (roll - baselineRoll).coerceIn(-12f, 12f)

                        // Normalize to -1..1
                        val rawX = (deltaRoll / 12f)
                        val rawY = (deltaPitch / 12f)

                        // Exponential smoothing
                        smoothX += (rawX - smoothX) * alpha
                        smoothY += (rawY - smoothY) * alpha

                        tiltState.value = TiltState(x = smoothX, y = smoothY)
                    }

                    Sensor.TYPE_GRAVITY -> {
                        // Only used if no rotation vector sensor
                        if (rotationSensor != null) return

                        val rawX = (event.values[0] / 9.8f).coerceIn(-1f, 1f)
                        val rawY = (event.values[1] / 9.8f).coerceIn(-1f, 1f)

                        // Set baseline
                        if (baselinePitch.isNaN()) {
                            baselinePitch = rawY * 12f
                            baselineRoll = rawX * 12f
                        }

                        val deltaX = rawX - (baselineRoll / 12f)
                        val deltaY = rawY - (baselinePitch / 12f)

                        gravX += (deltaX.coerceIn(-1f, 1f) - gravX) * alpha
                        gravY += (deltaY.coerceIn(-1f, 1f) - gravY) * alpha

                        tiltState.value = TiltState(x = gravX, y = gravY)
                    }
                    
                    Sensor.TYPE_ACCELEROMETER -> {
                        // 1. Shake detection
                        if (onShake != null) {
                            val gX = event.values[0] / SensorManager.GRAVITY_EARTH
                            val gY = event.values[1] / SensorManager.GRAVITY_EARTH
                            val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
                            val gForce = Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()
                            
                            // 2.2g is a good shake threshold
                            if (gForce > 2.2f) {
                                val now = System.currentTimeMillis()
                                if (now - lastShakeTime > 1000) {
                                    lastShakeTime = now
                                    onShake.invoke()
                                }
                            }
                        }
                        
                        // 2. Fallback for tilt if both rotation and gravity fail
                        if (rotationSensor == null && gravitySensor == null) {
                            val rawX = (event.values[0] / 9.8f).coerceIn(-1f, 1f)
                            val rawY = (event.values[1] / 9.8f).coerceIn(-1f, 1f)

                            if (baselinePitch.isNaN()) {
                                baselinePitch = rawY * 12f
                                baselineRoll = rawX * 12f
                            }

                            val deltaX = rawX - (baselineRoll / 12f)
                            val deltaY = rawY - (baselinePitch / 12f)

                            gravX += (deltaX.coerceIn(-1f, 1f) - gravX) * alpha
                            gravY += (deltaY.coerceIn(-1f, 1f) - gravY) * alpha

                            tiltState.value = TiltState(x = gravX, y = gravY)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Register sensors
        rotationSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gravitySensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return tiltState
}

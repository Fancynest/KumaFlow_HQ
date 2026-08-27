package com.bearbones.kumaflow.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

data class TiltState(
    val x: Float = 0f, // -1f (left) to 1f (right)
    val y: Float = 0f  // -1f (forward) to 1f (backward)
)

@Composable
fun rememberTiltState(onShake: (() -> Unit)? = null): State<TiltState> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(TiltState()) }

    // Animated values for buttery smooth spring-based interpolation
    val animX = remember { Animatable(0f) }
    val animY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Target values that the sensor writes to (raw filtered)
    val targetX = remember { mutableFloatStateOf(0f) }
    val targetY = remember { mutableFloatStateOf(0f) }

    // Check if user has disabled animations (accessibility)
    val animationsEnabled = remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) > 0f
        } catch (_: Exception) { true }
    }

    // Spring animation that drives tiltState from sensor targets
    LaunchedEffect(Unit) {
        snapshotFlow { targetX.floatValue to targetY.floatValue }
            .collect { (tx, ty) ->
                launch {
                    animX.animateTo(
                        tx,
                        animationSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                launch {
                    animY.animateTo(
                        ty,
                        animationSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
    }

    // Update tiltState from animated values
    LaunchedEffect(Unit) {
        snapshotFlow { animX.value to animY.value }
            .collect { (ax, ay) ->
                tiltState.value = TiltState(x = ax, y = ay)
            }
    }

    DisposableEffect(animationsEnabled) {
        if (!animationsEnabled) {
            tiltState.value = TiltState()
            return@DisposableEffect onDispose {}
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Prefer GAME_ROTATION_VECTOR (no magnetometer noise)
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val rotMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            // Baseline calibration
            private var baselinePitch = Float.NaN
            private var baselineRoll = Float.NaN

            // Low-pass filter for sensor noise (separate from spring animation)
            private var lpX = 0f
            private var lpY = 0f
            private val lpAlpha = 0.15f // gentle low-pass to kill high-freq jitter

            private var lastShakeTime = 0L

            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                        SensorManager.getOrientation(rotMatrix, orientation)

                        val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                        val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

                        if (baselinePitch.isNaN()) {
                            baselinePitch = pitch
                            baselineRoll = roll
                        }

                        val maxAngle = 20f // tighter range = more responsive feel
                        val deltaPitch = (pitch - baselinePitch).coerceIn(-maxAngle, maxAngle)
                        val deltaRoll = (roll - baselineRoll).coerceIn(-maxAngle, maxAngle)

                        val rawX = deltaRoll / maxAngle
                        val rawY = deltaPitch / maxAngle

                        // Low-pass filter to remove sensor noise before spring animation
                        lpX += (rawX - lpX) * lpAlpha
                        lpY += (rawY - lpY) * lpAlpha

                        targetX.floatValue = lpX
                        targetY.floatValue = lpY
                    }

                    Sensor.TYPE_GRAVITY -> {
                        if (rotationSensor != null) return

                        val rawX = (event.values[0] / 9.8f).coerceIn(-1f, 1f)
                        val rawY = (event.values[1] / 9.8f).coerceIn(-1f, 1f)

                        if (baselinePitch.isNaN()) {
                            baselinePitch = rawY * 20f
                            baselineRoll = rawX * 20f
                        }

                        val deltaX = (rawX - (baselineRoll / 20f)).coerceIn(-1f, 1f)
                        val deltaY = (rawY - (baselinePitch / 20f)).coerceIn(-1f, 1f)

                        lpX += (deltaX - lpX) * lpAlpha
                        lpY += (deltaY - lpY) * lpAlpha

                        targetX.floatValue = lpX
                        targetY.floatValue = lpY
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        // Shake detection
                        if (onShake != null) {
                            val gX = event.values[0] / SensorManager.GRAVITY_EARTH
                            val gY = event.values[1] / SensorManager.GRAVITY_EARTH
                            val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
                            val gForce = Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

                            if (gForce > 2.2f) {
                                val now = System.currentTimeMillis()
                                if (now - lastShakeTime > 1000) {
                                    lastShakeTime = now
                                    onShake.invoke()
                                }
                            }
                        }

                        // Fallback tilt if both rotation and gravity fail
                        if (rotationSensor == null && gravitySensor == null) {
                            val rawX = (event.values[0] / 9.8f).coerceIn(-1f, 1f)
                            val rawY = (event.values[1] / 9.8f).coerceIn(-1f, 1f)

                            if (baselinePitch.isNaN()) {
                                baselinePitch = rawY * 20f
                                baselineRoll = rawX * 20f
                            }

                            val deltaX = (rawX - (baselineRoll / 20f)).coerceIn(-1f, 1f)
                            val deltaY = (rawY - (baselinePitch / 20f)).coerceIn(-1f, 1f)

                            lpX += (deltaX - lpX) * lpAlpha
                            lpY += (deltaY - lpY) * lpAlpha

                            targetX.floatValue = lpX
                            targetY.floatValue = lpY
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        rotationSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        gravitySensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        accelSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return tiltState
}

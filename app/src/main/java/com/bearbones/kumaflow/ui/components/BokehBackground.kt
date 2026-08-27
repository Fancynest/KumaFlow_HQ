package com.bearbones.kumaflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.bearbones.kumaflow.LocalIsDark
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.translate

@Composable
fun BokehBackground(
    isPaused: Boolean = false,
    scrollOffsetProvider: () -> Float = { 0f }
) {
    val isDark = LocalIsDark.current
    
    // Aesthetic dynamic colors for the bokeh mesh gradient
    val color1 = if (isDark) Color(0xFF673AB7).copy(alpha = 0.5f) else Color(0xFFFFB74D).copy(alpha = 0.4f)
    val color2 = if (isDark) Color(0xFF00695C).copy(alpha = 0.5f) else Color(0xFF4DD0E1).copy(alpha = 0.4f)
    val color3 = if (isDark) Color(0xFFC62828).copy(alpha = 0.4f) else Color(0xFFF06292).copy(alpha = 0.4f)

    // Manual animation loop that pauses when `isPaused` is true.
    // This saves GPU and battery when the background is obscured.
    var timeMillis by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(isPaused) {
        if (!isPaused) {
            var lastTime = androidx.compose.runtime.withFrameNanos { it }
            while (isActive) {
                val currentTime = androidx.compose.runtime.withFrameNanos { it }
                timeMillis += (currentTime - lastTime) / 1000000L
                lastTime = currentTime
            }
        }
    }

    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                
                val width = if (size.width.isNaN()) 1f else size.width
                val height = size.height

                // Cache the brushes (centered at 0,0) so they aren't recreated 60 times a second
                val brush1 = Brush.radialGradient(
                    colors = listOf(color1, Color.Transparent),
                    center = Offset.Zero,
                    radius = (width * 0.9f).coerceAtLeast(1f)
                )
                val brush2 = Brush.radialGradient(
                    colors = listOf(color2, Color.Transparent),
                    center = Offset.Zero,
                    radius = (width * 1.0f).coerceAtLeast(1f)
                )
                val brush3 = Brush.radialGradient(
                    colors = listOf(color3, Color.Transparent),
                    center = Offset.Zero,
                    radius = (width * 0.8f).coerceAtLeast(1f)
                )

                onDrawBehind {
                    // Read state in draw phase
                    val t = timeMillis / 10000f
                    val parallaxOffset = scrollOffsetProvider() * 0.15f

                    val cx1 = width * 0.2f + cos(t) * (width * 0.1f)
                    val cy1 = height * 0.1f + sin(t) * (height * 0.1f) - parallaxOffset

                    val cx2 = width * 0.8f + cos(t + 2f) * (width * 0.15f)
                    val cy2 = height * 0.6f + sin(t + 2f) * (height * 0.15f) - (parallaxOffset * 0.8f)

                    val cx3 = width * 0.3f + sin(t + 4f) * (width * 0.1f)
                    val cy3 = height * 0.9f + cos(t + 4f) * (height * 0.1f) - (parallaxOffset * 1.2f)

                    translate(left = cx1, top = cy1) {
                        drawCircle(brush = brush1, radius = (width * 0.9f).coerceAtLeast(1f), center = Offset.Zero)
                    }
                    translate(left = cx2, top = cy2) {
                        drawCircle(brush = brush2, radius = (width * 1.0f).coerceAtLeast(1f), center = Offset.Zero)
                    }
                    translate(left = cx3, top = cy3) {
                        drawCircle(brush = brush3, radius = (width * 0.8f).coerceAtLeast(1f), center = Offset.Zero)
                    }
                }
            }
    )
}

package com.bearbones.kumaflow.utils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.clip

/**
 * Material 3 Expressive bouncy scale modifier using spring physics and advanced haptics.
 */
fun Modifier.bouncyScale(
    interactionSource: InteractionSource,
    scaleDown: Float = 0.8f,
    disableHaptics: Boolean = false
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    
    LaunchedEffect(isPressed) {
        if (!disableHaptics) {
            if (isPressed) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Squish thud
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Release tick
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "BouncyScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * A combined clickable modifier that automatically applies the bouncy scale effect.
 */
fun Modifier.kumaClickable(
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    indication: androidx.compose.foundation.Indication? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: androidx.compose.ui.semantics.Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val actualInteractionSource = interactionSource ?: androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val actualIndication = indication ?: androidx.compose.foundation.LocalIndication.current
    this.then(Modifier.bouncyScale(actualInteractionSource))
        .clickable(
            interactionSource = actualInteractionSource,
            indication = actualIndication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick
        )
}

/**
 * Modifier to apply a spring-loaded scaling effect to bottom sheet content.
 */
fun Modifier.bouncySheetContent(): Modifier = composed {
    val isLaunched = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { isLaunched.value = true }

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isLaunched.value) 1f else 0.85f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "sheet_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
    }
}

/**
 * Material 3 Expressive Tonal Elevation.
 * Instead of black drop shadows, we blend a lighter overlay over the base color depending on elevation.
 */
fun Modifier.expressiveElevation(
    elevation: Dp,
    shape: Shape,
    baseColor: Color,
    tintColor: Color = Color.White
): Modifier = composed {
    val elevationPx = elevation.value
    // Algorithm: higher elevation = more tint color blended into base color
    val alpha = ((Math.log(elevationPx.toDouble() + 1) * 4.5f) / 100f).coerceIn(0.0, 1.0).toFloat()
    val tonalColor = tintColor.copy(alpha = alpha).compositeOver(baseColor)
    
    this.clip(shape).background(tonalColor)
}

class MorphPolygonShape(private val morph: Morph, private val progress: Float) : Shape {
    private var cachedSize: Size? = null
    private var cachedOutline: Outline? = null
    
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size == cachedSize && cachedOutline != null) return cachedOutline!!
        val matrix = android.graphics.Matrix()
        matrix.setScale(size.width / 2f, size.height / 2f)
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        val path = morph.toPath(progress = progress)
        path.transform(matrix)
        val outline = Outline.Generic(path.asComposePath())
        cachedSize = size
        cachedOutline = outline
        return outline
    }
}

class PolygonShape(private val polygon: RoundedPolygon) : Shape {
    private var cachedSize: Size? = null
    private var cachedOutline: Outline? = null
    
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size == cachedSize && cachedOutline != null) return cachedOutline!!
        val matrix = android.graphics.Matrix()
        matrix.setScale(size.width / 2f, size.height / 2f)
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        val path = polygon.toPath()
        path.transform(matrix)
        val outline = Outline.Generic(path.asComposePath())
        cachedSize = size
        cachedOutline = outline
        return outline
    }
}

val m3Shapes = listOf(
    RoundedPolygon.circle(),                                                                // Circle
    RoundedPolygon(4, rounding = CornerRounding(radius = 0.4f)),                            // Squircle
    RoundedPolygon(4, rounding = CornerRounding(radius = 0.15f)),                           // Rounded Square
    RoundedPolygon(3, rounding = CornerRounding(radius = 0.3f)),                            // Rounded Triangle
    RoundedPolygon(5, rounding = CornerRounding(radius = 0.3f)),                            // Pentagon
    RoundedPolygon(6, rounding = CornerRounding(radius = 0.2f)),                            // Hexagon
    RoundedPolygon(8, rounding = CornerRounding(radius = 0.15f)),                           // Octagon
    RoundedPolygon.star(4, innerRadius = 0.7f, rounding = CornerRounding(radius = 0.2f)),   // 4-point soft star
    RoundedPolygon.star(6, innerRadius = 0.75f, rounding = CornerRounding(radius = 0.15f)), // 6-point wavy
    RoundedPolygon.star(8, innerRadius = 0.85f, rounding = CornerRounding(radius = 0.1f))   // 8-point badge
)

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

/**
 * Material 3 Expressive bouncy scale modifier using spring physics.
 * Adapts to any interaction source so it works with clickable or combinedClickable.
 */
fun Modifier.bouncyScale(
    interactionSource: InteractionSource,
    scaleDown: Float = 0.85f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    
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

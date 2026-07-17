package com.bearbones.kumaflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bearbones.kumaflow.utils.PolygonShape
import com.bearbones.kumaflow.utils.m3Shapes
import kotlin.math.abs

@Composable
fun KumaExpressiveIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    containerColor: Color = tint.copy(alpha = 0.1f),
    size: Dp = 24.dp,
    iconPadding: Dp = 4.dp
) {
    // Generate a deterministic shape index based on the icon's name
    // This ensures the same icon always gets the same shape across the app
    val shapeIndex = remember(imageVector.name) {
        if (m3Shapes.isEmpty()) 0 else abs(imageVector.name.hashCode()) % m3Shapes.size
    }
    
    val shape = remember(shapeIndex) {
        PolygonShape(m3Shapes[shapeIndex])
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                this.shape = shape
                clip = true
            }
            .background(containerColor)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.padding(iconPadding).size(size - (iconPadding * 2))
        )
    }
}

package com.bearbones.kumaflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.bearbones.kumaflow.neobrutalism
import com.bearbones.kumaflow.ui.theme.LocalIsBrutal

@Composable
fun KumaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val isBrutal = LocalIsBrutal.current
    val brutalModifier = if (isBrutal) {
        Modifier.neobrutalism(
            isBrutal = true,
            cornerRadius = 100.dp, // Default button shape is stadium, so use a high corner radius
            borderWidth = 3.dp,
            offset = 4.dp
        )
    } else {
        Modifier
    }

    Button(
        onClick = onClick,
        modifier = modifier.then(brutalModifier),
        enabled = enabled,
        shape = if (isBrutal) androidx.compose.foundation.shape.RoundedCornerShape(100.dp) else shape,
        colors = colors,
        elevation = if (isBrutal) null else elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

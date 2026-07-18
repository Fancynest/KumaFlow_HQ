package com.bearbones.kumaflow.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bearbones.kumaflow.neobrutalism
import com.bearbones.kumaflow.ui.theme.LocalIsBrutal

@Composable
fun KumaOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = com.bearbones.kumaflow.AppSurface(),
        unfocusedContainerColor = com.bearbones.kumaflow.AppSurface(),
        disabledContainerColor = com.bearbones.kumaflow.AppSurface().copy(alpha = 0.5f),
        errorContainerColor = com.bearbones.kumaflow.AppSurface()
    )
) {
    val isBrutal = LocalIsBrutal.current
    val brutalModifier = if (isBrutal) {
        Modifier
            .padding(end = 4.dp, bottom = 4.dp)
            .neobrutalism(
                isBrutal = true,
                cornerRadius = 12.dp,
                borderWidth = 3.dp,
                offset = 4.dp
            )
    } else {
        Modifier
    }

    val brutalColors = if (isBrutal) {
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            errorBorderColor = Color.Transparent,
            focusedContainerColor = com.bearbones.kumaflow.AppSurface(),
            unfocusedContainerColor = com.bearbones.kumaflow.AppSurface(),
            disabledContainerColor = com.bearbones.kumaflow.AppSurface().copy(alpha = 0.5f),
            errorContainerColor = com.bearbones.kumaflow.AppSurface(),
            focusedTextColor = colors.focusedTextColor,
            unfocusedTextColor = colors.unfocusedTextColor,
            disabledTextColor = colors.disabledTextColor,
            errorTextColor = colors.errorTextColor,
            focusedLabelColor = colors.focusedLabelColor,
            unfocusedLabelColor = colors.unfocusedLabelColor,
            disabledLabelColor = colors.disabledLabelColor,
            errorLabelColor = colors.errorLabelColor,
            focusedPlaceholderColor = colors.focusedPlaceholderColor,
            unfocusedPlaceholderColor = colors.unfocusedPlaceholderColor,
            disabledPlaceholderColor = colors.disabledPlaceholderColor,
            errorPlaceholderColor = colors.errorPlaceholderColor,
            focusedLeadingIconColor = colors.focusedLeadingIconColor,
            unfocusedLeadingIconColor = colors.unfocusedLeadingIconColor,
            disabledLeadingIconColor = colors.disabledLeadingIconColor,
            errorLeadingIconColor = colors.errorLeadingIconColor,
            focusedTrailingIconColor = colors.focusedTrailingIconColor,
            unfocusedTrailingIconColor = colors.unfocusedTrailingIconColor,
            disabledTrailingIconColor = colors.disabledTrailingIconColor,
            errorTrailingIconColor = colors.errorTrailingIconColor,
            focusedPrefixColor = colors.focusedPrefixColor,
            unfocusedPrefixColor = colors.unfocusedPrefixColor,
            disabledPrefixColor = colors.disabledPrefixColor,
            errorPrefixColor = colors.errorPrefixColor,
            focusedSuffixColor = colors.focusedSuffixColor,
            unfocusedSuffixColor = colors.unfocusedSuffixColor,
            disabledSuffixColor = colors.disabledSuffixColor,
            errorSuffixColor = colors.errorSuffixColor,
            focusedSupportingTextColor = colors.focusedSupportingTextColor,
            unfocusedSupportingTextColor = colors.unfocusedSupportingTextColor,
            disabledSupportingTextColor = colors.disabledSupportingTextColor,
            errorSupportingTextColor = colors.errorSupportingTextColor,
            cursorColor = colors.cursorColor,
            errorCursorColor = colors.errorCursorColor
        )
    } else colors

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().then(brutalModifier),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactionSource,
            shape = if (isBrutal) androidx.compose.foundation.shape.RoundedCornerShape(12.dp) else shape,
            colors = brutalColors
        )
    }
}

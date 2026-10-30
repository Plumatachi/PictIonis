package com.etna.pictionis.ui.theme

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PixelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    borderColor: Color = Color.Black,
    focusedBorderColor: Color = Color.Black,
    textColor: Color = Color.Black,
    backgroundColor: Color = Color.White,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val cornerSize = 0.dp
    val shadowOffset = 4.dp

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.offset(x = shadowOffset, y = shadowOffset),
        label = label?.let {
            {
                Text(
                    text = it,
                    fontFamily = PixelFontFamily,
                    fontSize = 14.sp
                )
            }
        },
        placeholder = placeholder?.let {
            {
                Text(
                    text = it,
                    fontFamily = PixelFontFamily,
                    fontSize = 14.sp
                )
            }
        },
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(cornerSize),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = borderColor,
            errorBorderColor = Color.Red,
            focusedContainerColor = backgroundColor,
            unfocusedContainerColor = backgroundColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f),
            cursorColor = textColor,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            errorTextColor = Color.Red
        )
    )
}
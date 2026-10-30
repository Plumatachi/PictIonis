package com.etna.pictionis.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

// Bouton style pixel art avec bordure épaisse et coins carrés
@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    backgroundColor: Color,
    borderColor: Color = Color.Black,
    shadowColor: Color = Color(0xFF000000),
    textColor: Color = Color.White,
    enabled: Boolean = true
) {
    val cornerSize = 0.dp // Coins carrés
    val borderWidth = 3.dp // Bordure épaisse
    val shadowOffset = 4.dp

    Button(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = 0.dp,
                spotColor = shadowColor,
                ambientColor = shadowColor
            )
            .offset(x = shadowOffset, y = shadowOffset),
        enabled = enabled,
        shape = RoundedCornerShape(cornerSize),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f),
            disabledContentColor = textColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
    }
}

// Bouton outlined style pixel art
@Composable
fun PixelOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    borderColor: Color,
    textColor: Color,
    enabled: Boolean = true
) {
    val cornerSize = 0.dp
    val borderWidth = 3.dp
    val shadowOffset = 4.dp

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = 0.dp,
                spotColor = Color(0xFF000000),
                ambientColor = Color(0xFF000000)
            )
            .offset(x = shadowOffset, y = shadowOffset),
        enabled = enabled,
        shape = RoundedCornerShape(cornerSize),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = textColor,
            disabledContentColor = textColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
    }
}

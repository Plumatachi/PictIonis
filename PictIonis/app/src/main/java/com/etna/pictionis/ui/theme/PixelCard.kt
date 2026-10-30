package com.etna.pictionis.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Card style pixel art
@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    borderColor: Color = Color.Black,
    shadowColor: Color = Color(0xFF000000),
    shadowOffset: Dp = 4.dp,
    borderWidth: Dp = 3.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val cornerSize = 0.dp // Coins carrés

    Card(
        modifier = modifier
            .offset(x = shadowOffset, y = shadowOffset),
        shape = RoundedCornerShape(cornerSize),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(borderWidth, borderColor),
        content = content
    )
}

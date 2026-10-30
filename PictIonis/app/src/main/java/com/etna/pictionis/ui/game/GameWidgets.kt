package com.etna.pictionis.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etna.pictionis.ui.profile.initialsOf
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.PictHint
import com.etna.pictionis.ui.theme.PixelCard
import com.etna.pictionis.ui.theme.contentColor

// Petits composants partagés par les écrans de partie.

@Composable
fun SectionLabel(text: String) {
    Text(text = text, color = PictHint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
}

// Carte blanche des corps clairs (même style que la carte stats du profil).
@Composable
fun WhiteCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalAppPalette.current
    PixelCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = Color.White,
        borderColor = palette.border
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

// Rangée de pastilles à choix unique (manches, durée, mode...).
@Composable
fun <T> OptionChips(
    label: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    val palette = LocalAppPalette.current
    Text(
        text = label,
        color = palette.onLight,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val shape = RoundedCornerShape(10.dp)
            Text(
                text = labelOf(option),
                color = if (isSelected) palette.secondary.contentColor() else palette.onLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(shape)
                    .background(if (isSelected) palette.secondary else Color.White)
                    .border(1.dp, if (isSelected) palette.secondary else palette.border, shape)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) }
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

// Pastille aux initiales du joueur, grisée s'il est hors ligne.
@Composable
fun PlayerAvatar(name: String, size: Dp = 36.dp, dimmed: Boolean = false) {
    val palette = LocalAppPalette.current
    val bg = if (dimmed) PictHint else palette.primary
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialsOf(name),
            color = bg.contentColor(),
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// Bouton secondaire (contour) des pages de partie, ex. "Quitter".
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, palette.secondary),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = palette.onLight
        )
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

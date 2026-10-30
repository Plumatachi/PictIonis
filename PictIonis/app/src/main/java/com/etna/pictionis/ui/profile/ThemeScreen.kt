package com.etna.pictionis.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etna.pictionis.data.ThemeSettings
import com.etna.pictionis.ui.theme.AppPalette
import com.etna.pictionis.ui.theme.AppPalettes
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.PictHint
import com.etna.pictionis.ui.theme.PixelCard
import com.etna.pictionis.ui.theme.accentOnLight
import com.etna.pictionis.ui.theme.contentColor

// Page "Thème et couleurs" : liste toutes les palettes (Design/Palettes).
// Ouverte depuis la ligne "Theme et couleurs" du profil, le choix est
// persisté via ThemeSettings et s'applique à toute l'interface.
@Composable
fun ThemeScreen(
    onBack: () -> Unit = {},
    onSelect: (AppPalette) -> Unit = { ThemeSettings.select(it.id) }
) {
    val palette = LocalAppPalette.current
    val onBackground = palette.background.contentColor()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // --- Header : retour + titre ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 24.dp, top = 56.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                color = onBackground,
                fontSize = 36.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "Thème et couleurs",
                    color = onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choisis la palette de ton interface",
                    color = palette.accent,
                    fontSize = 13.sp
                )
            }
        }

        // --- Corps clair : cartes des palettes ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(palette.body)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppPalettes.forEach { item ->
                ThemePaletteCard(
                    item = item,
                    selected = item.id == palette.id,
                    onClick = { onSelect(item) }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// Une carte = bandeau des 5 couleurs brutes + nom + badge si active.
@Composable
private fun ThemePaletteCard(
    item: AppPalette,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    PixelCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        backgroundColor = palette.background,
        borderColor = if (selected) palette.accentOnLight() else palette.border,
        borderWidth = if (selected) 4.dp else 3.dp
    ) {
        // Bandeau : les 5 couleurs brutes de la palette
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            item.preview.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.name,
                color = palette.background.contentColor(),
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                val badge = palette.accentOnLight()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(badge)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Activé",
                        color = badge.contentColor(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(text = "›", color = PictHint, fontSize = 22.sp)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ThemeScreenPreview() {
    ThemeScreen()
}

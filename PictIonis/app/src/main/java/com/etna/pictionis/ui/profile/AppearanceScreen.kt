package com.etna.pictionis.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etna.pictionis.data.AppearanceMode
import com.etna.pictionis.data.ThemeSettings
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.PictHint
import com.etna.pictionis.ui.theme.accentOnLight

// Page "Apparence" : interface claire, sombre, ou qui suit le système
// du téléphone (défaut). Le choix est immédiat et persisté en local.
@Composable
fun AppearanceScreen(
    onBack: () -> Unit = {}
) {
    val palette = LocalAppPalette.current

    SettingsPageScaffold(
        title = "Apparence",
        subtitle = "Mode lumineux ou sombre de l'interface",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(vertical = 4.dp)
        ) {
            AppearanceMode.entries.forEach { mode ->
                AppearanceRow(
                    mode = mode,
                    selected = mode == ThemeSettings.appearanceMode,
                    onClick = { ThemeSettings.setAppearance(mode) }
                )
            }
        }
    }
}

@Composable
private fun AppearanceRow(
    mode: AppearanceMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pastille radio dessinée en Canvas (cohérent avec les autres icônes)
        Canvas(modifier = Modifier.size(22.dp)) {
            val color = palette.accentOnLight()
            val outline = Stroke(width = size.width * 0.12f)
            drawCircle(
                color = color,
                radius = size.width * 0.42f,
                style = outline
            )
            if (selected) {
                drawCircle(
                    color = color,
                    radius = size.width * 0.22f
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.label,
                color = palette.onLight,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = mode.description,
                color = PictHint,
                fontSize = 12.sp
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AppearanceScreenPreview() {
    AppearanceScreen()
}

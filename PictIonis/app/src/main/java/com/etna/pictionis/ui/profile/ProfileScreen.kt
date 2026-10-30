package com.etna.pictionis.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etna.pictionis.data.ThemeSettings
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.PictDanger
import com.etna.pictionis.ui.theme.PictHint
import com.etna.pictionis.ui.theme.accentOnLight
import com.etna.pictionis.ui.theme.contentColor

// Page profil d'après maquette.
// Les stats et le niveau sont des paramètres : 0 tant que Firestore
// n'est pas branché, la preview affiche les valeurs de la maquette.
@Composable
fun ProfileScreen(
    email: String? = null,
    displayName: String? = null,
    emailVerified: Boolean = false,
    level: Int = 0,
    levelTitle: String = "débutant",
    gamesPlayed: Int = 0,
    winRate: Int = 0,
    wordsFound: Int = 0,
    onEditUsername: () -> Unit = {},
    onEmail: () -> Unit = {},
    onPassword: () -> Unit = {},
    onAppearance: () -> Unit = {},
    onTheme: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val name = displayName
        ?: email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
        ?: "Joueur"

    val palette = LocalAppPalette.current
    val onBackground = palette.background.contentColor()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // --- Header : avatar + nom + niveau ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(palette.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initialsOf(name),
                    color = palette.primary.contentColor(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = name,
                color = onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "niveau $level - $levelTitle",
                color = palette.accent,
                fontSize = 14.sp
            )
        }

        // --- Corps aux coins hauts arrondis ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(palette.body)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Carte stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.background)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCell(value = "$gamesPlayed", label = "parties")
                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 40.dp)
                        .background(palette.border)
                )
                StatCell(value = "$winRate%", label = "victoires")
                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 40.dp)
                        .background(palette.border)
                )
                StatCell(value = "$wordsFound", label = "mots trouvés")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PARAMETRES",
                color = PictHint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Carte paramètres : chaque ligne ouvre sa page dédiée.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.background)
            ) {
                SettingRow(
                    title = "Nom d'utilisateur",
                    glyph = SettingGlyph.PERSON,
                    onClick = onEditUsername
                )
                HorizontalDivider(color = palette.border, thickness = 1.dp)
                SettingRow(
                    title = "Adresse email",
                    subtitle = email?.let { "$it • ${if (emailVerified) "vérifié" else "à vérifier"}" },
                    glyph = SettingGlyph.MAIL,
                    onClick = onEmail
                )
                HorizontalDivider(color = palette.border, thickness = 1.dp)
                SettingRow(
                    title = "Mot de passe",
                    subtitle = "••••••••",
                    glyph = SettingGlyph.LOCK,
                    onClick = onPassword
                )
                HorizontalDivider(color = palette.border, thickness = 1.dp)
                SettingRow(
                    title = "Apparence",
                    subtitle = ThemeSettings.appearanceMode.label,
                    glyph = SettingGlyph.MOON,
                    onClick = onAppearance
                )
                HorizontalDivider(color = palette.border, thickness = 1.dp)
                SettingRow(
                    title = "Theme et couleurs",
                    subtitle = palette.name,
                    glyph = SettingGlyph.PALETTE,
                    onClick = onTheme
                )
                HorizontalDivider(color = palette.border, thickness = 1.dp)
                SettingRow(
                    title = "Déconnexion",
                    glyph = SettingGlyph.EXIT,
                    danger = true,
                    onClick = onSignOut
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Initiales : "Lucie.M" -> "LM", "toi" -> "T".
internal fun initialsOf(name: String): String {
    val parts = name.split('.', ' ', '-', '_').filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts[0].take(2).uppercase()
    return (parts[0].first() + "" + parts[1].first()).uppercase()
}

@Composable
private fun StatCell(value: String, label: String) {
    val accentOnLight = LocalAppPalette.current.accentOnLight()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = accentOnLight,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = label, color = PictHint, fontSize = 12.sp)
    }
}

@Composable
private fun SettingRow(
    title: String,
    glyph: SettingGlyph,
    danger: Boolean = false,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val contentColor = if (danger) PictDanger else palette.background.contentColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingGlyphIcon(kind = glyph, danger = danger)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = PictHint,
                    fontSize = 12.sp
                )
            }
        }
        Text(text = "›", color = PictHint, fontSize = 22.sp)
    }
}

private enum class SettingGlyph { PERSON, MAIL, LOCK, MOON, PALETTE, BELL, EXIT }

// Petites icônes des lignes paramètres, dessinées en Canvas
// (accent de la palette, rouge pour la déconnexion).
@Composable
private fun SettingGlyphIcon(kind: SettingGlyph, danger: Boolean) {
    val color = if (danger) PictDanger else LocalAppPalette.current.accentOnLight()
    val palette = LocalAppPalette.current
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val pixelSize = w * 0.12f
        val outline = Stroke(width = pixelSize, cap = StrokeCap.Square)
        when (kind) {
            SettingGlyph.PERSON -> {
                // Tête - carré
                drawRect(
                    color = color,
                    size = Size(pixelSize * 2, pixelSize * 2),
                    topLeft = Offset(w * 0.38f, h * 0.20f),
                    style = outline
                )
                // Corps - rectangle
                drawRect(
                    color = color,
                    size = Size(pixelSize * 3, pixelSize * 3),
                    topLeft = Offset(w * 0.26f, h * 0.44f),
                    style = outline
                )
            }
            SettingGlyph.PALETTE -> {
                // Pastille - carré
                drawRect(
                    color = color,
                    size = Size(pixelSize * 4, pixelSize * 4),
                    topLeft = Offset(w * 0.26f, h * 0.26f),
                    style = outline
                )
                // 3 points de couleur - carrés
                drawRect(
                    color = color,
                    size = Size(pixelSize, pixelSize),
                    topLeft = Offset(w * 0.32f, h * 0.38f)
                )
                drawRect(
                    color = color,
                    size = Size(pixelSize, pixelSize),
                    topLeft = Offset(w * 0.50f, h * 0.32f)
                )
                drawRect(
                    color = color,
                    size = Size(pixelSize, pixelSize),
                    topLeft = Offset(w * 0.44f, h * 0.50f)
                )
            }
            SettingGlyph.MAIL -> {
                // Enveloppe - rectangle
                drawRect(
                    color = color,
                    size = Size(pixelSize * 5, pixelSize * 3),
                    topLeft = Offset(w * 0.14f, h * 0.32f),
                    style = outline
                )
                // Rabat en V - lignes
                drawLine(
                    color = color,
                    start = Offset(w * 0.14f, h * 0.32f),
                    end = Offset(w * 0.50f, h * 0.56f),
                    strokeWidth = pixelSize,
                    cap = StrokeCap.Square
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.86f, h * 0.32f),
                    end = Offset(w * 0.50f, h * 0.56f),
                    strokeWidth = pixelSize,
                    cap = StrokeCap.Square
                )
            }
            SettingGlyph.LOCK -> {
                // Anse - forme U pixel
                drawLine(
                    color = color,
                    start = Offset(w * 0.32f, h * 0.14f),
                    end = Offset(w * 0.32f, h * 0.38f),
                    strokeWidth = pixelSize,
                    cap = StrokeCap.Square
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.68f, h * 0.14f),
                    end = Offset(w * 0.68f, h * 0.38f),
                    strokeWidth = pixelSize,
                    cap = StrokeCap.Square
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.32f, h * 0.14f),
                    end = Offset(w * 0.68f, h * 0.14f),
                    strokeWidth = pixelSize,
                    cap = StrokeCap.Square
                )
                // Corps - rectangle
                drawRect(
                    color = color,
                    size = Size(pixelSize * 4, pixelSize * 3),
                    topLeft = Offset(w * 0.20f, h * 0.38f),
                    style = outline
                )
                // Trou de serrure - carré
                drawRect(
                    color = color,
                    size = Size(pixelSize, pixelSize),
                    topLeft = Offset(w * 0.44f, h * 0.56f)
                )
            }
            SettingGlyph.MOON -> {
                // Croissant - forme pixel
                drawRect(
                    color = color,
                    size = Size(pixelSize * 3, pixelSize * 4),
                    topLeft = Offset(w * 0.32f, h * 0.32f)
                )
                // Découpe - carré de la couleur du fond
                drawRect(
                    color = palette.body,
                    size = Size(pixelSize * 2, pixelSize * 3),
                    topLeft = Offset(w * 0.44f, h * 0.38f)
                )
                drawCircle(
                    color = palette.background,
                    radius = w * 0.27f,
                    center = Offset(w * 0.58f, h * 0.40f)
                )
            }
            SettingGlyph.BELL -> {
                // Dôme pixel + battant
                drawRect(
                    color = color,
                    size = Size(pixelSize * 4, pixelSize * 2),
                    topLeft = Offset(w * 0.20f, h * 0.44f),
                    style = outline
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.14f, h * 0.68f),
                    end = Offset(w * 0.86f, h * 0.68f),
                    strokeWidth = pixelSize,
                    cap = StrokeCap.Square
                )
                drawRect(
                    color = color,
                    size = Size(pixelSize, pixelSize),
                    topLeft = Offset(w * 0.44f, h * 0.80f)
                )
            }
            SettingGlyph.EXIT -> {
                // Flèche qui sort d'un montant de porte - style pixel
                drawLine(
                    color = color,
                    start = Offset(w * 0.22f, h * 0.10f),
                    end = Offset(w * 0.22f, h * 0.90f),
                    strokeWidth = pixelSize,
                    cap = StrokeCap.Square
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.22f, h * 0.50f),
                    end = Offset(w * 0.78f, h * 0.50f),
                    strokeWidth = pixelSize,
                    cap = StrokeCap.Square
                )
                // Pointe de flèche
                drawLine(
                    color = color,
                    start = Offset(w * 0.62f, h * 0.38f),
                    end = Offset(w * 0.78f, h * 0.50f),
                    strokeWidth = pixelSize,
                    cap = StrokeCap.Square
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.62f, h * 0.62f),
                    end = Offset(w * 0.78f, h * 0.50f),
                    strokeWidth = pixelSize,
                    cap = StrokeCap.Square
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(
        displayName = "Lucie.M",
        level = 7,
        levelTitle = "artiste confirmee",
        gamesPlayed = 128,
        winRate = 64,
        wordsFound = 312
    )
}

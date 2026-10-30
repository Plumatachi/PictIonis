package com.etna.pictionis.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etna.pictionis.data.game.CODE_LENGTH
import com.etna.pictionis.data.game.Game
import com.etna.pictionis.data.game.GameManager
import com.etna.pictionis.data.game.GameStatus
import com.etna.pictionis.data.game.MAX_PLAYERS
import com.etna.pictionis.ui.profile.SettingsErrorText
import com.etna.pictionis.ui.profile.SettingsPageScaffold
import com.etna.pictionis.ui.profile.SettingsPrimaryButton
import com.etna.pictionis.ui.profile.SettingsTextField
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.PictHint
import com.etna.pictionis.ui.theme.accentOnLight

// Rejoindre : par code, ou depuis la liste des parties publiques
// (en attente ou déjà lancées, plusieurs parties tournent en parallèle).
@Composable
fun JoinGameScreen(onBack: () -> Unit = {}) {
    var code by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { GameManager.clearMessages() }
    DisposableEffect(Unit) {
        GameManager.watchOpenGames()
        onDispose { GameManager.stopWatchingOpenGames() }
    }

    SettingsPageScaffold(
        title = "Rejoindre une partie",
        subtitle = "Entre un code ou choisis une partie en cours",
        onBack = onBack
    ) {
        SettingsTextField(
            label = "Code de la partie",
            value = code,
            onValueChange = { if (it.length <= CODE_LENGTH) code = it.uppercase() },
            placeholder = "Ex : K7QM2X"
        )
        Spacer(modifier = Modifier.height(16.dp))

        GameManager.errorMessage?.let { SettingsErrorText(it) }

        SettingsPrimaryButton(
            text = if (GameManager.isLoading) "Patiente..." else "Rejoindre",
            enabled = !GameManager.isLoading && code.length == CODE_LENGTH,
            onClick = { GameManager.joinGame(code) }
        )
        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("PARTIES EN COURS")
        val games = GameManager.openGames
        if (games.isEmpty()) {
            Text(
                text = "Aucune partie publique pour l'instant. Crée la tienne !",
                color = PictHint,
                fontSize = 14.sp
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                games.forEach { game ->
                    OpenGameRow(
                        game = game,
                        enabled = !GameManager.isLoading,
                        onClick = { GameManager.joinGame(game.id) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun OpenGameRow(game: Game, enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    val online = game.onlinePlayers.size
    val full = online >= MAX_PLAYERS
    WhiteCard(
        modifier = Modifier.clickable(enabled = enabled && !full, onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.name,
                    color = palette.onLight,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$online/$MAX_PLAYERS joueurs • " +
                        if (game.status == GameStatus.LOBBY) "en attente" else "en cours",
                    color = PictHint,
                    fontSize = 12.sp
                )
            }
            Text(
                text = if (full) "complète" else game.id,
                color = palette.accentOnLight(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

package com.etna.pictionis.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etna.pictionis.data.game.Game
import com.etna.pictionis.data.game.GameSettings
import com.etna.pictionis.data.game.GameStatus
import com.etna.pictionis.data.game.MAX_PLAYERS
import com.etna.pictionis.data.game.MIN_PLAYERS
import com.etna.pictionis.data.game.Player
import com.etna.pictionis.ui.profile.SettingsErrorText
import com.etna.pictionis.ui.profile.SettingsPageScaffold
import com.etna.pictionis.ui.profile.SettingsPrimaryButton
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.PictHint
import com.etna.pictionis.ui.theme.accentOnLight

// Salle d'attente : code à partager, joueurs connectés, lancement par l'hôte.
@Composable
fun LobbyScreen(
    game: Game,
    myUid: String?,
    errorMessage: String? = null,
    onStart: () -> Unit = {},
    onLeave: () -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val isHost = game.hostUid == myUid
    val canStart = game.onlinePlayers.size >= MIN_PLAYERS

    BackHandler(onBack = onLeave)

    SettingsPageScaffold(
        title = game.name,
        subtitle = "Salle d'attente",
        onBack = onLeave
    ) {
        WhiteCard {
            Text(
                text = "CODE DE LA PARTIE",
                color = PictHint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = game.id,
                color = palette.accentOnLight(),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Partage ce code à tes amis pour qu'ils te rejoignent",
                color = PictHint,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel("JOUEURS (${game.onlinePlayers.size}/$MAX_PLAYERS)")
        WhiteCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                game.players.forEach { player ->
                    PlayerRow(player = player, isHost = player.uid == game.hostUid, isMe = player.uid == myUid)
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel("RÈGLES")
        Text(
            text = settingsSummary(game.settings),
            color = palette.onLight,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        errorMessage?.let { SettingsErrorText(it) }

        if (isHost) {
            SettingsPrimaryButton(
                text = if (canStart) "Lancer la partie" else "En attente de joueurs ($MIN_PLAYERS minimum)",
                enabled = canStart,
                onClick = onStart
            )
        } else {
            Text(
                text = "En attente du lancement par l'hôte...",
                color = PictHint,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryButton(text = "Quitter", onClick = onLeave)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PlayerRow(player: Player, isHost: Boolean, isMe: Boolean) {
    val palette = LocalAppPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        PlayerAvatar(name = player.name, dimmed = !player.online)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.name + if (isMe) " (toi)" else "",
                color = palette.onLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (!player.online) {
                Text(text = "hors ligne", color = PictHint, fontSize = 12.sp)
            }
        }
        if (isHost) {
            Text(
                text = "hôte",
                color = palette.accentOnLight(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun settingsSummary(settings: GameSettings): String =
    "Mode ${settings.mode.label} • mots ${settings.difficulty.label.lowercase()}\n" +
        "${settings.rounds} manche${if (settings.rounds > 1) "s" else ""} • " +
        "${settings.roundSeconds} s par tour • " +
        (if (settings.isPublic) "publique" else "privée") +
        when {
            settings.customOnly -> "\nUniquement les ${settings.customWords.size} mots personnalisés"
            settings.customWords.isNotEmpty() -> "\n+ ${settings.customWords.size} mots personnalisés"
            else -> ""
        }

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LobbyScreenPreview() {
    LobbyScreen(
        game = Game(
            id = "K7QM2X",
            name = "Partie de Lucie",
            hostUid = "a",
            status = GameStatus.LOBBY,
            settings = GameSettings(),
            players = listOf(
                Player(uid = "a", name = "Lucie.M"),
                Player(uid = "b", name = "Malo"),
                Player(uid = "c", name = "Iris", online = false)
            ),
            order = emptyList(),
            round = null
        ),
        myUid = "a"
    )
}

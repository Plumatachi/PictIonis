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
import com.etna.pictionis.data.game.Player
import com.etna.pictionis.ui.profile.SettingsPageScaffold
import com.etna.pictionis.ui.profile.SettingsPrimaryButton
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.PictHint
import com.etna.pictionis.ui.theme.accentOnLight

// Fin de partie : vainqueur(s) et classement final.
@Composable
fun ResultsScreen(
    game: Game,
    myUid: String?,
    onLeave: () -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val ranking = game.players.sortedByDescending { it.score }
    val best = ranking.firstOrNull()?.score ?: 0
    val winners = ranking.filter { it.score == best }

    BackHandler(onBack = onLeave)

    SettingsPageScaffold(
        title = "Partie terminée",
        subtitle = game.name,
        onBack = onLeave
    ) {
        WhiteCard {
            Text(
                text = when {
                    winners.size > 1 -> "Égalité !"
                    winners.firstOrNull()?.uid == myUid -> "Tu as gagné !"
                    else -> "${winners.firstOrNull()?.name ?: "?"} gagne !"
                },
                color = palette.accentOnLight(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (winners.size > 1) winners.joinToString(", ") { it.name } + " avec $best points"
                else "avec $best points",
                color = PictHint,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel("CLASSEMENT")
        WhiteCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ranking.forEachIndexed { index, player ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${index + 1}",
                            color = palette.onLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp)
                        )
                        PlayerAvatar(name = player.name, dimmed = !player.online)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = player.name + if (player.uid == myUid) " (toi)" else "",
                            color = palette.onLight,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${player.score} pts",
                            color = palette.accentOnLight(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        SettingsPrimaryButton(text = "Retour à l'accueil", onClick = onLeave)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ResultsScreenPreview() {
    ResultsScreen(
        game = Game(
            id = "K7QM2X",
            name = "Partie de Lucie",
            hostUid = "a",
            status = GameStatus.FINISHED,
            settings = GameSettings(),
            players = listOf(
                Player(uid = "a", name = "Lucie.M", score = 184),
                Player(uid = "b", name = "Malo", score = 125),
                Player(uid = "c", name = "Iris", score = 60)
            ),
            order = emptyList(),
            round = null
        ),
        myUid = "a"
    )
}

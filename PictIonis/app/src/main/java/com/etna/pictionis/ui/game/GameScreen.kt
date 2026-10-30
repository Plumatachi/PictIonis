package com.etna.pictionis.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etna.pictionis.data.game.Game
import com.etna.pictionis.data.game.GameManager
import com.etna.pictionis.data.game.GameMode
import com.etna.pictionis.data.game.GameSettings
import com.etna.pictionis.data.game.GameStatus
import com.etna.pictionis.data.game.Player
import com.etna.pictionis.data.game.Round
import com.etna.pictionis.data.game.formatCountdown
import com.etna.pictionis.data.game.hintIndices
import com.etna.pictionis.data.game.maskWord
import com.etna.pictionis.ui.profile.SettingsErrorText
import com.etna.pictionis.ui.profile.SettingsPrimaryButton
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.PictDanger
import com.etna.pictionis.ui.theme.PictHint
import com.etna.pictionis.ui.theme.contentColor
import kotlinx.coroutines.delay

private enum class GameTab(val label: String) { CHAT("chat"), TERMINAL("terminal") }

// Écran de jeu d'après maquette : header (minuteur, mot, joueurs), zone de
// dessin, puis chat ou terminal. L'affichage dépend du rôle : le dessinateur
// voit le mot et les outils, les autres voient le mot masqué.
@Composable
fun GameScreen(
    game: Game,
    myUid: String?,
    onLeave: () -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val round = game.round
    val drawer = game.player(round?.drawerUid)
    val isDrawer = drawer != null && drawer.uid == myUid
    val hasFound = myUid != null && round?.found?.containsKey(myUid) == true
    val word = round?.word.orEmpty()
    val isChoosing = round?.isChoosing == true
    var confirmLeave by remember { mutableStateOf(false) }
    var showScores by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(GameTab.CHAT) }
    var now by remember { mutableLongStateOf(GameManager.serverNow()) }

    // Minuteur : rafraîchit l'affichage et laisse le manager clore le tour.
    LaunchedEffect(Unit) {
        while (true) {
            now = GameManager.serverNow()
            GameManager.tick()
            delay(250)
        }
    }
    val remaining = (round?.endsAt ?: now) - now
    val revealed = if (game.settings.mode == GameMode.HINTS && round != null) {
        hintIndices(word, round.turn, 1.0 - remaining.toDouble() / round.durationMs)
    } else {
        emptySet()
    }

    BackHandler { confirmLeave = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.lightBg)
            .navigationBarsPadding()
            .imePadding() // le champ du chat reste visible au-dessus du clavier
    ) {
        GameHeader(
            title = when {
                isChoosing && isDrawer -> "choisis ton mot"
                isChoosing -> "${drawer?.name ?: "?"} choisit un mot"
                isDrawer -> "à toi de dessiner"
                hasFound -> "bravo, tu as trouvé !"
                else -> "mot à deviner"
            },
            subtitle = when {
                isChoosing -> "..."
                isDrawer || hasFound -> word.uppercase()
                else -> maskWord(word, revealed)
            },
            leading = formatCountdown(remaining),
            leadingAlert = remaining < 10_000,
            playerCount = game.onlinePlayers.size,
            onLeave = { confirmLeave = true },
            onPlayersClick = { showScores = true }
        )
        Text(
            text = "mode ${game.settings.mode.label.lowercase()} • tour ${(round?.turn ?: 0) + 1}/${game.totalTurns} • " +
                if (isDrawer) "tu dessines" else "${drawer?.name ?: "?"} dessine",
            color = PictHint,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        )

        DrawingBoard(
            strokes = GameManager.strokes,
            canDraw = isDrawer && !isChoosing,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp),
            onBegin = GameManager::beginStroke,
            onAppend = GameManager::appendStrokePoints,
            onClear = GameManager::clearDrawing
        )

        TabSwitch(selected = tab, onSelect = { tab = it })

        GameManager.errorMessage?.let {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) { SettingsErrorText(it) }
        }

        val panelModifier = Modifier
            .weight(1f)
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
        when (tab) {
            GameTab.CHAT -> ChatPanel(
                messages = GameManager.messages,
                modifier = panelModifier,
                placeholder = if (isDrawer || hasFound) "Discute avec les joueurs..." else "Écris ta proposition...",
                enabled = !isDrawer,
                onSend = GameManager::submitGuess
            )
            GameTab.TERMINAL -> TerminalPanel(
                game = game,
                messages = GameManager.messages,
                now = now,
                remainingMs = remaining,
                modifier = panelModifier
            )
        }
    }

    if (showScores) {
        ScoresDialog(game = game, myUid = myUid, onDismiss = { showScores = false })
    }

    if (isDrawer && isChoosing && !confirmLeave) {
        WordChoiceDialog(
            choices = round?.choices.orEmpty(),
            remainingMs = remaining,
            onChoose = GameManager::chooseWord
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Quitter la partie ?") },
            text = { Text("Tu pourras revenir avec le code ${game.id} tant que la partie dure.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    onLeave()
                }) { Text("Quitter") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) { Text("Rester") }
            }
        )
    }
}

// Sélecteur chat / terminal de la maquette.
@Composable
private fun TabSwitch(selected: GameTab, onSelect: (GameTab) -> Unit) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.border.copy(alpha = 0.5f))
            .padding(3.dp)
    ) {
        GameTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Text(
                text = tab.label,
                color = if (isSelected) palette.secondary.contentColor() else palette.onLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) palette.secondary else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(vertical = 7.dp)
            )
        }
    }
}

// Début de tour du dessinateur : choix parmi les mots générés.
@Composable
private fun WordChoiceDialog(choices: List<String>, remainingMs: Long, onChoose: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = {}, // choix obligatoire (sinon tirage automatique)
        title = { Text("Choisis ton mot") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tirage automatique dans ${formatCountdown(remainingMs)}",
                    color = PictHint,
                    fontSize = 13.sp
                )
                choices.forEach { choice ->
                    SettingsPrimaryButton(text = choice.uppercase(), onClick = { onChoose(choice) })
                }
            }
        },
        confirmButton = {}
    )
}

// Classement en cours (ouvert depuis le compteur de joueurs du header).
@Composable
private fun ScoresDialog(game: Game, myUid: String?, onDismiss: () -> Unit) {
    val round = game.round
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scores") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                game.players.sortedByDescending { it.score }.forEachIndexed { index, player ->
                    val tags = listOfNotNull(
                        "dessine".takeIf { player.uid == round?.drawerUid },
                        "a trouvé".takeIf { round?.found?.containsKey(player.uid) == true },
                        "hors ligne".takeIf { !player.online }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}.", fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(player.name + if (player.uid == myUid) " (toi)" else "")
                            if (tags.isNotEmpty()) {
                                Text(tags.joinToString(" • "), color = PictHint, fontSize = 12.sp)
                            }
                        }
                        Text("${player.score} pts", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

// Bandeau haut : retour, minuteur, infos du tour au centre, joueurs à droite.
@Composable
fun GameHeader(
    title: String,
    subtitle: String,
    playerCount: Int,
    onLeave: () -> Unit,
    leading: String? = null,
    leadingAlert: Boolean = false,
    onPlayersClick: () -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val onHeader = palette.background.contentColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.background)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹",
            color = onHeader,
            fontSize = 32.sp,
            modifier = Modifier
                .clickable(onClick = onLeave)
                .padding(horizontal = 10.dp)
        )
        Text(
            text = leading ?: "",
            color = if (leadingAlert) PictDanger else palette.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = palette.accent, fontSize = 11.sp)
            Text(
                text = subtitle,
                color = onHeader,
                fontSize = if (subtitle.length > 22) 14.sp else 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
        Text(
            text = "$playerCount joueurs",
            color = palette.accent,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(84.dp)
                .clickable(onClick = onPlayersClick)
                .padding(end = 8.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GameScreenPreview() {
    GameScreen(
        game = Game(
            id = "K7QM2X",
            name = "Partie de Lucie",
            hostUid = "a",
            status = GameStatus.PLAYING,
            settings = GameSettings(),
            players = listOf(Player(uid = "a", name = "Lucie.M"), Player(uid = "b", name = "Malo")),
            order = listOf("a", "b"),
            round = Round(turn = 0, drawerUid = "b", word = "arc-en-ciel"),
            totalTurns = 6
        ),
        myUid = "a"
    )
}

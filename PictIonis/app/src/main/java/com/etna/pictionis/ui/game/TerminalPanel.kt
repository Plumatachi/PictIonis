package com.etna.pictionis.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etna.pictionis.data.game.ChatMessage
import com.etna.pictionis.data.game.Game
import com.etna.pictionis.data.game.MessageKind
import com.etna.pictionis.data.game.formatCountdown
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TerminalBg = Color(0xFF14232B)
private val TerminalText = Color(0xFF80ED99)
private val TerminalCommand = Color(0xFFFFFFFF)

private data class TerminalLine(val time: Long, val text: String, val isCommand: Boolean = false)

// Onglet "terminal" de la maquette : journal des événements de la partie
// (arrivées, tours, mots trouvés) + commandes locales pour consulter l'état du jeu.
@Composable
fun TerminalPanel(
    game: Game,
    messages: List<ChatMessage>,
    now: Long,
    remainingMs: Long,
    modifier: Modifier = Modifier
) {
    var local by remember { mutableStateOf(listOf(TerminalLine(now, "tape aide pour voir les commandes"))) }
    var clearedAt by remember { mutableLongStateOf(0L) }
    val clock = remember { SimpleDateFormat("HH:mm:ss", Locale.FRANCE) }
    val listState = rememberLazyListState()

    val events = messages
        .filter { it.kind != MessageKind.CHAT && it.timestamp >= clearedAt }
        .map { TerminalLine(it.timestamp, it.text) }
    val lines = (events + local).sortedBy { it.time }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    Column(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TerminalBg),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(lines) { line ->
                Text(
                    text = if (line.isCommand) line.text else "[${clock.format(Date(line.time))}] ${line.text}",
                    color = if (line.isCommand) TerminalCommand else TerminalText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        MessageInput(placeholder = "Commande (aide)") { input ->
            if (input.trim().equals("effacer", ignoreCase = true)) {
                local = emptyList()
                clearedAt = now
            } else {
                local = local + TerminalLine(now, "$ ${input.trim()}", isCommand = true) +
                    runTerminalCommand(input, game, remainingMs).map { TerminalLine(now, it) }
            }
        }
    }
}

internal fun runTerminalCommand(input: String, game: Game, remainingMs: Long): List<String> {
    val round = game.round
    return when (input.trim().lowercase()) {
        "aide", "help" -> listOf("commandes : scores, joueurs, tour, code, effacer")
        "scores" -> game.players.sortedByDescending { it.score }
            .mapIndexed { i, p -> "${i + 1}. ${p.name} — ${p.score} pts" }
        "joueurs" -> game.players.map { p ->
            buildString {
                append(p.name)
                if (p.uid == game.hostUid) append(" [hôte]")
                if (p.uid == round?.drawerUid) append(" [dessine]")
                if (round?.found?.containsKey(p.uid) == true) append(" [a trouvé]")
                if (!p.online) append(" (hors ligne)")
            }
        }
        "tour" -> if (round == null) listOf("pas de tour en cours") else listOf(
            "tour ${round.turn + 1}/${game.totalTurns} — ${game.player(round.drawerUid)?.name ?: "?"} dessine",
            "temps restant ${formatCountdown(remainingMs)} — trouvé par ${round.found.size} joueur(s)"
        )
        "code" -> listOf("code de la partie : ${game.id}")
        else -> listOf("commande inconnue : ${input.trim()} (tape aide)")
    }
}

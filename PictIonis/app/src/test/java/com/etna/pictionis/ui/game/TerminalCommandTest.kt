package com.etna.pictionis.ui.game

import com.etna.pictionis.data.game.Game
import com.etna.pictionis.data.game.GameSettings
import com.etna.pictionis.data.game.GameStatus
import com.etna.pictionis.data.game.Player
import com.etna.pictionis.data.game.Round
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalCommandTest {

    private val game = Game(
        id = "K7QM2X",
        name = "Test",
        hostUid = "a",
        status = GameStatus.PLAYING,
        settings = GameSettings(),
        players = listOf(Player("a", "Lucie", score = 10), Player("b", "Malo", score = 90, online = false)),
        order = listOf("a", "b"),
        round = Round(turn = 1, drawerUid = "a", word = "chat", found = mapOf("b" to 90)),
        totalTurns = 6
    )

    @Test
    fun scoresAreRanked() {
        assertEquals(listOf("1. Malo — 90 pts", "2. Lucie — 10 pts"), runTerminalCommand(" Scores ", game, 0))
    }

    @Test
    fun playersShowRoles() {
        assertEquals(
            listOf("Lucie [hôte] [dessine]", "Malo [a trouvé] (hors ligne)"),
            runTerminalCommand("joueurs", game, 0)
        )
    }

    @Test
    fun unknownCommandSuggestsHelp() {
        assertTrue(runTerminalCommand("xyz", game, 0).single().contains("aide"))
    }
}

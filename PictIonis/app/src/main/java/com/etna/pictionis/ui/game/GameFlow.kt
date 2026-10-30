package com.etna.pictionis.ui.game

import androidx.compose.runtime.Composable
import com.etna.pictionis.data.game.GameManager
import com.etna.pictionis.data.game.GameStatus

// Point d'entrée choisi depuis l'accueil.
enum class GameEntry { CREATE, JOIN }

// Navigation du jeu : tant qu'on n'est pas en partie, écran de création ou
// de recherche ; ensuite l'écran suit le statut de la partie en temps réel.
@Composable
fun GameFlow(entry: GameEntry?, onExit: () -> Unit) {
    val game = GameManager.game
    val leave = {
        GameManager.leaveGame()
        onExit()
    }

    if (game == null) {
        when (entry) {
            GameEntry.CREATE -> CreateGameScreen(onBack = onExit)
            else -> JoinGameScreen(onBack = onExit)
        }
        return
    }

    when (game.status) {
        GameStatus.LOBBY -> LobbyScreen(
            game = game,
            myUid = GameManager.myUid,
            errorMessage = GameManager.errorMessage,
            onStart = { GameManager.startGame() },
            onLeave = leave
        )
        GameStatus.PLAYING -> GameScreen(
            game = game,
            myUid = GameManager.myUid,
            onLeave = leave
        )
        GameStatus.FINISHED -> ResultsScreen(
            game = game,
            myUid = GameManager.myUid,
            onLeave = leave
        )
    }
}

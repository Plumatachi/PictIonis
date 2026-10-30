package com.etna.pictionis.data.game

import com.google.firebase.database.DataSnapshot
import kotlin.random.Random

const val MIN_PLAYERS = 2
const val MAX_PLAYERS = 8
const val CODE_LENGTH = 6

// Sans I, O, 0 et 1 : un code se lit et se dicte sans ambiguïté.
private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

enum class GameStatus(val key: String) {
    LOBBY("lobby"),
    PLAYING("playing"),
    FINISHED("finished");

    companion object {
        fun of(key: String?): GameStatus = entries.firstOrNull { it.key == key } ?: LOBBY
    }
}

enum class GameMode(val key: String, val label: String, val description: String) {
    CLASSIC("classic", "Classique", "Tout le monde peut trouver, plus tu es rapide plus tu gagnes."),
    RUSH("rush", "Éclair", "Le premier qui trouve remporte le tour."),
    HINTS("hints", "Indices", "Des lettres du mot se dévoilent au fil du temps.");

    companion object {
        fun of(key: String?): GameMode = entries.firstOrNull { it.key == key } ?: CLASSIC
    }
}

enum class Difficulty(val key: String, val label: String) {
    EASY("easy", "Facile"),
    MEDIUM("medium", "Moyen"),
    HARD("hard", "Difficile"),
    MIXED("mixed", "Mélangé");

    companion object {
        fun of(key: String?): Difficulty = entries.firstOrNull { it.key == key } ?: MIXED
    }
}

data class GameSettings(
    val rounds: Int = 3,
    val roundSeconds: Int = 60,
    val isPublic: Boolean = true,
    val mode: GameMode = GameMode.CLASSIC,
    val difficulty: Difficulty = Difficulty.MIXED,
    val customWords: List<String> = emptyList(),
    val customOnly: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "rounds" to rounds,
        "roundSeconds" to roundSeconds,
        "public" to isPublic,
        "mode" to mode.key,
        "difficulty" to difficulty.key,
        "customWords" to customWords,
        "customOnly" to customOnly
    )

    companion object {
        fun from(s: DataSnapshot) = GameSettings(
            rounds = s.child("rounds").intOr(3),
            roundSeconds = s.child("roundSeconds").intOr(60),
            isPublic = s.child("public").getValue(Boolean::class.java) ?: true,
            mode = GameMode.of(s.child("mode").getValue(String::class.java)),
            difficulty = Difficulty.of(s.child("difficulty").getValue(String::class.java)),
            // Revalidés à la lecture : écrits par le terminal de l'hôte.
            customWords = parseCustomWords(
                s.child("customWords").children.mapNotNull { it.getValue(String::class.java) }.joinToString(",")
            ),
            customOnly = s.child("customOnly").getValue(Boolean::class.java) ?: false
        )
    }
}

data class Player(
    val uid: String,
    val name: String,
    val score: Long = 0,
    val online: Boolean = true,
    val joinedAt: Long = 0,
    val wordsFound: Int = 0
)

// Tour en cours : "turn" compte les tours depuis 0, chaque joueur dessine à son tour.
// Le tour commence par le choix du mot parmi "choices" (word vide), puis startedAt
// repart au choix. found : joueurs ayant trouvé le mot -> points gagnés.
// shortcut: le mot est lisible en base par tous les joueurs (masqué seulement à l'écran), passer par des Cloud Functions pour le cacher vraiment.
data class Round(
    val turn: Int,
    val drawerUid: String,
    val word: String = "",
    val startedAt: Long = 0,
    val durationMs: Long = 60_000,
    val found: Map<String, Long> = emptyMap(),
    val choices: List<String> = emptyList()
) {
    val isChoosing: Boolean get() = word.isEmpty() && choices.isNotEmpty()
    val endsAt: Long get() = startedAt + if (isChoosing) CHOICE_MS else durationMs
}

data class Game(
    val id: String,
    val name: String,
    val hostUid: String,
    val status: GameStatus,
    val settings: GameSettings,
    val players: List<Player>,
    val order: List<String>,
    val round: Round?,
    val totalTurns: Int = 0,
    val usedWords: Set<String> = emptySet()
) {
    val onlinePlayers: List<Player> get() = players.filter { it.online }
    fun player(uid: String?): Player? = players.firstOrNull { it.uid == uid }
}

fun parseGame(s: DataSnapshot): Game? {
    val id = s.key ?: return null
    val hostUid = s.child("hostUid").getValue(String::class.java) ?: return null
    val players = s.child("players").children.mapNotNull { p ->
        // Un nœud sans nom est un reste de présence (onDisconnect) : on l'ignore.
        val name = p.child("name").getValue(String::class.java) ?: return@mapNotNull null
        Player(
            uid = p.key ?: return@mapNotNull null,
            name = name,
            score = p.child("score").longOr(0),
            online = p.child("online").getValue(Boolean::class.java) ?: false,
            joinedAt = p.child("joinedAt").longOr(0),
            wordsFound = p.child("wordsFound").intOr(0)
        )
    }.sortedBy { it.joinedAt }
    val round = s.child("round").takeIf { it.exists() }?.let { r ->
        Round(
            turn = r.child("turn").intOr(0),
            drawerUid = r.child("drawerUid").getValue(String::class.java) ?: "",
            word = r.child("word").getValue(String::class.java) ?: "",
            startedAt = r.child("startedAt").longOr(0),
            durationMs = r.child("durationMs").longOr(60_000),
            found = r.child("found").children.associate { (it.key ?: "") to it.longOr(0) },
            choices = r.child("choices").children.mapNotNull { it.getValue(String::class.java) }
        )
    }
    return Game(
        id = id,
        name = s.child("name").getValue(String::class.java) ?: "Partie",
        hostUid = hostUid,
        status = GameStatus.of(s.child("status").getValue(String::class.java)),
        settings = GameSettings.from(s.child("settings")),
        players = players,
        order = s.child("order").children.mapNotNull { it.getValue(String::class.java) },
        round = round,
        totalTurns = s.child("totalTurns").intOr(0),
        usedWords = s.child("usedWords").children.mapNotNull { it.key }.toSet()
    )
}

fun generateGameCode(random: Random = Random.Default): String =
    (1..CODE_LENGTH).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")

fun normalizeGameCode(input: String): String = input.trim().uppercase()

fun isValidGameCode(code: String): Boolean =
    code.length == CODE_LENGTH && code.all { it in CODE_ALPHABET }

// RTDB renvoie des Long pour les entiers et des Double pour les décimaux.
internal fun DataSnapshot.longOr(default: Long): Long = (value as? Number)?.toLong() ?: default
internal fun DataSnapshot.intOr(default: Int): Int = (value as? Number)?.toInt() ?: default

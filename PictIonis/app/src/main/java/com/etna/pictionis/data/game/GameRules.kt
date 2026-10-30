package com.etna.pictionis.data.game

import java.text.Normalizer
import kotlin.math.roundToInt
import kotlin.random.Random

// Règles du jeu sans dépendance Firebase (testées dans GameRulesTest).

const val DRAWER_POINTS_PER_FIND = 25
private const val MIN_GUESS_POINTS = 20
private const val MAX_GUESS_POINTS = 100

// Comparaison tolérante : casse, accents, espaces et tirets ignorés
// ("Arc en ciel" trouve "arc-en-ciel"). Sert aussi de clé Firebase (a-z0-9).
fun normalizeGuess(input: String): String {
    val lower = input.trim().lowercase().replace("œ", "oe").replace("æ", "ae")
    return Normalizer.normalize(lower, Normalizer.Form.NFD).replace(Regex("[^a-z0-9]"), "")
}

fun isCorrectGuess(guess: String, word: String): Boolean {
    val target = normalizeGuess(word)
    return target.isNotEmpty() && normalizeGuess(guess) == target
}

// Le message donne-t-il la réponse ? Interdit au dessinateur et à ceux qui ont trouvé.
fun revealsWord(message: String, word: String): Boolean {
    val target = normalizeGuess(word)
    return target.isNotEmpty() && normalizeGuess(message).contains(target)
}

// "arc-en-ciel" -> "_ _ _ - _ _ - _ _ _ _" ; les lettres aux index révélés restent visibles.
fun maskWord(word: String, revealed: Set<Int> = emptySet()): String =
    word.mapIndexed { i, c ->
        when {
            c == ' ' -> " "
            !c.isLetterOrDigit() -> c.toString()
            i in revealed -> c.uppercaseChar().toString()
            else -> "_"
        }
    }.joinToString(" ")

// Mode Indices : jusqu'à la moitié des lettres se dévoilent au fil du tour.
// L'ordre dépend du mot et du tour : tous les terminaux révèlent les mêmes lettres.
fun hintIndices(word: String, turn: Int, elapsedFraction: Double): Set<Int> {
    val letters = word.indices.filter { word[it].isLetterOrDigit() }
    val count = (letters.size / 2 * elapsedFraction.coerceIn(0.0, 1.0)).toInt()
    return letters.shuffled(Random(word.hashCode() * 31 + turn)).take(count).toSet()
}

// Plus on trouve vite, plus on gagne : 100 points tout de suite, 20 à la dernière seconde.
fun guessPoints(remainingMs: Long, durationMs: Long): Int {
    if (durationMs <= 0) return MIN_GUESS_POINTS
    val ratio = (remainingMs.toDouble() / durationMs).coerceIn(0.0, 1.0)
    return MIN_GUESS_POINTS + ((MAX_GUESS_POINTS - MIN_GUESS_POINTS) * ratio).roundToInt()
}

// Rotation du dessinateur : on suit l'ordre de passage en sautant les absents.
fun drawerForTurn(order: List<String>, available: Set<String>, turn: Int): String? =
    order.indices.asSequence()
        .map { order[(turn + it) % order.size] }
        .firstOrNull { it in available }

const val WORD_CHOICES = 3
const val CHOICE_MS = 12_000L // temps laissé au dessinateur pour choisir son mot
const val MAX_CUSTOM_WORDS = 50

// Propositions de mots du tour, sans répétition dans la partie
// (on recycle les mots déjà joués s'il n'en reste plus assez).
fun generateChoices(
    pool: List<String>,
    used: Set<String>,
    count: Int = WORD_CHOICES,
    random: Random = Random.Default
): List<String> {
    val distinct = pool.distinctBy(::normalizeGuess)
    val fresh = distinct.filter { normalizeGuess(it) !in used }
    return (if (fresh.size >= count) fresh else distinct).shuffled(random).take(count)
}

// Mots personnalisés saisis par l'hôte : séparés par des virgules, 2 à 30
// caractères, au moins une lettre ou un chiffre (sinon pas de clé Firebase).
fun parseCustomWords(input: String): List<String> =
    input.split(',', ';', '\n')
        .map { it.trim().replace(Regex("\\s+"), " ") }
        .filter { it.length in 2..30 && normalizeGuess(it).isNotEmpty() }
        .distinctBy(::normalizeGuess)
        .take(MAX_CUSTOM_WORDS)

fun wordPool(settings: GameSettings): List<String> =
    if (settings.customOnly && settings.customWords.size >= WORD_CHOICES) settings.customWords
    else WordBank.forDifficulty(settings.difficulty) + settings.customWords

// 42 500 ms -> "0:43" (arrondi au-dessus, comme un compte à rebours).
fun formatCountdown(ms: Long): String {
    val seconds = ((ms + 999) / 1000).coerceAtLeast(0)
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

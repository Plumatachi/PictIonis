package com.etna.pictionis.data.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameRulesTest {

    @Test
    fun guessIgnoresCaseAccentsAndSeparators() {
        assertTrue(isCorrectGuess("  Arc en Ciel ", "arc-en-ciel"))
        assertTrue(isCorrectGuess("electricite", "électricité"))
        assertTrue(isCorrectGuess("COEUR", "cœur"))
        assertFalse(isCorrectGuess("chats", "chat"))
        assertFalse(isCorrectGuess("", ""))
    }

    @Test
    fun revealDetection() {
        assertTrue(revealsWord("c'est un CHAT noir", "chat"))
        assertFalse(revealsWord("un animal", "chat"))
    }

    @Test
    fun maskKeepsSeparatorsAndRevealedLetters() {
        assertEquals("_ _ _ - _ _ - _ _ _ _", maskWord("arc-en-ciel"))
        assertEquals("C _ _ _", maskWord("chat", setOf(0)))
        assertEquals("_ _ _ _   _ _ _ _", maskWord("trou noir"))
    }

    @Test
    fun hintsRevealUpToHalfTheLettersDeterministically() {
        val word = "arc-en-ciel" // 9 lettres
        assertEquals(emptySet<Int>(), hintIndices(word, 0, 0.0))
        val end = hintIndices(word, 0, 1.0)
        assertEquals(4, end.size)
        assertTrue(end.all { word[it].isLetter() })
        assertEquals(end, hintIndices(word, 0, 1.0)) // même résultat sur tous les terminaux
        assertTrue(hintIndices(word, 0, 0.5).all { it in end }) // les lettres s'ajoutent, ne changent pas
    }

    @Test
    fun pointsDecreaseWithTime() {
        assertEquals(100, guessPoints(60_000, 60_000))
        assertEquals(60, guessPoints(30_000, 60_000))
        assertEquals(20, guessPoints(-5, 60_000))
    }

    @Test
    fun drawerRotationSkipsAbsentPlayers() {
        val order = listOf("a", "b", "c")
        assertEquals("b", drawerForTurn(order, setOf("a", "b", "c"), 1))
        assertEquals("a", drawerForTurn(order, setOf("a", "b"), 2)) // c absent : on passe à a
        assertEquals("a", drawerForTurn(order, setOf("a", "b", "c"), 3)) // 2e manche
        assertNull(drawerForTurn(emptyList(), setOf("a"), 0))
    }

    @Test
    fun choicesAreDistinctAndAvoidUsedWords() {
        val pool = listOf("chat", "chien", "lapin", "Chat", "vache")
        val choices = generateChoices(pool, setOf("vache"), random = Random(1))
        assertEquals(3, choices.size)
        assertEquals(3, choices.map(::normalizeGuess).toSet().size)
        assertFalse("vache" in choices)
        // Plus assez de mots neufs : on recycle plutôt que de proposer moins de 3 mots.
        assertEquals(3, generateChoices(pool, setOf("chat", "chien", "lapin"), random = Random(1)).size)
    }

    @Test
    fun customWordsAreCleanedAndDeduplicated() {
        assertEquals(
            listOf("tour Eiffel", "licorne"),
            parseCustomWords(" tour   Eiffel , licorne,LICORNE, a, !!!, ")
        )
    }

    @Test
    fun customOnlyNeedsEnoughWords() {
        val few = GameSettings(customWords = listOf("a1", "b2"), customOnly = true)
        assertTrue(wordPool(few).size > 2) // pas assez de mots : liste du jeu conservée
        val enough = GameSettings(customWords = listOf("aa", "bb", "cc"), customOnly = true)
        assertEquals(listOf("aa", "bb", "cc"), wordPool(enough))
    }

    @Test
    fun wordBankKeysAreFirebaseSafeAndUnique() {
        val keys = WordBank.all.map(::normalizeGuess)
        assertTrue(keys.all { it.matches(Regex("[a-z0-9]+")) })
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun countdownFormat() {
        assertEquals("0:43", formatCountdown(42_500))
        assertEquals("1:00", formatCountdown(60_000))
        assertEquals("0:00", formatCountdown(-10))
    }
}

package com.etna.pictionis.data.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameModelsTest {

    @Test
    fun generatedCodesAreValid() {
        val random = Random(42)
        repeat(200) { assertTrue(isValidGameCode(generateGameCode(random))) }
    }

    @Test
    fun codeValidation() {
        assertEquals("K7QM2X", normalizeGameCode("  k7qm2x "))
        assertFalse(isValidGameCode("K7QM2")) // trop court
        assertFalse(isValidGameCode("K7QM2O")) // O exclu (confusion avec 0)
        assertFalse(isValidGameCode("k7qm2x")) // non normalisé
    }
}

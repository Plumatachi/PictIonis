package com.etna.pictionis.data.game

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class DrawingTest {

    @Test
    fun pointsSurviveEncoding() {
        val points = listOf(Offset(0f, 0f), Offset(0.12345f, 0.5f), Offset(1f, 0.9999f))
        val decoded = decodePoints(encodePoints(points))
        assertEquals(points.size, decoded.size)
        points.zip(decoded).forEach { (a, b) ->
            assertEquals(a.x, b.x, 0.0001f)
            assertEquals(a.y, b.y, 0.0001f)
        }
    }

    @Test
    fun malformedChunksAreIgnoredAndClamped() {
        assertEquals(listOf(Offset(1f, 0f)), decodePoints("abc;20000,-5;1,"))
        assertEquals(emptyList<Offset>(), decodePoints(""))
    }
}

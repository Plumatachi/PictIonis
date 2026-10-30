package com.etna.pictionis.data.game

import androidx.compose.ui.geometry.Offset
import com.google.firebase.database.DataSnapshot
import kotlin.math.roundToInt

// Outil du trait : main levée, ou forme géométrique définie par 2 points (début, fin).
enum class StrokeTool(val key: String) {
    PEN("pen"),
    LINE("line"),
    RECT("rect"),
    OVAL("oval");

    companion object {
        fun of(key: String?): StrokeTool = entries.firstOrNull { it.key == key } ?: PEN
    }
}

// Un trait du dessin. Coordonnées normalisées (0..1) et épaisseur relative à
// la largeur du canvas : le dessin est identique quelle que soit la taille d'écran.
data class Stroke(
    val id: String,
    val color: Long,
    val width: Float,
    val points: List<Offset>,
    val tool: StrokeTool = StrokeTool.PEN
)

// Points envoyés par paquets : "x,y;x,y" en entiers 0..10000 (compact et sans float).
private const val SCALE = 10_000f

fun encodePoints(points: List<Offset>): String =
    points.joinToString(";") { "${(it.x * SCALE).roundToInt()},${(it.y * SCALE).roundToInt()}" }

// Données écrites par les autres terminaux : on ignore ce qui est mal formé.
fun decodePoints(chunk: String): List<Offset> =
    chunk.split(';').mapNotNull { pair ->
        val parts = pair.split(',')
        val x = parts.getOrNull(0)?.toFloatOrNull() ?: return@mapNotNull null
        val y = parts.getOrNull(1)?.toFloatOrNull() ?: return@mapNotNull null
        Offset((x / SCALE).coerceIn(0f, 1f), (y / SCALE).coerceIn(0f, 1f))
    }

// drawings/{code}/{strokeId} = { c: couleur ARGB, w: épaisseur, t: outil, p: { "0": paquet, "1": paquet... } }
fun parseStroke(s: DataSnapshot): Stroke? {
    val id = s.key ?: return null
    return Stroke(
        id = id,
        color = s.child("c").longOr(0xFF000000),
        width = ((s.child("w").value as? Number)?.toFloat() ?: 0.01f).coerceIn(0.001f, 0.2f),
        // Les clés entières sont triées numériquement par Firebase : paquets dans l'ordre.
        points = s.child("p").children.flatMap { decodePoints(it.getValue(String::class.java) ?: "") },
        tool = StrokeTool.of(s.child("t").getValue(String::class.java))
    )
}

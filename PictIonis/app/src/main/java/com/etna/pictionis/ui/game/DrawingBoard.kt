package com.etna.pictionis.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.etna.pictionis.data.game.Stroke
import com.etna.pictionis.data.game.StrokeTool
import com.etna.pictionis.ui.theme.LocalAppPalette
import kotlin.math.abs
import kotlin.math.min
import androidx.compose.ui.graphics.drawscope.Stroke as LineStyle

// Couleurs indépendantes du thème : le dessin doit être identique sur tous les terminaux.
data class PenColor(val name: String, val argb: Long)

val PenColors = listOf(
    PenColor("Noir", 0xFF1A1A1A),
    PenColor("Gris", 0xFF8D8D8D),
    PenColor("Marron", 0xFF8B5A2B),
    PenColor("Rouge", 0xFFE63946),
    PenColor("Orange", 0xFFF4A261),
    PenColor("Jaune", 0xFFF9C74F),
    PenColor("Peau", 0xFFF1C27D),
    PenColor("Vert clair", 0xFF90BE6D),
    PenColor("Vert", 0xFF2BA84A),
    PenColor("Turquoise", 0xFF2A9D8F),
    PenColor("Bleu ciel", 0xFF4CC9F0),
    PenColor("Bleu", 0xFF1D70B8),
    PenColor("Bleu nuit", 0xFF1D3557),
    PenColor("Violet", 0xFF7B2CBF),
    PenColor("Rose", 0xFFF72585),
    PenColor("Bordeaux", 0xFF7F1D1D)
)

// Épaisseurs relatives à la largeur du canvas, de fin à très épais.
private val PenWidths = listOf(0.006f, 0.012f, 0.024f, 0.045f)

private const val ERASER_ARGB = 0xFFFFFFFF // fond du canvas
private const val ERASER_MIN_WIDTH = 0.03f
private const val FLUSH_MS = 50L // cadence d'envoi des points pendant le tracé
private const val MIN_STEP = 0.002f // ignore les micro-mouvements
private const val CANVAS_RATIO = 4f / 3f // même ratio partout pour ne pas déformer

// Zone de dessin + outils. Seul le dessinateur voit les outils et peut tracer.
@Composable
fun DrawingBoard(
    strokes: List<Stroke>,
    canDraw: Boolean,
    modifier: Modifier = Modifier,
    onBegin: (color: Long, width: Float, tool: StrokeTool, points: List<Offset>) -> String? = { _, _, _, _ -> null },
    onAppend: (strokeId: String, chunkIndex: Int, points: List<Offset>) -> Unit = { _, _, _ -> },
    onClear: () -> Unit = {}
) {
    val palette = LocalAppPalette.current
    var color by remember { mutableStateOf(PenColors.first()) }
    var tool by remember { mutableStateOf(StrokeTool.PEN) }
    var eraser by remember { mutableStateOf(false) }
    var widthIndex by remember { mutableStateOf(1) }
    val shape = RoundedCornerShape(12.dp)
    val width = PenWidths[widthIndex]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, palette.border, shape)
    ) {
        if (canDraw) {
            // Outils + épaisseurs + effacer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StrokeTool.entries.forEach { option ->
                        ToolButton(
                            label = toolLabel(option),
                            selected = !eraser && tool == option,
                            onClick = {
                                tool = option
                                eraser = false
                            }
                        ) { drawToolGlyph(option, it) }
                    }
                    ToolButton(label = "Gomme", selected = eraser, onClick = { eraser = !eraser }) {
                        drawEraserGlyph(it)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    PenWidths.forEachIndexed { index, _ ->
                        WidthDot(
                            index = index,
                            selected = index == widthIndex,
                            onClick = { widthIndex = index }
                        )
                    }
                }
                ToolButton(label = "Effacer le dessin", onClick = onClear) { drawTrashGlyph(it) }
            }
            // Palette étendue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PenColors.forEach { pen ->
                    ColorDot(
                        pen = pen,
                        selected = !eraser && pen == color,
                        onClick = {
                            color = pen
                            eraser = false
                        }
                    )
                }
            }
        }
        DrawingCanvas(
            strokes = strokes,
            canDraw = canDraw,
            color = if (eraser) ERASER_ARGB else color.argb,
            width = if (eraser) maxOf(width, ERASER_MIN_WIDTH) else width,
            tool = if (eraser) StrokeTool.PEN else tool,
            onBegin = onBegin,
            onAppend = onAppend
        )
    }
}

private fun toolLabel(tool: StrokeTool) = when (tool) {
    StrokeTool.PEN -> "Crayon"
    StrokeTool.LINE -> "Ligne"
    StrokeTool.RECT -> "Rectangle"
    StrokeTool.OVAL -> "Ovale"
}

@Composable
private fun DrawingCanvas(
    strokes: List<Stroke>,
    canDraw: Boolean,
    color: Long,
    width: Float,
    tool: StrokeTool,
    onBegin: (Long, Float, StrokeTool, List<Offset>) -> String?,
    onAppend: (String, Int, List<Offset>) -> Unit
) {
    val currentColor by rememberUpdatedState(color)
    val currentWidth by rememberUpdatedState(width)
    val currentTool by rememberUpdatedState(tool)
    val begin by rememberUpdatedState(onBegin)
    val append by rememberUpdatedState(onAppend)
    // Tracé local pas encore envoyé (crayon) ou aperçu de la forme en cours.
    var pending by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(CANVAS_RATIO)
            .clipToBounds()
            .pointerInput(canDraw) {
                if (!canDraw) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val first = normalize(down.position, size)
                    if (currentTool != StrokeTool.PEN) {
                        // Forme : aperçu local pendant le geste, envoyée en une fois au relâché.
                        var end = first
                        pending = listOf(first, first)
                        drag(down.id) { change ->
                            change.consume()
                            end = normalize(change.position, size)
                            pending = listOf(first, end)
                        }
                        begin(currentColor, currentWidth, currentTool, listOf(first, end))
                        pending = emptyList()
                        return@awaitEachGesture
                    }
                    val strokeId = begin(currentColor, currentWidth, StrokeTool.PEN, listOf(first))
                        ?: return@awaitEachGesture
                    var chunkIndex = 1
                    var buffer = mutableListOf<Offset>()
                    var last = first
                    var lastFlush = System.currentTimeMillis()
                    pending = listOf(first)
                    drag(down.id) { change ->
                        change.consume()
                        val point = normalize(change.position, size)
                        if ((point - last).getDistance() < MIN_STEP) return@drag
                        last = point
                        buffer += point
                        pending = pending + point
                        val now = System.currentTimeMillis()
                        if (now - lastFlush >= FLUSH_MS) {
                            append(strokeId, chunkIndex++, buffer)
                            buffer = mutableListOf()
                            lastFlush = now
                            pending = listOf(point) // la suite repart du dernier point envoyé
                        }
                    }
                    append(strokeId, chunkIndex, buffer)
                    pending = emptyList()
                }
            }
    ) {
        strokes.forEach { drawStroke(it.color, it.width, it.tool, it.points) }
        if (pending.size > 1) drawStroke(currentColor, currentWidth, currentTool, pending)
    }
}

private fun normalize(position: Offset, size: IntSize) = Offset(
    (position.x / size.width).coerceIn(0f, 1f),
    (position.y / size.height).coerceIn(0f, 1f)
)

private fun DrawScope.drawStroke(argb: Long, width: Float, tool: StrokeTool, points: List<Offset>) {
    if (points.isEmpty()) return
    val color = Color(argb)
    val px = width * size.width
    val scaled = points.map { Offset(it.x * size.width, it.y * size.height) }
    val start = scaled.first()
    val end = scaled.last()
    val outline = LineStyle(width = px, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val topLeft = Offset(min(start.x, end.x), min(start.y, end.y))
    val box = Size(abs(end.x - start.x), abs(end.y - start.y))
    when (tool) {
        StrokeTool.LINE -> drawLine(color, start, end, px, StrokeCap.Round)
        StrokeTool.RECT -> drawRect(color, topLeft, box, style = outline)
        StrokeTool.OVAL -> drawOval(color, topLeft, box, style = outline)
        StrokeTool.PEN -> {
            if (scaled.size == 1) {
                drawCircle(color = color, radius = px / 2f, center = start)
                return
            }
            val path = Path().apply {
                moveTo(start.x, start.y)
                for (i in 1 until scaled.size) lineTo(scaled[i].x, scaled[i].y)
            }
            drawPath(path, color, style = outline)
        }
    }
}

@Composable
private fun ColorDot(pen: PenColor, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = pen.name }
            .border(2.dp, if (selected) palette.secondary else Color.Transparent, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(Color(pen.argb))
            .border(1.dp, Color(0xFFD3D1C7), CircleShape)
    )
}

// Épaisseur du trait : pastille dont la taille montre l'épaisseur.
@Composable
private fun WidthDot(index: Int, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) palette.lightBg else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "Épaisseur ${index + 1}" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((5 + index * 4).dp)
                .clip(CircleShape)
                .background(if (selected) palette.secondary else Color(0xFF5F5E5A))
        )
    }
}

@Composable
fun ToolButton(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    glyph: DrawScope.(Color) -> Unit
) {
    val palette = LocalAppPalette.current
    val tint = if (selected) palette.secondary else Color(0xFF5F5E5A)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) palette.lightBg else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) { glyph(tint) }
    }
}

// Icônes dessinées en Canvas, comme le reste de l'app (pas de dépendance d'icônes).
private fun DrawScope.drawToolGlyph(tool: StrokeTool, color: Color) {
    val w = size.width
    val h = size.height
    val line = w * 0.09f
    val outline = LineStyle(width = line)
    when (tool) {
        StrokeTool.PEN -> {
            drawLine(color, Offset(w * 0.78f, h * 0.15f), Offset(w * 0.28f, h * 0.65f), w * 0.16f, StrokeCap.Round)
            drawLine(color, Offset(w * 0.28f, h * 0.65f), Offset(w * 0.15f, h * 0.85f), line, StrokeCap.Round)
        }
        StrokeTool.LINE -> drawLine(color, Offset(w * 0.15f, h * 0.85f), Offset(w * 0.85f, h * 0.15f), line, StrokeCap.Round)
        StrokeTool.RECT -> drawRect(color, Offset(w * 0.12f, h * 0.22f), Size(w * 0.76f, h * 0.56f), style = outline)
        StrokeTool.OVAL -> drawOval(color, Offset(w * 0.10f, h * 0.20f), Size(w * 0.80f, h * 0.60f), style = outline)
    }
}

private fun DrawScope.drawTrashGlyph(color: Color) {
    val w = size.width
    val h = size.height
    val line = w * 0.09f
    drawLine(color, Offset(w * 0.15f, h * 0.22f), Offset(w * 0.85f, h * 0.22f), line, StrokeCap.Round)
    drawLine(color, Offset(w * 0.40f, h * 0.10f), Offset(w * 0.60f, h * 0.10f), line, StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.25f, h * 0.32f),
        size = Size(w * 0.50f, h * 0.58f),
        style = LineStyle(width = line)
    )
    drawLine(color, Offset(w * 0.42f, h * 0.45f), Offset(w * 0.42f, h * 0.78f), line)
    drawLine(color, Offset(w * 0.58f, h * 0.45f), Offset(w * 0.58f, h * 0.78f), line)
}

private fun DrawScope.drawEraserGlyph(color: Color) {
    val w = size.width
    val h = size.height
    val body = Path().apply {
        moveTo(w * 0.55f, h * 0.12f)
        lineTo(w * 0.92f, h * 0.48f)
        lineTo(w * 0.52f, h * 0.88f)
        lineTo(w * 0.15f, h * 0.52f)
        close()
    }
    drawPath(body, color, style = LineStyle(width = w * 0.09f, join = StrokeJoin.Round))
    drawLine(color, Offset(w * 0.33f, h * 0.34f), Offset(w * 0.70f, h * 0.70f), w * 0.09f)
    drawLine(color, Offset(w * 0.52f, h * 0.92f), Offset(w * 0.92f, h * 0.92f), w * 0.09f, StrokeCap.Round)
}

@Preview(showBackground = true)
@Composable
fun DrawingBoardPreview() {
    DrawingBoard(
        strokes = listOf(
            Stroke("a", 0xFF1D70B8, 0.012f, listOf(Offset(0.1f, 0.2f), Offset(0.5f, 0.6f), Offset(0.8f, 0.3f))),
            Stroke("b", 0xFFE63946, 0.024f, listOf(Offset(0.55f, 0.55f), Offset(0.9f, 0.9f)), StrokeTool.OVAL)
        ),
        canDraw = true
    )
}

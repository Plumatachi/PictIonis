package com.etna.pictionis.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etna.pictionis.data.game.ChatMessage
import com.etna.pictionis.data.game.MAX_MESSAGE_LENGTH
import com.etna.pictionis.data.game.MessageKind
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.PictHint
import com.etna.pictionis.ui.theme.contentColor

// Couleurs des pseudos (lisibles sur fond blanc), une par joueur.
private val NameColors = listOf(
    Color(0xFF38A3A5), Color(0xFF22577A), Color(0xFF2E7D32),
    Color(0xFFC0392B), Color(0xFF8E44AD), Color(0xFFD35400)
)

fun nameColor(uid: String): Color = NameColors[Math.floorMod(uid.hashCode(), NameColors.size)]

// Zone de chat d'après maquette : messages en temps réel + champ de saisie.
@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier,
    placeholder: String = "Écris ta proposition...",
    enabled: Boolean = true,
    onSend: (String) -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val listState = rememberLazyListState()
    val shape = RoundedCornerShape(12.dp)

    // Toujours afficher le dernier message reçu.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(shape)
                .background(Color.White)
                .border(1.dp, palette.border, shape),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { MessageLine(it) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        MessageInput(placeholder = placeholder, enabled = enabled, onSend = onSend)
    }
}

// Champ de saisie + bouton envoyer (chat et terminal).
@Composable
fun MessageInput(
    placeholder: String,
    enabled: Boolean = true,
    onSend: (String) -> Unit
) {
    val palette = LocalAppPalette.current
    var text by remember { mutableStateOf("") }
    val send = {
        if (text.isNotBlank()) {
            onSend(text)
            text = ""
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) text = it },
            placeholder = { Text(placeholder, color = PictHint, fontSize = 13.sp) },
            enabled = enabled,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedBorderColor = palette.secondary,
                unfocusedBorderColor = palette.border,
                focusedTextColor = Color(0xFF1A1A1A),
                unfocusedTextColor = Color(0xFF1A1A1A),
                cursorColor = palette.secondary
            ),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        SendButton(enabled = enabled && text.isNotBlank(), onClick = send)
    }
}

@Composable
private fun MessageLine(message: ChatMessage) {
    val palette = LocalAppPalette.current
    when (message.kind) {
        MessageKind.CHAT -> Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = nameColor(message.uid), fontWeight = FontWeight.SemiBold)) {
                    append(message.name)
                }
                append(" ")
                withStyle(SpanStyle(color = Color(0xFF2C2C2A))) { append(message.text) }
            },
            fontSize = 13.sp
        )
        MessageKind.SYSTEM -> Text(
            text = message.text,
            color = PictHint,
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic
        )
        MessageKind.FOUND -> Text(
            text = message.text,
            color = palette.onLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(palette.border)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    val bg = if (enabled) palette.primary else palette.border
    val tint = bg.contentColor()
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = "Envoyer" },
        contentAlignment = Alignment.Center
    ) {
        // Avion en papier
        Canvas(modifier = Modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val plane = Path().apply {
                moveTo(w * 0.08f, h * 0.45f)
                lineTo(w * 0.92f, h * 0.10f)
                lineTo(w * 0.62f, h * 0.92f)
                lineTo(w * 0.45f, h * 0.58f)
                close()
            }
            drawPath(plane, tint, style = Stroke(width = w * 0.09f, join = StrokeJoin.Round))
            drawLine(tint, Offset(w * 0.45f, h * 0.58f), Offset(w * 0.92f, h * 0.10f), w * 0.09f)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatPanelPreview() {
    ChatPanel(
        messages = listOf(
            ChatMessage("1", "n", "Nora", "Nora a rejoint la partie", MessageKind.SYSTEM, 0),
            ChatMessage("2", "m", "Malo", "c'est un animal ?", MessageKind.CHAT, 0),
            ChatMessage("3", "i", "Iris", "un chat ?", MessageKind.CHAT, 0),
            ChatMessage("4", "i", "Iris", "Iris a trouvé le mot !", MessageKind.FOUND, 0)
        ),
        modifier = Modifier.height(320.dp)
    )
}

package com.etna.pictionis.data.game

import com.google.firebase.database.DataSnapshot

const val MAX_MESSAGE_LENGTH = 120

enum class MessageKind(val key: String) {
    CHAT("chat"),
    SYSTEM("system"), // arrivées, départs, début de tour...
    FOUND("found"); // un joueur a trouvé le mot

    companion object {
        fun of(key: String?): MessageKind = entries.firstOrNull { it.key == key } ?: CHAT
    }
}

data class ChatMessage(
    val id: String,
    val uid: String,
    val name: String,
    val text: String,
    val kind: MessageKind,
    val timestamp: Long
)

// chats/{code}/{messageId} = { uid, name, text, kind, ts }
fun parseMessage(s: DataSnapshot): ChatMessage? {
    val text = s.child("text").getValue(String::class.java) ?: return null
    return ChatMessage(
        id = s.key ?: return null,
        uid = s.child("uid").getValue(String::class.java) ?: "",
        name = s.child("name").getValue(String::class.java) ?: "?",
        text = text.take(MAX_MESSAGE_LENGTH),
        kind = MessageKind.of(s.child("kind").getValue(String::class.java)),
        timestamp = s.child("ts").longOr(0)
    )
}

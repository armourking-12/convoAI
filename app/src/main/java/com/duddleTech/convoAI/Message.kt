// File: Message.kt
package com.duddleTech.convoAI

data class Message(
    var message: String,
    var sentBy: String,
    var imageUrl: String? = null
) {
    companion object {
        const val SENT_BY_ME = "me"
        const val SENT_BY_BOT = "bot"
        // Or if you use these:
        const val SENT_BY_USER = "me"
    }
}
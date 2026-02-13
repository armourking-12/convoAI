package com.duddleTech.convoAI

import com.google.gson.annotations.SerializedName

// --- REQUEST MODELS (Sending to AI) ---

data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>
)

data class GroqMessage(
    val role: String,
    // ✅ 'Any' allows this to hold TEXT (String) or IMAGES (List)
    @SerializedName("content")
    val content: Any
)

// --- RESPONSE MODELS (Getting from AI) ---

data class GroqResponse(
    val choices: List<GroqChoice>
)

data class GroqChoice(
    val message: GroqMessage // Reuses GroqMessage to capture the reply
)

// --- HELPERS ---

// Helper: Creates simple text message
fun createTextContent(text: String): String = text

// Helper: Creates Image + Text message (Vision)
fun createImageContent(text: String, base64Image: String): List<Map<String, Any>> {
    return listOf(
        mapOf("type" to "text", "text" to text),
        mapOf(
            "type" to "image_url",
            "image_url" to mapOf(
                "url" to "data:image/jpeg;base64,$base64Image"
            )
        )
    )
}
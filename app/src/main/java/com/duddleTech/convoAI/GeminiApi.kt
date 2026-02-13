package com.duddleTech.convoAI

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GeminiApi {
    // We use "v1/chat/completions" because it is the Universal Standard
    // supported by Groq, OpenAI, OpenRouter, and DeepSeek.
    @POST("v1/chat/completions")
    fun getResponse(
        @Header("Authorization") token: String,
        @Body request: GroqRequest
    ): Call<GroqResponse>
}
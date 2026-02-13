package com.duddleTech.convoAI

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 1. Setup Back Button
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Closes this screen and goes back to Chat
        }

        // 2. Setup "Clear History" Button (Placeholder logic)
        // You can expand this later!
    }
}
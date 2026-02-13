package com.duddleTech.convoAI

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Wait for 2 seconds (2000ms), then check login status
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, 2000)
    }

    private fun checkUserStatus() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // User is already logged in -> Go to Main Chat
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // User is NOT logged in -> Go to Login Screen
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish() // Close Splash so back button doesn't return here
    }
}
package com.duddleTech.convoAI

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // 1. Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // 2. Connect to UI
        val nameInput = findViewById<TextInputEditText>(R.id.nameInput)
        val emailInput = findViewById<TextInputEditText>(R.id.emailInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.passwordInput)
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        val loginLink = findViewById<TextView>(R.id.loginLink)

        // 3. Handle Sign Up Click
        signUpButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Validation
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create User in Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Success!
                        Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show()

                        // Go to Main App and clear history so back button doesn't return here
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        // Failure
                        Toast.makeText(this, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // 4. Handle Login Link (Go back)
        loginLink.setOnClickListener {
            finish() // Just closes this screen to reveal LoginActivity behind it
        }
    }
}
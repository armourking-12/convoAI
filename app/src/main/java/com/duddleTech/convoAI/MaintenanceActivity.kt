package com.duddleTech.convoAI

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class MaintenanceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maintenance)

        val btnRetry = findViewById<MaterialButton>(R.id.btnRetry)

        btnRetry.setOnClickListener {
            checkMaintenanceStatus()
        }
    }

    // 🚫 Block the Back Button so they can't bypass the screen
    override fun onBackPressed() {
        // Do nothing (User stays stuck here until maintenance is over)
    }

    private fun checkMaintenanceStatus() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0) // Fetch instantly for testing
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val isMaintenance = remoteConfig.getBoolean("is_maintenance_active")

                if (!isMaintenance) {
                    // ✅ Maintenance Over! Go back to App
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // ❌ Still Maintenance
                    Toast.makeText(this, "Still updating... please wait.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
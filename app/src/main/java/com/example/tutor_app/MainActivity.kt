package com.example.tutor_app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var welcomeText: TextView
    private lateinit var userTypeText: TextView
    private lateinit var logoutButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        welcomeText = findViewById(R.id.welcomeText)
        userTypeText = findViewById(R.id.userTypeText)
        logoutButton = findViewById(R.id.logoutButton)

        // Get user type from intent
        val userType = intent.getStringExtra("USER_TYPE") ?: "unknown"
        val userId = intent.getStringExtra("USER_ID") ?: ""

        // Display user info
        val currentUser = auth.currentUser
        welcomeText.text = "Welcome, ${currentUser?.email ?: "User"}!"
        userTypeText.text = "Logged in as: ${userType.capitalize()}"

        // Logout button
        logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Navigate back to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
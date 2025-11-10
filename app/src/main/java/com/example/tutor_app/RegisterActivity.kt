package com.example.tutor_app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var registerButton: MaterialButton
    private lateinit var loginText: TextView
    private lateinit var cardStudent: MaterialCardView
    private lateinit var cardTutor: MaterialCardView

    private var isStudentSelected = true  // Track selection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        registerButton = findViewById(R.id.registerButton)
        loginText = findViewById(R.id.loginText)
        cardStudent = findViewById(R.id.cardStudent)
        cardTutor = findViewById(R.id.cardTutor)

        // Set up card click listeners
        setupUserTypeSelection()

        // Register button click
        registerButton.setOnClickListener {
            registerUser()
        }

        // Navigate to Login
        loginText.setOnClickListener {
            finish() // Go back to login
        }
    }

    private fun setupUserTypeSelection() {
        cardStudent.setOnClickListener {
            isStudentSelected = true
            cardStudent.strokeWidth = 3
            cardTutor.strokeWidth = 0
        }

        cardTutor.setOnClickListener {
            isStudentSelected = false
            cardTutor.strokeWidth = 3
            cardStudent.strokeWidth = 0
        }

        // Set initial selection
        cardStudent.strokeWidth = 3
        cardTutor.strokeWidth = 0
    }

    private fun registerUser() {
        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()
        val userType = if (isStudentSelected) "student" else "tutor"

        // Validation
        if (name.isEmpty()) {
            nameEditText.error = "Name is required"
            nameEditText.requestFocus()
            return
        }

        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            emailEditText.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            passwordEditText.requestFocus()
            return
        }

        if (password.length < 6) {
            passwordEditText.error = "Password must be at least 6 characters"
            passwordEditText.requestFocus()
            return
        }

        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Passwords do not match"
            confirmPasswordEditText.requestFocus()
            return
        }

        // Show loading
        registerButton.isEnabled = false
        registerButton.text = "Creating Account..."

        // Firebase Authentication - Create User
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid

                    // Save user data to Firestore
                    val user = User(
                        userId = userId ?: "",
                        name = name,
                        email = email,
                        userType = userType
                    )

                    if (userId != null) {
                        firestore.collection("users").document(userId)
                            .set(user)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()

                                // Navigate to MainActivity
                                val intent = Intent(this, MainActivity::class.java)
                                intent.putExtra("USER_TYPE", userType)
                                intent.putExtra("USER_ID", userId)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                registerButton.isEnabled = true
                                registerButton.text = "Create Account"
                            }
                    }
                } else {
                    Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    registerButton.isEnabled = true
                    registerButton.text = "Create Account"
                }
            }
    }
}
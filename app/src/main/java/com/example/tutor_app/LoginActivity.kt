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

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var registerText: TextView
    private lateinit var cardStudent: MaterialCardView
    private lateinit var cardTutor: MaterialCardView

    private var isStudentSelected = true  // Track selection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerText = findViewById(R.id.registerText)
        cardStudent = findViewById(R.id.cardStudent)
        cardTutor = findViewById(R.id.cardTutor)

        // Set up card click listeners for user type selection
        setupUserTypeSelection()

        // Login button click
        loginButton.setOnClickListener {
            loginUser()
        }

        // Navigate to Register
        registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupUserTypeSelection() {
        // Card click listeners
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

        // Set initial selection (Student selected by default)
        cardStudent.strokeWidth = 3
        cardTutor.strokeWidth = 0
    }

    private fun loginUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val userType = if (isStudentSelected) "student" else "tutor"

        // Validation
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

        // Show loading
        loginButton.isEnabled = false
        loginButton.text = "Logging in..."

        // Firebase Authentication
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Get user data from Firestore to verify user type
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        firestore.collection("users").document(userId)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val savedUserType = document.getString("userType")

                                    // Check if user type matches
                                    if (savedUserType == userType) {
                                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                                        // Navigate to MainActivity
                                        val intent = Intent(this, MainActivity::class.java)
                                        intent.putExtra("USER_TYPE", userType)
                                        intent.putExtra("USER_ID", userId)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Please select correct user type (You registered as $savedUserType)",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        auth.signOut()
                                        loginButton.isEnabled = true
                                        loginButton.text = "Login"
                                    }
                                } else {
                                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                                    loginButton.isEnabled = true
                                    loginButton.text = "Login"
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                loginButton.isEnabled = true
                                loginButton.text = "Login"
                            }
                    }
                } else {
                    Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    loginButton.isEnabled = true
                    loginButton.text = "Login"
                }
            }
    }
}
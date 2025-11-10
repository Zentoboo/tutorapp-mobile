package com.example.tutor_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val splashTimeOut: Long = 3000 // 3 seconds
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Apply animation to the main content
        val contentLayout = findViewById<LinearLayout>(R.id.splashContent)
        val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        contentLayout.startAnimation(slideUpAnimation)

        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user is already logged in
            val currentUser = auth.currentUser
            val intent = if (currentUser != null) {
                // User is logged in, go to MainActivity
                Intent(this, MainActivity::class.java)
            } else {
                // User not logged in, go to LoginActivity
                Intent(this, LoginActivity::class.java)
            }

            startActivity(intent)
            overridePendingTransition(R.anim.slide_up_activity, R.anim.slide_out_up)
            finish()
        }, splashTimeOut)
    }
}
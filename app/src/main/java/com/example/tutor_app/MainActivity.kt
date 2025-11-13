package com.example.tutor_app

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var toolbar: MaterialToolbar

    private var userType: String = "student"
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        bottomNav = findViewById(R.id.bottom_navigation)

        // Set up toolbar
        setSupportActionBar(toolbar)

        // Get user data from intent or Firebase
        userType = intent.getStringExtra("USER_TYPE") ?: "student"
        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid ?: ""

        // If no user type in intent, fetch from Firestore
        if (intent.getStringExtra("USER_TYPE") == null) {
            fetchUserType()
        } else {
            setupNavigation()
        }
    }

    private fun fetchUserType() {
        if (userId.isNotEmpty()) {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    userType = document.getString("userType") ?: "student"
                    setupNavigation()
                }
                .addOnFailureListener {
                    userType = "student"
                    setupNavigation()
                }
        } else {
            setupNavigation()
        }
    }

    private fun setupNavigation() {
        // Set appropriate menu based on user type
        if (userType == "tutor") {
            bottomNav.inflateMenu(R.menu.bottom_nav_menu_tutor)
            toolbar.title = "Tutor Dashboard"
            loadFragment(DashboardFragment())
        } else {
            bottomNav.inflateMenu(R.menu.bottom_nav_menu_student)
            toolbar.title = "TutorHub"
            loadFragment(HomeFragment())
        }

        // Handle bottom navigation clicks
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    if (userType == "tutor") {
                        toolbar.title = "Dashboard"
                        loadFragment(DashboardFragment())
                    } else {
                        toolbar.title = "Find Tutors"
                        loadFragment(HomeFragment())
                    }
                    true
                }
                R.id.navigation_bookings -> {
                    toolbar.title = "My Bookings"
                    loadFragment(ScheduleFragment())
                    true
                }
                R.id.navigation_schedule -> {
                    toolbar.title = "My Schedule"
                    loadFragment(ScheduleFragment())
                    true
                }
                R.id.navigation_messages -> {
                    toolbar.title = "Messages"
                    loadFragment(MessagesFragment())
                    true
                }
                R.id.navigation_profile -> {
                    toolbar.title = "Profile"
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    // Create options menu in toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            R.id.action_settings -> {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
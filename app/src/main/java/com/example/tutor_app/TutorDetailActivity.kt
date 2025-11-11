package com.example.tutor_app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TutorDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var tutorProfileImage: ImageView
    private lateinit var tutorProfileInitials: TextView
    private lateinit var tutorNameText: TextView
    private lateinit var tutorRatingText: TextView
    private lateinit var tutorReviewsText: TextView
    private lateinit var tutorHourlyRateText: TextView
    private lateinit var subjectsChipGroup: ChipGroup
    private lateinit var educationLevelsChipGroup: ChipGroup
    private lateinit var tutorBioText: TextView
    private lateinit var callButton: MaterialButton
    private lateinit var messageButton: MaterialButton
    private lateinit var bookSessionButton: MaterialButton
    private lateinit var backButton: ImageButton

    private var tutorId: String = ""
    private var tutorPhone: String = ""
    private var tutorEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutor_detail)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        tutorProfileImage = findViewById(R.id.tutorProfileImage)
        tutorProfileInitials = findViewById(R.id.tutorProfileInitials)
        tutorNameText = findViewById(R.id.tutorNameText)
        tutorRatingText = findViewById(R.id.tutorRatingText)
        tutorReviewsText = findViewById(R.id.tutorReviewsText)
        tutorHourlyRateText = findViewById(R.id.tutorHourlyRateText)
        subjectsChipGroup = findViewById(R.id.subjectsChipGroup)
        educationLevelsChipGroup = findViewById(R.id.educationLevelsChipGroup)
        tutorBioText = findViewById(R.id.tutorBioText)
        callButton = findViewById(R.id.callButton)
        messageButton = findViewById(R.id.messageButton)
        bookSessionButton = findViewById(R.id.bookSessionButton)
        backButton = findViewById(R.id.backButton)

        // Get tutor ID from intent
        tutorId = intent.getStringExtra("TUTOR_ID") ?: ""

        if (tutorId.isNotEmpty()) {
            loadTutorDetails(tutorId)
        } else {
            Toast.makeText(this, "Error loading tutor details", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Back button
        backButton.setOnClickListener {
            finish()
        }

        // Call button - Implicit Intent
        callButton.setOnClickListener {
            if (tutorPhone.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$tutorPhone")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Message button - Open chat
        messageButton.setOnClickListener {
            openChat()
        }

        // Book Session button
        bookSessionButton.setOnClickListener {
            Toast.makeText(this, "Booking feature coming soon!", Toast.LENGTH_SHORT).show()
            // TODO: Implement booking functionality later
        }
    }

    private fun loadTutorDetails(tutorId: String) {
        firestore.collection("users").document(tutorId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val tutor = Tutor(
                        userId = document.id,
                        name = document.getString("name") ?: "",
                        email = document.getString("email") ?: "",
                        phoneNumber = document.getString("phoneNumber") ?: "",
                        profileImageUrl = document.getString("profileImageUrl") ?: "",
                        subjectsToTeach = document.get("subjectsToTeach") as? List<String> ?: emptyList(),
                        educationLevelsToTeach = document.get("educationLevelsToTeach") as? List<String> ?: emptyList(),
                        hourlyRate = document.getString("hourlyRate") ?: "0",
                        bio = document.getString("bio") ?: "",
                        rating = document.getDouble("rating") ?: 0.0,
                        totalReviews = document.getLong("totalReviews")?.toInt() ?: 0
                    )

                    displayTutorDetails(tutor)
                } else {
                    Toast.makeText(this, "Tutor not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayTutorDetails(tutor: Tutor) {
        tutorPhone = tutor.phoneNumber
        tutorEmail = tutor.email

        // Display name
        tutorNameText.text = tutor.name

        // Display profile picture or initials
        if (tutor.profileImageUrl.isNotEmpty()) {
            tutorProfileImage.visibility = View.VISIBLE
            tutorProfileInitials.visibility = View.GONE
            Glide.with(this)
                .load(tutor.profileImageUrl)
                .centerCrop()
                .into(tutorProfileImage)
        } else {
            tutorProfileImage.visibility = View.GONE
            tutorProfileInitials.visibility = View.VISIBLE
            tutorProfileInitials.text = tutor.getInitials()
        }

        // Display rating
        tutorRatingText.text = if (tutor.rating > 0) "%.1f".format(tutor.rating) else "New"
        tutorReviewsText.text = if (tutor.totalReviews > 0) "(${tutor.totalReviews} reviews)" else "(No reviews yet)"

        // Display hourly rate
        tutorHourlyRateText.text = "RM ${tutor.hourlyRate}"

        // Display subjects
        tutor.subjectsToTeach.forEach { subject ->
            val chip = Chip(this)
            chip.text = subject
            chip.isClickable = false
            subjectsChipGroup.addView(chip)
        }

        // Display education levels
        tutor.educationLevelsToTeach.forEach { level ->
            val chip = Chip(this)
            chip.text = level
            chip.isClickable = false
            educationLevelsChipGroup.addView(chip)
        }

        // Display bio
        tutorBioText.text = tutor.bio
    }

    private fun openChat() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "Please login to send messages", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if chat already exists
        firestore.collection("chats")
            .whereArrayContains("participantIds", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                var existingChatId: String? = null

                for (doc in documents) {
                    val participants = doc.get("participantIds") as? List<String>
                    if (participants?.contains(tutorId) == true) {
                        existingChatId = doc.id
                        break
                    }
                }

                val intent = Intent(this, ChatActivity::class.java)
                if (existingChatId != null) {
                    intent.putExtra("CHAT_ID", existingChatId)
                }
                intent.putExtra("OTHER_USER_ID", tutorId)
                intent.putExtra("OTHER_USER_NAME", tutorNameText.text.toString())
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
package com.example.tutor_app

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class CreateBookingActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Views
    private lateinit var backButton: ImageButton
    private lateinit var tutorProfileImage: ImageView
    private lateinit var tutorInitials: TextView
    private lateinit var tutorNameText: TextView
    private lateinit var tutorRateText: TextView
    private lateinit var subjectAutoComplete: AutoCompleteTextView
    private lateinit var educationLevelAutoComplete: AutoCompleteTextView
    private lateinit var daysChipGroup: ChipGroup
    private lateinit var preferredTimeEdit: TextInputEditText
    private lateinit var sessionsPerWeekEdit: TextInputEditText
    private lateinit var hoursPerSessionEdit: TextInputEditText
    private lateinit var totalHoursText: TextView
    private lateinit var estimatedPriceText: TextView
    private lateinit var studentNotesEdit: TextInputEditText
    private lateinit var submitBookingButton: MaterialButton

    // Data
    private var tutorId: String = ""
    private var tutorName: String = ""
    private var hourlyRate: Double = 0.0
    private var currentUserId: String = ""
    private var currentUserName: String = ""

    // Day chips mapping
    private val dayChipMap = mutableMapOf<String, Chip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_booking)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        // Get data from intent
        tutorId = intent.getStringExtra("TUTOR_ID") ?: ""
        tutorName = intent.getStringExtra("TUTOR_NAME") ?: ""
        hourlyRate = intent.getDoubleExtra("HOURLY_RATE", 0.0)

        if (tutorId.isEmpty()) {
            Toast.makeText(this, "Error loading tutor information", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        initializeViews()

        // Load current user name
        loadCurrentUserName()

        // Setup tutor info
        setupTutorInfo()

        // Setup dropdowns
        setupDropdowns()

        // Setup listeners
        setupListeners()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        tutorProfileImage = findViewById(R.id.tutorProfileImage)
        tutorInitials = findViewById(R.id.tutorInitials)
        tutorNameText = findViewById(R.id.tutorNameText)
        tutorRateText = findViewById(R.id.tutorRateText)
        subjectAutoComplete = findViewById(R.id.subjectAutoComplete)
        educationLevelAutoComplete = findViewById(R.id.educationLevelAutoComplete)
        daysChipGroup = findViewById(R.id.daysChipGroup)
        preferredTimeEdit = findViewById(R.id.preferredTimeEdit)
        sessionsPerWeekEdit = findViewById(R.id.sessionsPerWeekEdit)
        hoursPerSessionEdit = findViewById(R.id.hoursPerSessionEdit)
        totalHoursText = findViewById(R.id.totalHoursText)
        estimatedPriceText = findViewById(R.id.estimatedPriceText)
        studentNotesEdit = findViewById(R.id.studentNotesEdit)
        submitBookingButton = findViewById(R.id.submitBookingButton)

        // Map day chips
        dayChipMap["Monday"] = findViewById(R.id.chipMonday)
        dayChipMap["Tuesday"] = findViewById(R.id.chipTuesday)
        dayChipMap["Wednesday"] = findViewById(R.id.chipWednesday)
        dayChipMap["Thursday"] = findViewById(R.id.chipThursday)
        dayChipMap["Friday"] = findViewById(R.id.chipFriday)
        dayChipMap["Saturday"] = findViewById(R.id.chipSaturday)
        dayChipMap["Sunday"] = findViewById(R.id.chipSunday)
    }

    private fun loadCurrentUserName() {
        firestore.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                currentUserName = document.getString("name") ?: "Unknown"
            }
    }

    private fun setupTutorInfo() {
        tutorNameText.text = tutorName
        tutorRateText.text = "RM %.0f/hour".format(hourlyRate)

        // Load tutor profile image
        firestore.collection("users").document(tutorId)
            .get()
            .addOnSuccessListener { document ->
                val profileImageUrl = document.getString("profileImageUrl") ?: ""

                if (profileImageUrl.isNotEmpty()) {
                    tutorProfileImage.visibility = View.VISIBLE
                    tutorInitials.visibility = View.GONE
                    Glide.with(this)
                        .load(profileImageUrl)
                        .centerCrop()
                        .into(tutorProfileImage)
                } else {
                    tutorProfileImage.visibility = View.GONE
                    tutorInitials.visibility = View.VISIBLE
                    tutorInitials.text = getInitials(tutorName)
                }
            }
    }

    private fun getInitials(name: String): String {
        val parts = name.trim().split(" ")
        return when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            parts.size == 1 && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "?"
        }
    }

    private fun setupDropdowns() {
        // Load tutor's subjects
        firestore.collection("users").document(tutorId)
            .get()
            .addOnSuccessListener { document ->
                val subjects = document.get("subjectsToTeach") as? List<String> ?: emptyList()
                val subjectAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, subjects)
                subjectAutoComplete.setAdapter(subjectAdapter)

                val educationLevels = document.get("educationLevelsToTeach") as? List<String> ?: emptyList()
                val levelAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, educationLevels)
                educationLevelAutoComplete.setAdapter(levelAdapter)
            }
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        // Time picker
        preferredTimeEdit.setOnClickListener {
            showTimePicker()
        }

        // Calculate total hours when inputs change
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateTotalHours()
            }
        }

        sessionsPerWeekEdit.addTextChangedListener(textWatcher)
        hoursPerSessionEdit.addTextChangedListener(textWatcher)

        // Submit button
        submitBookingButton.setOnClickListener {
            submitBookingRequest()
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val startTime = String.format("%02d:%02d", selectedHour, selectedMinute)

            // Assume 2-hour sessions by default
            val endHour = (selectedHour + 2) % 24
            val endTime = String.format("%02d:%02d", endHour, selectedMinute)

            preferredTimeEdit.setText("$startTime - $endTime")
        }, hour, minute, true).show()
    }

    private fun calculateTotalHours() {
        val sessionsText = sessionsPerWeekEdit.text.toString()
        val hoursText = hoursPerSessionEdit.text.toString()

        if (sessionsText.isNotEmpty() && hoursText.isNotEmpty()) {
            try {
                val sessions = sessionsText.toInt()
                val hours = hoursText.toDouble()

                // Calculate for 4 weeks (1 month)
                val totalHours = sessions * hours * 4
                val totalPrice = totalHours * hourlyRate

                totalHoursText.text = "Total Hours: %.0f hours/month".format(totalHours)
                estimatedPriceText.text = "Estimated Price: RM %.2f/month".format(totalPrice)
            } catch (e: Exception) {
                totalHoursText.text = "Total Hours: 0"
                estimatedPriceText.text = "Estimated Price: RM 0"
            }
        } else {
            totalHoursText.text = "Total Hours: 0"
            estimatedPriceText.text = "Estimated Price: RM 0"
        }
    }

    private fun getSelectedDays(): List<String> {
        val selectedDays = mutableListOf<String>()
        dayChipMap.forEach { (day, chip) ->
            if (chip.isChecked) {
                selectedDays.add(day)
            }
        }
        return selectedDays
    }

    private fun submitBookingRequest() {
        // Validate inputs
        val subject = subjectAutoComplete.text.toString().trim()
        val educationLevel = educationLevelAutoComplete.text.toString().trim()
        val selectedDays = getSelectedDays()
        val preferredTime = preferredTimeEdit.text.toString().trim()
        val sessionsText = sessionsPerWeekEdit.text.toString().trim()
        val hoursText = hoursPerSessionEdit.text.toString().trim()

        when {
            subject.isEmpty() -> {
                Toast.makeText(this, "Please select a subject", Toast.LENGTH_SHORT).show()
                return
            }
            educationLevel.isEmpty() -> {
                Toast.makeText(this, "Please select education level", Toast.LENGTH_SHORT).show()
                return
            }
            selectedDays.isEmpty() -> {
                Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show()
                return
            }
            preferredTime.isEmpty() -> {
                Toast.makeText(this, "Please select preferred time", Toast.LENGTH_SHORT).show()
                return
            }
            sessionsText.isEmpty() -> {
                Toast.makeText(this, "Please enter sessions per week", Toast.LENGTH_SHORT).show()
                return
            }
            hoursText.isEmpty() -> {
                Toast.makeText(this, "Please enter hours per session", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val sessionsPerWeek = sessionsText.toInt()
        val hoursPerSession = hoursText.toDouble()
        val totalHoursPerMonth = (sessionsPerWeek * hoursPerSession * 4).toInt()
        val totalMonthlyPrice = totalHoursPerMonth * hourlyRate

        // Create booking
        val bookingId = firestore.collection("bookings").document().id
        val booking = hashMapOf(
            "bookingId" to bookingId,
            "studentId" to currentUserId,
            "studentName" to currentUserName,
            "tutorId" to tutorId,
            "tutorName" to tutorName,
            "subject" to subject,
            "educationLevel" to educationLevel,
            "preferredDays" to selectedDays,
            "preferredTime" to preferredTime,
            "totalHoursPerMonth" to totalHoursPerMonth,
            "sessionsPerWeek" to sessionsPerWeek,
            "hoursPerSession" to hoursPerSession,
            "hourlyRate" to hourlyRate,
            "totalMonthlyPrice" to totalMonthlyPrice,
            "status" to "PENDING",
            "studentNotes" to studentNotesEdit.text.toString().trim(),
            "tutorNotes" to "",
            "rejectionReason" to "",
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis(),
            "offerMadeAt" to 0L,
            "acceptedAt" to 0L,
            "startDate" to 0L,
            "endDate" to 0L
        )

        // Save to Firestore
        submitBookingButton.isEnabled = false
        firestore.collection("bookings").document(bookingId)
            .set(booking)
            .addOnSuccessListener {
                Toast.makeText(this, "Booking request sent successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                submitBookingButton.isEnabled = true
            }
    }
}
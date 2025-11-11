package com.example.tutor_app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Common views
    private lateinit var profileImageView: ImageView
    private lateinit var profileInitials: TextView
    private lateinit var editProfilePicButton: ImageButton
    private lateinit var userTypeBadge: TextView
    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var logoutButton: MaterialButton

    // Student-specific views
    private lateinit var studentSubjectsCard: MaterialCardView
    private lateinit var subjectsChipGroup: ChipGroup

    // Tutor-specific views
    private lateinit var tutorSubjectsCard: MaterialCardView
    private lateinit var tutorSubjectsChipGroup: ChipGroup
    private lateinit var educationLevelsCard: MaterialCardView
    private lateinit var educationLevelsChipGroup: ChipGroup
    private lateinit var hourlyRateCard: MaterialCardView
    private lateinit var hourlyRateEditText: TextInputEditText
    private lateinit var bioCard: MaterialCardView
    private lateinit var bioEditText: TextInputEditText

    private var userType: String = "student"
    private var profileImageUri: Uri? = null
    private var currentProfileImageUrl: String = ""
    private val selectedSubjects = mutableSetOf<String>()
    private val selectedTutorSubjects = mutableSetOf<String>()
    private val selectedEducationLevels = mutableSetOf<String>()

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                profileImageUri = uri
                displayImage(uri.toString())
                uploadProfileImage(uri)
            }
        }
    }

    // Available subjects
    private val availableSubjects = listOf(
        "Mathematics", "English", "Physics", "Chemistry",
        "Biology", "History", "Geography", "Economics", "Accounting",
        "Computer Science", "Malay Language", "Chinese Language"
    )

    // Education levels that tutors can teach
    private val teachingEducationLevels = listOf(
        "Primary School (Year 1-6)",
        "Secondary School (Form 1-3)",
        "Secondary School (Form 4-5)",
        "A-Level/STPM",
        "Foundation/Diploma",
        "Undergraduate",
        "Postgraduate"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize common views
        profileImageView = view.findViewById(R.id.profileImageView)
        profileInitials = view.findViewById(R.id.profileInitials)
        editProfilePicButton = view.findViewById(R.id.editProfilePicButton)
        userTypeBadge = view.findViewById(R.id.userTypeBadge)
        nameEditText = view.findViewById(R.id.nameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        phoneEditText = view.findViewById(R.id.phoneEditText)
        saveButton = view.findViewById(R.id.saveButton)
        logoutButton = view.findViewById(R.id.logoutButton)

        // Initialize student-specific views
        studentSubjectsCard = view.findViewById(R.id.studentSubjectsCard)
        subjectsChipGroup = view.findViewById(R.id.subjectsChipGroup)

        // Initialize tutor-specific views
        tutorSubjectsCard = view.findViewById(R.id.tutorSubjectsCard)
        tutorSubjectsChipGroup = view.findViewById(R.id.tutorSubjectsChipGroup)
        educationLevelsCard = view.findViewById(R.id.educationLevelsCard)
        educationLevelsChipGroup = view.findViewById(R.id.educationLevelsChipGroup)
        hourlyRateCard = view.findViewById(R.id.hourlyRateCard)
        hourlyRateEditText = view.findViewById(R.id.hourlyRateEditText)
        bioCard = view.findViewById(R.id.bioCard)
        bioEditText = view.findViewById(R.id.bioEditText)

        // Load user data first to determine user type
        loadUserProfile()

        // Edit profile picture button
        editProfilePicButton.setOnClickListener {
            openImagePicker()
        }

        // Save button
        saveButton.setOnClickListener {
            saveProfile()
        }

        // Logout button
        logoutButton.setOnClickListener {
            logout()
        }

        return view
    }

    private fun setupUIForUserType() {
        if (userType == "tutor") {
            // Hide student fields
            studentSubjectsCard.visibility = View.GONE

            // Show tutor-specific fields
            tutorSubjectsCard.visibility = View.VISIBLE
            educationLevelsCard.visibility = View.VISIBLE
            hourlyRateCard.visibility = View.VISIBLE
            bioCard.visibility = View.VISIBLE

            // Setup tutor subjects chips
            setupTutorSubjectsChips()

            // Setup education levels chips
            setupEducationLevelsChips()

            // Change badge background for tutors
            userTypeBadge.setBackgroundResource(R.drawable.tutor_badge_background)
        } else {
            // Student - show student fields, hide tutor fields
            studentSubjectsCard.visibility = View.VISIBLE
            tutorSubjectsCard.visibility = View.GONE
            educationLevelsCard.visibility = View.GONE
            hourlyRateCard.visibility = View.GONE
            bioCard.visibility = View.GONE

            // Setup student subjects chips
            setupSubjectsChips()
        }
    }

    private fun setupSubjectsChips() {
        subjectsChipGroup.removeAllViews()
        availableSubjects.forEach { subject ->
            val chip = Chip(requireContext())
            chip.text = subject
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedSubjects.add(subject)
                } else {
                    selectedSubjects.remove(subject)
                }
            }
            subjectsChipGroup.addView(chip)
        }
    }

    private fun setupTutorSubjectsChips() {
        tutorSubjectsChipGroup.removeAllViews()
        availableSubjects.forEach { subject ->
            val chip = Chip(requireContext())
            chip.text = subject
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedTutorSubjects.add(subject)
                } else {
                    selectedTutorSubjects.remove(subject)
                }
            }
            tutorSubjectsChipGroup.addView(chip)
        }
    }

    private fun setupEducationLevelsChips() {
        educationLevelsChipGroup.removeAllViews()
        teachingEducationLevels.forEach { level ->
            val chip = Chip(requireContext())
            chip.text = level
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedEducationLevels.add(level)
                } else {
                    selectedEducationLevels.remove(level)
                }
            }
            educationLevelsChipGroup.addView(chip)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return

        editProfilePicButton.isEnabled = false
        Toast.makeText(requireContext(), "Uploading image...", Toast.LENGTH_SHORT).show()

        val imageRef = storage.reference
            .child("profile_images")
            .child("$userId.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    currentProfileImageUrl = uri.toString()

                    firestore.collection("users").document(userId)
                        .update("profileImageUrl", currentProfileImageUrl)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
                            editProfilePicButton.isEnabled = true
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            editProfilePicButton.isEnabled = true
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                editProfilePicButton.isEnabled = true
            }
    }

    private fun displayImage(imageUrl: String) {
        if (imageUrl.isNotEmpty()) {
            profileImageView.visibility = View.VISIBLE
            profileInitials.visibility = View.GONE

            Glide.with(this)
                .load(imageUrl)
                .centerCrop()
                .into(profileImageView)
        } else {
            profileImageView.visibility = View.GONE
            profileInitials.visibility = View.VISIBLE
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

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    user?.let {
                        userType = it.userType
                        currentProfileImageUrl = it.profileImageUrl

                        // Setup UI based on user type
                        setupUIForUserType()

                        // Update common UI
                        userTypeBadge.text = it.userType.replaceFirstChar { char ->
                            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                        }
                        nameEditText.setText(it.name)
                        emailEditText.setText(it.email)
                        phoneEditText.setText(it.phoneNumber)

                        // Set profile picture or initials
                        if (currentProfileImageUrl.isNotEmpty()) {
                            displayImage(currentProfileImageUrl)
                        } else {
                            profileInitials.text = getInitials(it.name)
                        }

                        if (userType == "student") {
                            // Load student subjects
                            selectedSubjects.clear()
                            selectedSubjects.addAll(it.subjectsOfInterest)

                            for (i in 0 until subjectsChipGroup.childCount) {
                                val chip = subjectsChipGroup.getChildAt(i) as Chip
                                chip.isChecked = selectedSubjects.contains(chip.text.toString())
                            }
                        } else {
                            // Load tutor data
                            hourlyRateEditText.setText(it.hourlyRate)
                            bioEditText.setText(it.bio)

                            selectedTutorSubjects.clear()
                            selectedTutorSubjects.addAll(it.subjectsToTeach)

                            selectedEducationLevels.clear()
                            selectedEducationLevels.addAll(it.educationLevelsToTeach)

                            // Check tutor subjects chips
                            for (i in 0 until tutorSubjectsChipGroup.childCount) {
                                val chip = tutorSubjectsChipGroup.getChildAt(i) as Chip
                                chip.isChecked = selectedTutorSubjects.contains(chip.text.toString())
                            }

                            // Check education levels chips
                            for (i in 0 until educationLevelsChipGroup.childCount) {
                                val chip = educationLevelsChipGroup.getChildAt(i) as Chip
                                chip.isChecked = selectedEducationLevels.contains(chip.text.toString())
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfile() {
        val name = nameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()

        // Common validation
        if (name.isEmpty()) {
            nameEditText.error = "Name is required"
            nameEditText.requestFocus()
            return
        }

        // Tutor-specific validation
        if (userType == "tutor") {
            val hourlyRate = hourlyRateEditText.text.toString().trim()
            val bio = bioEditText.text.toString().trim()

            if (selectedTutorSubjects.isEmpty()) {
                Toast.makeText(requireContext(), "Please select at least one subject to teach", Toast.LENGTH_SHORT).show()
                return
            }

            if (selectedEducationLevels.isEmpty()) {
                Toast.makeText(requireContext(), "Please select at least one education level to teach", Toast.LENGTH_SHORT).show()
                return
            }

            if (hourlyRate.isEmpty()) {
                hourlyRateEditText.error = "Hourly rate is required"
                hourlyRateEditText.requestFocus()
                return
            }

            if (bio.isEmpty() || bio.length < 50) {
                bioEditText.error = "Please write at least 50 characters about yourself"
                bioEditText.requestFocus()
                return
            }
        } else {
            // Student validation
            if (selectedSubjects.isEmpty()) {
                Toast.makeText(requireContext(), "Please select at least one subject of interest", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Show loading
        saveButton.isEnabled = false
        saveButton.text = "Saving..."

        val userId = auth.currentUser?.uid ?: return

        // Update initials if name changed
        profileInitials.text = getInitials(name)

        // Prepare updates based on user type
        val updates = if (userType == "tutor") {
            hashMapOf<String, Any>(
                "name" to name,
                "phoneNumber" to phone,
                "subjectsToTeach" to selectedTutorSubjects.toList(),
                "educationLevelsToTeach" to selectedEducationLevels.toList(),
                "hourlyRate" to hourlyRateEditText.text.toString().trim(),
                "bio" to bioEditText.text.toString().trim()
            )
        } else {
            hashMapOf<String, Any>(
                "name" to name,
                "phoneNumber" to phone,
                "subjectsOfInterest" to selectedSubjects.toList()
            )
        }

        firestore.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                saveButton.isEnabled = true
                saveButton.text = "Save Profile"
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                saveButton.isEnabled = true
                saveButton.text = "Save Profile"
            }
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
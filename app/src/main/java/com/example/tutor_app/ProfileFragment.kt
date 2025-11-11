package com.example.tutor_app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
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

    private lateinit var profileImageView: ImageView
    private lateinit var profileInitials: TextView
    private lateinit var editProfilePicButton: ImageButton
    private lateinit var userTypeBadge: TextView
    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var educationLevelDropdown: AutoCompleteTextView
    private lateinit var subjectsChipGroup: ChipGroup
    private lateinit var saveButton: MaterialButton
    private lateinit var logoutButton: MaterialButton

    private var userType: String = "student"
    private var profileImageUri: Uri? = null
    private var currentProfileImageUrl: String = ""
    private val selectedSubjects = mutableListOf<String>()

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

    // Education levels
    private val educationLevels = listOf(
        "Primary School", "Secondary School", "A-Level/STPM",
        "Foundation", "Diploma", "Undergraduate", "Postgraduate"
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

        // Initialize views
        profileImageView = view.findViewById(R.id.profileImageView)
        profileInitials = view.findViewById(R.id.profileInitials)
        editProfilePicButton = view.findViewById(R.id.editProfilePicButton)
        userTypeBadge = view.findViewById(R.id.userTypeBadge)
        nameEditText = view.findViewById(R.id.nameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        phoneEditText = view.findViewById(R.id.phoneEditText)
        educationLevelDropdown = view.findViewById(R.id.educationLevelDropdown)
        subjectsChipGroup = view.findViewById(R.id.subjectsChipGroup)
        saveButton = view.findViewById(R.id.saveButton)
        logoutButton = view.findViewById(R.id.logoutButton)

        // Setup education level dropdown
        setupEducationLevelDropdown()

        // Setup subjects chips
        setupSubjectsChips()

        // Load user data
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

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return

        // Show progress
        editProfilePicButton.isEnabled = false
        Toast.makeText(requireContext(), "Uploading image...", Toast.LENGTH_SHORT).show()

        // Create reference to storage
        val imageRef = storage.reference
            .child("profile_images")
            .child("$userId.jpg")

        // Upload image
        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                // Get download URL
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    currentProfileImageUrl = uri.toString()

                    // Update Firestore
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

            // Load image using Glide
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

    private fun setupEducationLevelDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            educationLevels
        )
        educationLevelDropdown.setAdapter(adapter)
    }

    private fun setupSubjectsChips() {
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

                        // Update UI - capitalize first letter
                        userTypeBadge.text = it.userType.replaceFirstChar { char ->
                            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                        }
                        nameEditText.setText(it.name)
                        emailEditText.setText(it.email)
                        phoneEditText.setText(it.phoneNumber)
                        educationLevelDropdown.setText(it.educationLevel, false)

                        // Set profile picture or initials
                        if (currentProfileImageUrl.isNotEmpty()) {
                            displayImage(currentProfileImageUrl)
                        } else {
                            profileInitials.text = getInitials(it.name)
                        }

                        // Set selected subjects
                        selectedSubjects.clear()
                        selectedSubjects.addAll(it.subjectsOfInterest)

                        // Check the chips
                        for (i in 0 until subjectsChipGroup.childCount) {
                            val chip = subjectsChipGroup.getChildAt(i) as Chip
                            chip.isChecked = selectedSubjects.contains(chip.text.toString())
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
        val educationLevel = educationLevelDropdown.text.toString()

        // Validation
        if (name.isEmpty()) {
            nameEditText.error = "Name is required"
            nameEditText.requestFocus()
            return
        }

        if (educationLevel.isEmpty()) {
            Toast.makeText(requireContext(), "Please select education level", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        saveButton.isEnabled = false
        saveButton.text = "Saving..."

        val userId = auth.currentUser?.uid ?: return

        // Update initials if name changed
        profileInitials.text = getInitials(name)

        // Update user data
        val updates = hashMapOf<String, Any>(
            "name" to name,
            "phoneNumber" to phone,
            "educationLevel" to educationLevel,
            "subjectsOfInterest" to selectedSubjects
        )

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
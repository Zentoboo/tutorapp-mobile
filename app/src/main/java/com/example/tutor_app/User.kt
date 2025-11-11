package com.example.tutor_app

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val userType: String = "", // "student" or "tutor"
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),

    // Student-specific fields
    val subjectsOfInterest: List<String> = emptyList(),

    // Tutor-specific fields
    val subjectsToTeach: List<String> = emptyList(),
    val educationLevelsToTeach: List<String> = emptyList(),
    val hourlyRate: String = "",
    val bio: String = "",
    val rating: Double = 0.0,
    val totalReviews: Int = 0
)
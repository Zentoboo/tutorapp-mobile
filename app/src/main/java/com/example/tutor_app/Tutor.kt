package com.example.tutor_app

data class Tutor(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    val subjectsToTeach: List<String> = emptyList(),
    val educationLevelsToTeach: List<String> = emptyList(),
    val hourlyRate: String = "",
    val bio: String = "",
    val rating: Double = 0.0,
    val totalReviews: Int = 0
) {
    // Helper function to get initials
    fun getInitials(): String {
        val parts = name.trim().split(" ")
        return when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            parts.size == 1 && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "?"
        }
    }

    // Helper function to get subjects as comma-separated string
    fun getSubjectsString(): String {
        return when {
            subjectsToTeach.isEmpty() -> "No subjects listed"
            subjectsToTeach.size <= 2 -> subjectsToTeach.joinToString(", ")
            else -> "${subjectsToTeach.take(2).joinToString(", ")} +${subjectsToTeach.size - 2}"
        }
    }
}
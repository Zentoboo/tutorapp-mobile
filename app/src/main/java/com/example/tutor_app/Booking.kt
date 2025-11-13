package com.example.tutor_app

data class Booking(
    val bookingId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val tutorId: String = "",
    val tutorName: String = "",
    val subject: String = "",
    val educationLevel: String = "",

    // Schedule details
    val preferredDays: List<String> = emptyList(), // e.g., ["Monday", "Wednesday", "Friday"]
    val preferredTime: String = "", // e.g., "3:00 PM - 5:00 PM"
    val totalHoursPerMonth: Int = 0,
    val sessionsPerWeek: Int = 0,
    val hoursPerSession: Double = 0.0,

    // Pricing
    val hourlyRate: Double = 0.0,
    val totalMonthlyPrice: Double = 0.0, // hourlyRate * totalHoursPerMonth

    // Status and notes
    val status: String = "PENDING", // PENDING, OFFER_MADE, ACCEPTED, REJECTED, COMPLETED, CANCELLED
    val studentNotes: String = "",
    val tutorNotes: String = "",
    val rejectionReason: String = "",

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val offerMadeAt: Long = 0L,
    val acceptedAt: Long = 0L,
    val startDate: Long = 0L, // When the tutoring starts
    val endDate: Long = 0L // When the tutoring ends (1 month from start)
) {
    // Helper functions
    fun getStatusDisplay(): String {
        return when (status) {
            "PENDING" -> "Pending Tutor Response"
            "OFFER_MADE" -> "Offer Received"
            "ACCEPTED" -> "Active"
            "REJECTED" -> "Rejected"
            "COMPLETED" -> "Completed"
            "CANCELLED" -> "Cancelled"
            else -> status
        }
    }

    fun getStatusColor(): Int {
        return when (status) {
            "PENDING" -> android.graphics.Color.parseColor("#F59E0B") // warning
            "OFFER_MADE" -> android.graphics.Color.parseColor("#3B82F6") // info
            "ACCEPTED" -> android.graphics.Color.parseColor("#10B981") // success
            "REJECTED" -> android.graphics.Color.parseColor("#EF4444") // error
            "COMPLETED" -> android.graphics.Color.parseColor("#6B7280") // text_secondary
            "CANCELLED" -> android.graphics.Color.parseColor("#9CA3AF") // text_hint
            else -> android.graphics.Color.parseColor("#111827") // text_primary
        }
    }

    fun getScheduleSummary(): String {
        val days = preferredDays.joinToString(", ")
        return "$days at $preferredTime"
    }
}
package com.example.tutor_app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ScheduleFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var bookingsRecyclerView: RecyclerView? = null
    private var bookingAdapter: BookingAdapter? = null
    private var emptyStateLayout: LinearLayout? = null
    private var loadingProgressBar: ProgressBar? = null

    private var bookingsListener: ListenerRegistration? = null
    private var currentUserId: String = ""
    private var userType: String = "student"
    private var isUserTypeLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            val view = inflater.inflate(R.layout.fragment_schedule, container, false)

            // Initialize Firebase
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            currentUserId = auth.currentUser?.uid ?: ""

            if (currentUserId.isEmpty()) {
                return view
            }

            // Initialize views
            bookingsRecyclerView = view.findViewById(R.id.bookingsRecyclerView)
            emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
            loadingProgressBar = view.findViewById(R.id.loadingProgressBar)

            // IMPORTANT: Get user type FIRST, then setup everything else
            showLoading(true)
            getUserType()

            view
        } catch (e: Exception) {
            android.util.Log.e("ScheduleFragment", "Error in onCreateView", e)
            View(requireContext())
        }
    }

    private fun getUserType() {
        firestore.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                userType = document.getString("userType") ?: "student"
                isUserTypeLoaded = true

                android.util.Log.d("ScheduleFragment", "User type loaded: $userType")

                // Now that we have userType, setup RecyclerView and load bookings
                setupRecyclerView()
                loadBookings()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ScheduleFragment", "Error loading user type", e)
                userType = "student"
                isUserTypeLoaded = true

                // Continue anyway with default
                setupRecyclerView()
                loadBookings()
            }
    }

    private fun setupRecyclerView() {
        try {
            if (!isUserTypeLoaded) {
                android.util.Log.w("ScheduleFragment", "setupRecyclerView called before userType loaded!")
                return
            }

            android.util.Log.d("ScheduleFragment", "Setting up RecyclerView with userType: $userType")

            bookingAdapter = BookingAdapter(
                emptyList(),
                currentUserId,
                userType,
                onViewDetails = { booking ->
                    openBookingDetails(booking)
                },
                onAccept = { booking ->
                    if (userType == "tutor") {
                        acceptBookingRequest(booking)
                    } else {
                        acceptTutorOffer(booking)
                    }
                },
                onReject = { booking ->
                    showRejectDialog(booking)
                }
            )

            bookingsRecyclerView?.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = bookingAdapter
            }
        } catch (e: Exception) {
            android.util.Log.e("ScheduleFragment", "Error setting up RecyclerView", e)
        }
    }

    private fun loadBookings() {
        try {
            if (!isUserTypeLoaded) {
                android.util.Log.w("ScheduleFragment", "loadBookings called before userType loaded!")
                return
            }

            showLoading(true)

            val field = if (userType == "tutor") "tutorId" else "studentId"

            android.util.Log.d("ScheduleFragment", "Loading bookings for $field = $currentUserId")

            bookingsListener = firestore.collection("bookings")
                .whereEqualTo(field, currentUserId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, error ->
                    if (!isAdded) return@addSnapshotListener

                    try {
                        showLoading(false)

                        if (error != null) {
                            android.util.Log.e("ScheduleFragment", "Error loading bookings", error)
                            bookingsRecyclerView?.visibility = View.GONE
                            emptyStateLayout?.visibility = View.VISIBLE
                            return@addSnapshotListener
                        }

                        val bookings = snapshots?.documents?.mapNotNull { doc ->
                            try {
                                Booking(
                                    bookingId = doc.id,
                                    studentId = doc.getString("studentId") ?: "",
                                    studentName = doc.getString("studentName") ?: "",
                                    tutorId = doc.getString("tutorId") ?: "",
                                    tutorName = doc.getString("tutorName") ?: "",
                                    subject = doc.getString("subject") ?: "",
                                    educationLevel = doc.getString("educationLevel") ?: "",
                                    preferredDays = doc.get("preferredDays") as? List<String> ?: emptyList(),
                                    preferredTime = doc.getString("preferredTime") ?: "",
                                    totalHoursPerMonth = (doc.getLong("totalHoursPerMonth") ?: 0L).toInt(),
                                    sessionsPerWeek = (doc.getLong("sessionsPerWeek") ?: 0L).toInt(),
                                    hoursPerSession = doc.getDouble("hoursPerSession") ?: 0.0,
                                    hourlyRate = doc.getDouble("hourlyRate") ?: 0.0,
                                    totalMonthlyPrice = doc.getDouble("totalMonthlyPrice") ?: 0.0,
                                    status = doc.getString("status") ?: "PENDING",
                                    studentNotes = doc.getString("studentNotes") ?: "",
                                    tutorNotes = doc.getString("tutorNotes") ?: "",
                                    rejectionReason = doc.getString("rejectionReason") ?: "",
                                    createdAt = doc.getLong("createdAt") ?: 0L,
                                    updatedAt = doc.getLong("updatedAt") ?: 0L,
                                    offerMadeAt = doc.getLong("offerMadeAt") ?: 0L,
                                    acceptedAt = doc.getLong("acceptedAt") ?: 0L,
                                    startDate = doc.getLong("startDate") ?: 0L,
                                    endDate = doc.getLong("endDate") ?: 0L
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("ScheduleFragment", "Error parsing booking", e)
                                null
                            }
                        } ?: emptyList()

                        android.util.Log.d("ScheduleFragment", "Loaded ${bookings.size} bookings")
                        bookings.forEach { booking ->
                            android.util.Log.d("ScheduleFragment", "Booking: ${booking.subject} - Status: ${booking.status}")
                        }

                        bookingAdapter?.updateBookings(bookings)

                        if (bookings.isEmpty()) {
                            bookingsRecyclerView?.visibility = View.GONE
                            emptyStateLayout?.visibility = View.VISIBLE
                        } else {
                            bookingsRecyclerView?.visibility = View.VISIBLE
                            emptyStateLayout?.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ScheduleFragment", "Error in snapshot listener", e)
                        showLoading(false)
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("ScheduleFragment", "Error in loadBookings", e)
            showLoading(false)
            emptyStateLayout?.visibility = View.VISIBLE
        }
    }

    private fun openBookingDetails(booking: Booking) {
        val otherPartyName = if (userType == "tutor") booking.studentName else booking.tutorName

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Booking with $otherPartyName")
            .setMessage("""
                Subject: ${booking.subject}
                Education Level: ${booking.educationLevel}
                
                Schedule: ${booking.getScheduleSummary()}
                Hours: ${booking.totalHoursPerMonth} hours/month
                Sessions: ${booking.sessionsPerWeek}x per week (${booking.hoursPerSession}h each)
                
                Price: RM ${String.format("%.2f", booking.totalMonthlyPrice)}
                Status: ${booking.getStatusDisplay()}
                
                ${if (booking.studentNotes.isNotEmpty()) "Student Notes:\n${booking.studentNotes}\n\n" else ""}
                ${if (booking.tutorNotes.isNotEmpty()) "Tutor Notes:\n${booking.tutorNotes}\n\n" else ""}
                ${if (booking.rejectionReason.isNotEmpty()) "Rejection Reason:\n${booking.rejectionReason}" else ""}
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun acceptBookingRequest(booking: Booking) {
        // Tutor accepts the student's request as-is
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Accept Booking")
            .setMessage("Accept this booking request from ${booking.studentName}?")
            .setPositiveButton("Accept") { _, _ ->
                val updates = hashMapOf<String, Any>(
                    "status" to "ACCEPTED",
                    "updatedAt" to System.currentTimeMillis(),
                    "acceptedAt" to System.currentTimeMillis(),
                    "startDate" to System.currentTimeMillis(),
                    "endDate" to System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // 30 days
                )

                firestore.collection("bookings").document(booking.bookingId)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Booking accepted! 🎉", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun acceptTutorOffer(booking: Booking) {
        // Student accepts the tutor's counter-offer
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Accept Offer")
            .setMessage("Accept the offer from ${booking.tutorName}?")
            .setPositiveButton("Accept") { _, _ ->
                val updates = hashMapOf<String, Any>(
                    "status" to "ACCEPTED",
                    "updatedAt" to System.currentTimeMillis(),
                    "acceptedAt" to System.currentTimeMillis(),
                    "startDate" to System.currentTimeMillis(),
                    "endDate" to System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // 30 days
                )

                firestore.collection("bookings").document(booking.bookingId)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Booking confirmed! 🎉", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRejectDialog(booking: Booking) {
        val input = android.widget.EditText(requireContext())
        input.hint = "Reason for rejection (optional)"
        input.setPadding(50, 20, 50, 20)

        val otherPartyName = if (userType == "tutor") booking.studentName else booking.tutorName

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reject Booking")
            .setMessage("Are you sure you want to reject this booking with $otherPartyName?")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString().trim()
                rejectBooking(booking, reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectBooking(booking: Booking, reason: String) {
        val updates = hashMapOf<String, Any>(
            "status" to "REJECTED",
            "rejectionReason" to reason,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("bookings").document(booking.bookingId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Booking rejected", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        try {
            if (show) {
                loadingProgressBar?.visibility = View.VISIBLE
                bookingsRecyclerView?.visibility = View.GONE
                emptyStateLayout?.visibility = View.GONE
            } else {
                loadingProgressBar?.visibility = View.GONE
            }
        } catch (e: Exception) {
            android.util.Log.e("ScheduleFragment", "Error in showLoading", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bookingsListener?.remove()
        bookingsListener = null
        bookingsRecyclerView = null
        bookingAdapter = null
        emptyStateLayout = null
        loadingProgressBar = null
    }
}
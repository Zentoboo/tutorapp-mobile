package com.example.tutor_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class BookingAdapter(
    private var bookings: List<Booking>,
    private val currentUserId: String,
    private val userType: String, // "student" or "tutor"
    private val onViewDetails: (Booking) -> Unit,
    private val onAccept: (Booking) -> Unit,
    private val onReject: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val otherPartyNameText: TextView = itemView.findViewById(R.id.otherPartyNameText)
        private val subjectText: TextView = itemView.findViewById(R.id.subjectText)
        private val statusBadge: TextView = itemView.findViewById(R.id.statusBadge)
        private val scheduleText: TextView = itemView.findViewById(R.id.scheduleText)
        private val hoursText: TextView = itemView.findViewById(R.id.hoursText)
        private val priceText: TextView = itemView.findViewById(R.id.priceText)
        private val actionButtonsLayout: LinearLayout = itemView.findViewById(R.id.actionButtonsLayout)
        private val acceptButton: MaterialButton = itemView.findViewById(R.id.acceptButton)
        private val rejectButton: MaterialButton = itemView.findViewById(R.id.rejectButton)
        private val viewDetailsButton: MaterialButton = itemView.findViewById(R.id.viewDetailsButton)

        fun bind(booking: Booking) {
            // Show tutor or student name based on user type
            otherPartyNameText.text = if (userType == "student") {
                booking.tutorName
            } else {
                booking.studentName
            }

            // Subject and education level
            subjectText.text = buildString {
                append(booking.subject)
                append(" - ")
                append(booking.educationLevel)
            }

            // Status badge
            statusBadge.text = booking.getStatusDisplay()
            statusBadge.setBackgroundColor(booking.getStatusColor())

            // Schedule
            scheduleText.text = booking.getScheduleSummary()

            // Hours
            hoursText.text = buildString {
                append(booking.totalHoursPerMonth)
                append(" hours/month")
            }

            // Price
            priceText.text = buildString {
                append("RM ")
                append(String.format("%.2f", booking.totalMonthlyPrice))
            }

            // Action buttons visibility
            val showActions = when {
                // Tutor sees actions for PENDING requests
                userType == "tutor" && booking.status == "PENDING" -> true
                // Student sees actions for OFFER_MADE
                userType == "student" && booking.status == "OFFER_MADE" -> true
                else -> false
            }

            if (showActions) {
                actionButtonsLayout.visibility = View.VISIBLE

                // Update button text based on user type
                when (userType) {
                    "tutor" -> {
                        acceptButton.text = "Accept"
                        rejectButton.text = "Reject"
                    }
                    else -> {
                        acceptButton.text = "Accept Offer"
                        rejectButton.text = "Decline"
                    }
                }
            } else {
                actionButtonsLayout.visibility = View.GONE
            }

            // Button listeners
            acceptButton.setOnClickListener {
                onAccept(booking)
            }

            rejectButton.setOnClickListener {
                onReject(booking)
            }

            viewDetailsButton.setOnClickListener {
                onViewDetails(booking)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()
    }
}
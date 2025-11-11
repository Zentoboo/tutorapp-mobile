package com.example.tutor_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class TutorAdapter(
    private var tutors: List<Tutor>,
    private val onTutorClick: (Tutor) -> Unit
) : RecyclerView.Adapter<TutorAdapter.TutorViewHolder>() {

    inner class TutorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tutorImageView: ImageView = itemView.findViewById(R.id.tutorImageView)
        val tutorInitials: TextView = itemView.findViewById(R.id.tutorInitials)
        val tutorNameText: TextView = itemView.findViewById(R.id.tutorNameText)
        val tutorSubjectsText: TextView = itemView.findViewById(R.id.tutorSubjectsText)
        val tutorRatingText: TextView = itemView.findViewById(R.id.tutorRatingText)
        val tutorReviewsText: TextView = itemView.findViewById(R.id.tutorReviewsText)
        val tutorRateText: TextView = itemView.findViewById(R.id.tutorRateText)

        fun bind(tutor: Tutor) {
            tutorNameText.text = tutor.name
            tutorSubjectsText.text = tutor.getSubjectsString()
            tutorRatingText.text = if (tutor.rating > 0) "%.1f".format(tutor.rating) else "New"
            tutorReviewsText.text = if (tutor.totalReviews > 0) "(${tutor.totalReviews} reviews)" else "(No reviews yet)"
            tutorRateText.text = "RM ${tutor.hourlyRate}/hour"

            // Display profile picture or initials
            if (tutor.profileImageUrl.isNotEmpty()) {
                tutorImageView.visibility = View.VISIBLE
                tutorInitials.visibility = View.GONE
                Glide.with(itemView.context)
                    .load(tutor.profileImageUrl)
                    .centerCrop()
                    .into(tutorImageView)
            } else {
                tutorImageView.visibility = View.GONE
                tutorInitials.visibility = View.VISIBLE
                tutorInitials.text = tutor.getInitials()
            }

            // Click listener
            itemView.setOnClickListener {
                onTutorClick(tutor)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tutor_card, parent, false)
        return TutorViewHolder(view)
    }

    override fun onBindViewHolder(holder: TutorViewHolder, position: Int) {
        holder.bind(tutors[position])
    }

    override fun getItemCount(): Int = tutors.size

    // Update tutor list (for filtering/searching)
    fun updateTutors(newTutors: List<Tutor>) {
        tutors = newTutors
        notifyDataSetChanged()
    }
}
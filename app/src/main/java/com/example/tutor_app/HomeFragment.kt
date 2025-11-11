package com.example.tutor_app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var tutorsRecyclerView: RecyclerView
    private lateinit var tutorAdapter: TutorAdapter
    private lateinit var searchEditText: TextInputEditText
    private lateinit var subjectFilterChipGroup: ChipGroup
    private lateinit var tutorCountText: TextView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var loadingProgressBar: ProgressBar

    private var allTutors = listOf<Tutor>()
    private var filteredTutors = listOf<Tutor>()
    private val selectedSubjects = mutableSetOf<String>()
    private var studentSubjectsOfInterest = listOf<String>()

    private val availableSubjects = listOf(
        "All", "Mathematics", "English", "Physics", "Chemistry",
        "Biology", "History", "Geography", "Economics", "Accounting",
        "Computer Science", "Malay Language", "Chinese Language"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        tutorsRecyclerView = view.findViewById(R.id.tutorsRecyclerView)
        searchEditText = view.findViewById(R.id.searchEditText)
        subjectFilterChipGroup = view.findViewById(R.id.subjectFilterChipGroup)
        tutorCountText = view.findViewById(R.id.tutorCountText)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup search functionality
        setupSearch()

        // Load student's subjects of interest first, then setup chips
        loadStudentPreferences()

        return view
    }

    private fun loadStudentPreferences() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    studentSubjectsOfInterest = document.get("subjectsOfInterest") as? List<String> ?: emptyList()
                }

                // Now setup chips with student's preferences
                setupSubjectChips()

                // Load tutors from Firestore
                loadTutors()
            }
            .addOnFailureListener {
                // If loading preferences fails, just continue without them
                setupSubjectChips()
                loadTutors()
            }
    }

    private fun setupRecyclerView() {
        tutorAdapter = TutorAdapter(emptyList()) { tutor ->
            // Navigate to TutorDetailActivity
            val intent = Intent(requireContext(), TutorDetailActivity::class.java)
            intent.putExtra("TUTOR_ID", tutor.userId)
            startActivity(intent)
        }

        tutorsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tutorAdapter
        }
    }

    private fun setupSubjectChips() {
        subjectFilterChipGroup.removeAllViews()

        // Check if student has subjects of interest
        val hasInterests = studentSubjectsOfInterest.isNotEmpty()

        availableSubjects.forEach { subject ->
            val chip = Chip(requireContext())
            chip.text = subject
            chip.isCheckable = true

            // Auto-select chips based on student's interests
            if (subject == "All") {
                // Select "All" only if student has no interests
                chip.isChecked = !hasInterests
            } else if (hasInterests && studentSubjectsOfInterest.contains(subject)) {
                // Auto-select student's subjects of interest
                chip.isChecked = true
                selectedSubjects.add(subject)
            }

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (subject == "All") {
                    if (isChecked) {
                        // Clear all other selections
                        selectedSubjects.clear()
                        for (i in 0 until subjectFilterChipGroup.childCount) {
                            val otherChip = subjectFilterChipGroup.getChildAt(i) as Chip
                            if (otherChip.text != "All") {
                                otherChip.isChecked = false
                            }
                        }
                    }
                } else {
                    // Uncheck "All" when other subject is selected
                    if (isChecked) {
                        selectedSubjects.add(subject)
                        (subjectFilterChipGroup.getChildAt(0) as Chip).isChecked = false
                    } else {
                        selectedSubjects.remove(subject)
                        // If no subjects selected, select "All"
                        if (selectedSubjects.isEmpty()) {
                            (subjectFilterChipGroup.getChildAt(0) as Chip).isChecked = true
                        }
                    }
                }
                filterTutors()
            }

            subjectFilterChipGroup.addView(chip)
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterTutors()
            }
        })
    }

    private fun loadTutors() {
        showLoading(true)

        firestore.collection("users")
            .whereEqualTo("userType", "tutor")
            .get()
            .addOnSuccessListener { documents ->
                allTutors = documents.mapNotNull { doc ->
                    try {
                        Tutor(
                            userId = doc.id,
                            name = doc.getString("name") ?: "",
                            email = doc.getString("email") ?: "",
                            phoneNumber = doc.getString("phoneNumber") ?: "",
                            profileImageUrl = doc.getString("profileImageUrl") ?: "",
                            subjectsToTeach = doc.get("subjectsToTeach") as? List<String> ?: emptyList(),
                            educationLevelsToTeach = doc.get("educationLevelsToTeach") as? List<String> ?: emptyList(),
                            hourlyRate = doc.getString("hourlyRate") ?: "0",
                            bio = doc.getString("bio") ?: "",
                            rating = doc.getDouble("rating") ?: 0.0,
                            totalReviews = doc.getLong("totalReviews")?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                showLoading(false)
                filterTutors()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error loading tutors: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun filterTutors() {
        val searchQuery = searchEditText.text.toString().lowercase().trim()
        val allSelected = subjectFilterChipGroup.childCount > 0 &&
                (subjectFilterChipGroup.getChildAt(0) as? Chip)?.isChecked == true

        filteredTutors = allTutors.filter { tutor ->
            // Search filter
            val matchesSearch = searchQuery.isEmpty() ||
                    tutor.name.lowercase().contains(searchQuery) ||
                    tutor.subjectsToTeach.any { it.lowercase().contains(searchQuery) }

            // Subject filter
            val matchesSubject = allSelected ||
                    selectedSubjects.isEmpty() ||
                    tutor.subjectsToTeach.any { selectedSubjects.contains(it) }

            matchesSearch && matchesSubject
        }

        updateUI()
    }

    private fun updateUI() {
        tutorAdapter.updateTutors(filteredTutors)
        tutorCountText.text = "${filteredTutors.size} tutor${if (filteredTutors.size != 1) "s" else ""}"

        if (filteredTutors.isEmpty()) {
            tutorsRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            tutorsRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            loadingProgressBar.visibility = View.VISIBLE
            tutorsRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.GONE
        } else {
            loadingProgressBar.visibility = View.GONE
        }
    }

    // Refresh data when fragment becomes visible again
    override fun onResume() {
        super.onResume()
        // Reload preferences in case they changed
        loadStudentPreferences()
    }
}
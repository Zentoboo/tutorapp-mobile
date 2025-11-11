package com.example.tutor_app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MessagesFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var chatsRecyclerView: RecyclerView? = null
    private var chatAdapter: ChatAdapter? = null
    private var emptyStateLayout: LinearLayout? = null
    private var loadingProgressBar: ProgressBar? = null

    private var chatsListener: ListenerRegistration? = null
    private var currentUserId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            val view = inflater.inflate(R.layout.fragment_messages, container, false)

            // Initialize Firebase
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            currentUserId = auth.currentUser?.uid ?: ""

            if (currentUserId.isEmpty()) {
                return view
            }

            // Initialize views
            chatsRecyclerView = view.findViewById(R.id.chatsRecyclerView)
            emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
            loadingProgressBar = view.findViewById(R.id.loadingProgressBar)

            // Setup RecyclerView
            setupRecyclerView()

            // Load chats
            loadChats()

            view
        } catch (e: Exception) {
            android.util.Log.e("MessagesFragment", "Error in onCreateView", e)
            View(requireContext())
        }
    }

    private fun setupRecyclerView() {
        try {
            chatAdapter = ChatAdapter(emptyList(), currentUserId) { chat ->
                try {
                    if (!isAdded) return@ChatAdapter

                    val intent = Intent(requireContext(), ChatActivity::class.java)
                    intent.putExtra("CHAT_ID", chat.chatId)
                    intent.putExtra("OTHER_USER_ID", chat.getOtherParticipantId(currentUserId))
                    intent.putExtra("OTHER_USER_NAME", chat.getOtherParticipantName(currentUserId))
                    startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("MessagesFragment", "Error opening chat", e)
                }
            }

            chatsRecyclerView?.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = chatAdapter
            }
        } catch (e: Exception) {
            android.util.Log.e("MessagesFragment", "Error setting up RecyclerView", e)
        }
    }

    private fun loadChats() {
        try {
            showLoading(true)

            chatsListener = firestore.collection("chats")
                .whereArrayContains("participantIds", currentUserId)
                .addSnapshotListener { snapshots, error ->
                    if (!isAdded) return@addSnapshotListener

                    try {
                        showLoading(false)

                        if (error != null) {
                            android.util.Log.e("MessagesFragment", "Error loading chats", error)
                            chatsRecyclerView?.visibility = View.GONE
                            emptyStateLayout?.visibility = View.VISIBLE
                            return@addSnapshotListener
                        }

                        val chats = snapshots?.documents?.mapNotNull { doc ->
                            try {
                                // Properly handle unreadCount as Map<String, Any> from Firestore
                                val unreadCountRaw = doc.get("unreadCount") as? Map<String, Any> ?: emptyMap()
                                val unreadCount = unreadCountRaw.mapValues { (_, value) ->
                                    when (value) {
                                        is Long -> value
                                        is Int -> value.toLong()
                                        else -> 0L
                                    }
                                }

                                Chat(
                                    chatId = doc.id,
                                    participantIds = doc.get("participantIds") as? List<String> ?: emptyList(),
                                    participantNames = doc.get("participantNames") as? Map<String, String> ?: emptyMap(),
                                    participantTypes = doc.get("participantTypes") as? Map<String, String> ?: emptyMap(),
                                    lastMessage = doc.getString("lastMessage") ?: "",
                                    lastMessageTime = doc.getLong("lastMessageTime") ?: System.currentTimeMillis(),
                                    unreadCount = unreadCount // Now properly typed as Map<String, Long>
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("MessagesFragment", "Error parsing chat", e)
                                null
                            }
                        } ?: emptyList()

                        chatAdapter?.updateChats(chats)

                        if (chats.isEmpty()) {
                            chatsRecyclerView?.visibility = View.GONE
                            emptyStateLayout?.visibility = View.VISIBLE
                        } else {
                            chatsRecyclerView?.visibility = View.VISIBLE
                            emptyStateLayout?.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MessagesFragment", "Error in snapshot listener", e)
                        showLoading(false)
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("MessagesFragment", "Error in loadChats", e)
            showLoading(false)
            emptyStateLayout?.visibility = View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        try {
            if (show) {
                loadingProgressBar?.visibility = View.VISIBLE
                chatsRecyclerView?.visibility = View.GONE
                emptyStateLayout?.visibility = View.GONE
            } else {
                loadingProgressBar?.visibility = View.GONE
            }
        } catch (e: Exception) {
            android.util.Log.e("MessagesFragment", "Error in showLoading", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listener to prevent memory leaks
        chatsListener?.remove()
        chatsListener = null

        // Clear references
        chatsRecyclerView = null
        chatAdapter = null
        emptyStateLayout = null
        loadingProgressBar = null
    }
}
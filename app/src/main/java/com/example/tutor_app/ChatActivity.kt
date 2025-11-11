package com.example.tutor_app

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var backButton: ImageButton
    private lateinit var chatProfileImage: ImageView
    private lateinit var chatProfileInitials: TextView
    private lateinit var chatTitleText: TextView
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageEditText: TextInputEditText
    private lateinit var sendButton: FloatingActionButton

    private var chatId: String = ""
    private var otherUserId: String = ""
    private var otherUserName: String = ""
    private var currentUserId: String = ""
    private var currentUserName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        // Get data from intent
        chatId = intent.getStringExtra("CHAT_ID") ?: ""
        otherUserId = intent.getStringExtra("OTHER_USER_ID") ?: ""
        otherUserName = intent.getStringExtra("OTHER_USER_NAME") ?: ""

        // Initialize views
        backButton = findViewById(R.id.backButton)
        chatProfileImage = findViewById(R.id.chatProfileImage)
        chatProfileInitials = findViewById(R.id.chatProfileInitials)
        chatTitleText = findViewById(R.id.chatTitleText)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        // Set chat title
        chatTitleText.text = otherUserName

        // Load other user's profile picture
        loadOtherUserProfile()

        // Back button
        backButton.setOnClickListener {
            finish()
        }

        // Get current user name
        loadCurrentUserName()

        // Setup RecyclerView
        setupRecyclerView()

        // If no chat ID, create new chat
        if (chatId.isEmpty()) {
            createNewChat()
        } else {
            // Load existing messages
            loadMessages()
            markMessagesAsRead()
        }

        // Send button
        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun loadOtherUserProfile() {
        firestore.collection("users").document(otherUserId)
            .get()
            .addOnSuccessListener { document ->
                val profileImageUrl = document.getString("profileImageUrl") ?: ""

                if (profileImageUrl.isNotEmpty()) {
                    // Show profile image
                    chatProfileImage.visibility = View.VISIBLE
                    chatProfileInitials.visibility = View.GONE

                    Glide.with(this)
                        .load(profileImageUrl)
                        .centerCrop()
                        .into(chatProfileImage)
                } else {
                    // Show initials
                    chatProfileImage.visibility = View.GONE
                    chatProfileInitials.visibility = View.VISIBLE
                    chatProfileInitials.text = getInitials(otherUserName)
                }
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

    private fun loadCurrentUserName() {
        firestore.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                currentUserName = document.getString("name") ?: "Unknown"
            }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(emptyList(), currentUserId)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true

        messagesRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = messageAdapter
        }
    }

    private fun createNewChat() {
        chatId = firestore.collection("chats").document().id

        android.util.Log.d("ChatActivity", "Creating new chat with ID: $chatId")

        firestore.collection("users").document(otherUserId)
            .get()
            .addOnSuccessListener { otherUserDoc ->
                val otherUserType = otherUserDoc.getString("userType") ?: "student"

                firestore.collection("users").document(currentUserId)
                    .get()
                    .addOnSuccessListener { currentUserDoc ->
                        val currentUserType = currentUserDoc.getString("userType") ?: "student"

                        val chat = hashMapOf(
                            "participantIds" to listOf(currentUserId, otherUserId),
                            "participantNames" to mapOf(
                                currentUserId to currentUserName,
                                otherUserId to otherUserName
                            ),
                            "participantTypes" to mapOf(
                                currentUserId to currentUserType,
                                otherUserId to otherUserType
                            ),
                            "lastMessage" to "",
                            "lastMessageTime" to System.currentTimeMillis(),
                            "unreadCount" to mapOf(
                                currentUserId to 0,
                                otherUserId to 0
                            )
                        )

                        firestore.collection("chats").document(chatId)
                            .set(chat)
                            .addOnSuccessListener {
                                android.util.Log.d("ChatActivity", "Chat created successfully!")
                                loadMessages()
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("ChatActivity", "Failed to create chat", e)
                                Toast.makeText(this, "Failed to create chat: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
            }
    }

    private fun loadMessages() {
        firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading messages: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val messages = snapshots?.documents?.mapNotNull { doc ->
                    try {
                        Message(
                            messageId = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            receiverId = doc.getString("receiverId") ?: "",
                            receiverName = doc.getString("receiverName") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isRead = doc.getBoolean("isRead") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                messageAdapter.updateMessages(messages)

                if (messages.isNotEmpty()) {
                    messagesRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
    }

    private fun sendMessage() {
        val messageText = messageEditText.text.toString().trim()

        if (messageText.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        val messageId = firestore.collection("chats").document(chatId)
            .collection("messages").document().id

        val message = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "senderName" to currentUserName,
            "receiverId" to otherUserId,
            "receiverName" to otherUserName,
            "text" to messageText,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )

        firestore.collection("chats").document(chatId)
            .collection("messages").document(messageId)
            .set(message)
            .addOnSuccessListener {
                val chatUpdate = hashMapOf<String, Any>(
                    "lastMessage" to messageText,
                    "lastMessageTime" to System.currentTimeMillis(),
                    "unreadCount.$otherUserId" to com.google.firebase.firestore.FieldValue.increment(1)
                )

                firestore.collection("chats").document(chatId)
                    .update(chatUpdate)

                messageEditText.text?.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun markMessagesAsRead() {
        firestore.collection("chats").document(chatId)
            .update("unreadCount.$currentUserId", 0)
    }
}
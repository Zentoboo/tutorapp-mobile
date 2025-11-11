package com.example.tutor_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ChatAdapter(
    private var chats: List<Chat>,
    private val currentUserId: String,
    private val onChatClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatInitials: TextView = itemView.findViewById(R.id.chatInitials)
        val chatNameText: TextView = itemView.findViewById(R.id.chatNameText)
        val chatTimeText: TextView = itemView.findViewById(R.id.chatTimeText)
        val chatLastMessageText: TextView = itemView.findViewById(R.id.chatLastMessageText)
        val unreadBadge: TextView = itemView.findViewById(R.id.unreadBadge)

        fun bind(chat: Chat) {
            val otherParticipantName = chat.getOtherParticipantName(currentUserId)

            // Display name
            chatNameText.text = otherParticipantName

            // Display initials
            chatInitials.text = getInitials(otherParticipantName)

            // Display last message
            chatLastMessageText.text = if (chat.lastMessage.isNotEmpty()) {
                chat.lastMessage
            } else {
                "Start a conversation"
            }

            // Display time
            chatTimeText.text = getTimeAgo(chat.lastMessageTime)

            // Display unread badge - Fixed to handle Long type
            val unreadCount = chat.unreadCount[currentUserId] ?: 0L // Changed to 0L for Long
            if (unreadCount > 0) {
                unreadBadge.visibility = View.VISIBLE
                unreadBadge.text = if (unreadCount > 9) "9+" else unreadCount.toString()
            } else {
                unreadBadge.visibility = View.GONE
            }

            // Click listener
            itemView.setOnClickListener {
                onChatClick(chat)
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

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
                diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
                diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size

    fun updateChats(newChats: List<Chat>) {
        chats = newChats
        notifyDataSetChanged()
    }
}
package com.example.tutor_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private var messages: List<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderMessageLayout: LinearLayout = itemView.findViewById(R.id.senderMessageLayout)
        val senderMessageText: TextView = itemView.findViewById(R.id.senderMessageText)
        val senderTimeText: TextView = itemView.findViewById(R.id.senderTimeText)

        val receiverMessageLayout: LinearLayout = itemView.findViewById(R.id.receiverMessageLayout)
        val receiverMessageText: TextView = itemView.findViewById(R.id.receiverMessageText)
        val receiverTimeText: TextView = itemView.findViewById(R.id.receiverTimeText)

        fun bind(message: Message) {
            val isSender = message.senderId == currentUserId

            if (isSender) {
                // Show sender layout (right side)
                senderMessageLayout.visibility = View.VISIBLE
                receiverMessageLayout.visibility = View.GONE
                senderMessageText.text = message.text
                senderTimeText.text = formatTime(message.timestamp)
            } else {
                // Show receiver layout (left side)
                senderMessageLayout.visibility = View.GONE
                receiverMessageLayout.visibility = View.VISIBLE
                receiverMessageText.text = message.text
                receiverTimeText.text = formatTime(message.timestamp)
            }
        }

        private fun formatTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
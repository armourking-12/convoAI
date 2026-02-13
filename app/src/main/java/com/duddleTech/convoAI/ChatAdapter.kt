package com.duddleTech.convoAI

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation

class ChatAdapter(private val messageList: List<Message>) : RecyclerView.Adapter<ChatAdapter.MyViewHolder>() {

    // Track animation state
    private var lastPosition = -1

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Bot Side
        val leftChatLayout: LinearLayout = itemView.findViewById(R.id.leftChatLayout)
        val leftChatText: TextView = itemView.findViewById(R.id.leftChatText)
        val leftChatImage: ImageView = itemView.findViewById(R.id.leftChatImage)

        // User Side
        val rightChatLayout: LinearLayout = itemView.findViewById(R.id.rightChatLayout)
        val rightChatText: TextView = itemView.findViewById(R.id.rightChatText)
        val rightChatImage: ImageView = itemView.findViewById(R.id.rightChatImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // ✅ FIX: Use 'bindingAdapterPosition' instead of 'position'
        val currentPos = holder.bindingAdapterPosition

        // Safety Check: If item was deleted/moved instantly, stop
        if (currentPos == RecyclerView.NO_POSITION) return

        val message = messageList[currentPos]

        // --- ANIMATION ---
        if (currentPos > lastPosition) {
            val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_message_enter)
            holder.itemView.startAnimation(animation)
            lastPosition = currentPos
        }

        // --- BIND DATA ---
        if (message.sentBy == Message.SENT_BY_USER) {
            // SHOW RIGHT (User)
            holder.rightChatLayout.visibility = View.VISIBLE
            holder.leftChatLayout.visibility = View.GONE

            if (message.message.isNotEmpty()) {
                holder.rightChatText.visibility = View.VISIBLE
                holder.rightChatText.text = message.message
            } else {
                holder.rightChatText.visibility = View.GONE
            }

            if (message.imageUrl != null) {
                holder.rightChatImage.visibility = View.VISIBLE
                holder.rightChatImage.load(message.imageUrl) {
                    transformations(RoundedCornersTransformation(16f))
                }
            } else {
                holder.rightChatImage.visibility = View.GONE
            }

        } else {
            // SHOW LEFT (Bot)
            holder.leftChatLayout.visibility = View.VISIBLE
            holder.rightChatLayout.visibility = View.GONE

            if (message.message.isNotEmpty()) {
                holder.leftChatText.visibility = View.VISIBLE
                holder.leftChatText.text = message.message
            } else {
                holder.leftChatText.visibility = View.GONE
            }

            if (message.imageUrl != null) {
                holder.leftChatImage.visibility = View.VISIBLE
                holder.leftChatImage.load(message.imageUrl) {
                    transformations(RoundedCornersTransformation(16f))
                }
            } else {
                holder.leftChatImage.visibility = View.GONE
            }
        }
    }

    // Stop animation when view is recycled
    override fun onViewDetachedFromWindow(holder: MyViewHolder) {
        holder.itemView.clearAnimation()
    }

    override fun getItemCount(): Int = messageList.size
}
package com.duddleTech.convoAI

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil // ✅ Added for Pro performance
import androidx.recyclerview.widget.RecyclerView

class SidebarAdapter(
    private var sessions: List<ChatSession>,
    private val onChatClick: (ChatSession) -> Unit,
    private val onRenameClick: (ChatSession) -> Unit,
    private val onDeleteClick: (ChatSession) -> Unit,
    private val onShareClick: (ChatSession) -> Unit
) : RecyclerView.Adapter<SidebarAdapter.SidebarViewHolder>() {

    class SidebarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.sidebarChatTitle)
        val btnOptions: ImageView = view.findViewById(R.id.btnChatOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SidebarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sidebar_chat, parent, false)
        return SidebarViewHolder(view)
    }

    override fun onBindViewHolder(holder: SidebarViewHolder, position: Int) {
        val session = sessions[position]
        holder.title.text = session.title

        holder.itemView.setOnClickListener { onChatClick(session) }

        holder.btnOptions.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menu.add(0, 1, 0, "Rename")
            popup.menu.add(0, 2, 1, "Share")
            popup.menu.add(0, 3, 2, "Delete")

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    1 -> onRenameClick(session)
                    2 -> onShareClick(session)
                    3 -> onDeleteClick(session)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount() = sessions.size

    // ✅ THE PRO WAY: Uses DiffUtil instead of notifyDataSetChanged
    fun updateData(newSessions: List<ChatSession>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = sessions.size
            override fun getNewListSize() = newSessions.size

            // Checks if it's the same item (by ID)
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return sessions[oldPos].id == newSessions[newPos].id
            }

            // Checks if the content changed (Title updated?)
            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return sessions[oldPos] == newSessions[newPos]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        sessions = newSessions
        diffResult.dispatchUpdatesTo(this) // This runs the specific animations automatically!
    }
}
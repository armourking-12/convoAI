package com.duddleTech.convoAI

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Prompt(val text: String, val icon: String)

class SuggestionAdapter(
    private val prompts: List<Prompt>,
    private val onPromptClick: (String) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.promptText)
        val icon: TextView = view.findViewById(R.id.promptIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val prompt = prompts[position]
        holder.text.text = prompt.text
        holder.icon.text = prompt.icon

        holder.itemView.setOnClickListener {
            onPromptClick(prompt.text)
        }
    }

    override fun getItemCount() = prompts.size
}
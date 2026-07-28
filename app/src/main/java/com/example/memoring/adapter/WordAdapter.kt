package com.example.memoring.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.memoring.R
import com.example.memoring.data.WordListItem
import androidx.core.content.ContextCompat

class WordAdapter(
    private var items: List<WordListItem>,
    private val onFavoriteClick: (Int) -> Unit,
    private val onItemClick: (WordListItem) -> Unit
) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    class WordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvWord: TextView = view.findViewById(R.id.tv_word)
        val tvMeaning: TextView = view.findViewById(R.id.tv_meaning)
        val tvFavorite: TextView = view.findViewById(R.id.tv_favorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_word, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val item = items[position]
        holder.tvWord.text = item.word
        holder.tvMeaning.text = item.meaning
        holder.tvFavorite.text = if (item.isFavorite) "★" else "☆"
        holder.tvFavorite.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (item.isFavorite) R.color.accent else R.color.ink_soft
            )
        )
        holder.tvFavorite.setOnClickListener { onFavoriteClick(item.wordId) }
        holder.itemView.setOnClickListener { onItemClick(item) }

        // 카테고리별 색 탭
        val tabColor = when (item.categoryId) {
            1 -> R.color.teal
            2 -> R.color.rust
            else -> R.color.accent
        }
        holder.itemView.findViewById<View>(R.id.tab_color)
            .setBackgroundColor(ContextCompat.getColor(holder.itemView.context, tabColor))

        // 암기상태 색점
        val statusColor = when (item.memorizationStatus) {
            "KNOWN" -> R.color.teal
            "CONFUSED", "UNKNOWN" -> R.color.rust
            else -> R.color.ink_soft
        }
        holder.itemView.findViewById<View>(R.id.status_dot)
            .setBackgroundColor(ContextCompat.getColor(holder.itemView.context, statusColor))
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<WordListItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
package com.example.koreantoenglishflashcardsaver

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.koreantoenglishflashcardsaver.model.Flashcard

class FlashCardAdapter(private val context: Context, private val onItemClick: (position: Int, id: Int) -> Unit)
    : ListAdapter<Flashcard, FlashCardAdapter.ItemViewHolder>(FlashCardComparator()){

        class ItemViewHolder(private val view: View, private val onItemClick: (position: Int, id: Int) -> Unit)
            : RecyclerView.ViewHolder(view){
            val flashcard: ConstraintLayout = view.findViewById(R.id.flashcard)
            val word: TextView = view.findViewById(R.id.word_item)
            val translation: TextView = view.findViewById(R.id.word_item_translation)
            val closebutton: Button = view.findViewById(R.id.close_button)
            var id: Int = -1
            init {
                closebutton.setOnClickListener {
                    deleteCard()
                }
            }

            private fun deleteCard() {
                val position = absoluteAdapterPosition
                onItemClick(position, id)
            }
        }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).
            inflate(R.layout.flashcard_layout, parent, false)
        return ItemViewHolder(adapterLayout){ position, id -> onItemClick(position, id) }
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.word.text = item.word
        holder.translation.text = item.translation
        holder.id = item.id
    }

    class FlashCardComparator : DiffUtil.ItemCallback<Flashcard>() {
        override fun areItemsTheSame(oldItem: Flashcard, newItem: Flashcard): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Flashcard, newItem: Flashcard): Boolean {
            return (oldItem.id == newItem.id && oldItem.word == newItem.word && oldItem.translation == newItem.translation)
        }
    }
}
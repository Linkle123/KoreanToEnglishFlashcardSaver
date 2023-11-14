package com.example.koreantoenglishflashcardsaver

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.koreantoenglishflashcardsaver.Flashcard

class FlashCardAdapter(private val context: Context)
    : ListAdapter<Flashcard, FlashCardAdapter.ItemViewHolder>(FlashCardComparator()){
        class ItemViewHolder(private val view: View)
            : RecyclerView.ViewHolder(view){
            val journal: ConstraintLayout = view.findViewById(R.id.flashcard)
            var id : Int = -1
            val word: EditText = view.findViewById(R.id.word_item)
            val translation: EditText = view.findViewById(R.id.word_item_translation)
        }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).
            inflate(R.layout.flashcard_layout, parent, false)
        return ItemViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.word.setText(item.word)
        holder.translation.setText(item.translation)
    }

    class FlashCardComparator : DiffUtil.ItemCallback<Flashcard>() {
        override fun areItemsTheSame(oldItem: Flashcard, newItem: Flashcard): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Flashcard, newItem: Flashcard): Boolean {
            return (oldItem.word == newItem.word && oldItem.translation == newItem.translation)
        }
    }
}
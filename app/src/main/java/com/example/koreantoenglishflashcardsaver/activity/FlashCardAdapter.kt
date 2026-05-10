package com.example.koreantoenglishflashcardsaver.activity

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.koreantoenglishflashcardsaver.R
import com.example.koreantoenglishflashcardsaver.model.Flashcard

class FlashCardAdapter(private val context: Context, private val onCloseClick: (position: Int, id: Int) -> Unit, private val onCardClick: ((flashcard: Flashcard, position: Int) -> Unit))
    : ListAdapter<Flashcard, FlashCardAdapter.ItemViewHolder>(FlashCardComparator()){

        class ItemViewHolder(private val view: View, private val onCloseClick: (position: Int, id: Int) -> Unit)
            : RecyclerView.ViewHolder(view){
            val flashcard: ConstraintLayout = view.findViewById(R.id.flashcard)
            val word: TextView = view.findViewById(R.id.word_item)
            val translation: TextView = view.findViewById(R.id.word_item_translations)
            val border2: View = view.findViewById(R.id.border2)
            val exampleSentences: TextView = view.findViewById(R.id.word_item_examples)
            val border3: View = view.findViewById(R.id.border3)
            val directTranslation: TextView = view.findViewById(R.id.word_item_direct_translation)
            val closebutton: Button = view.findViewById(R.id.close_button)
            var id: Int = -1
            init {
                closebutton.setOnClickListener {
                    deleteCard()
                }
            }

            private fun deleteCard() {
                val position = absoluteAdapterPosition
                onCloseClick(position, id)
            }
        }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).
            inflate(R.layout.flashcard_layout, parent, false)
        return ItemViewHolder(adapterLayout){ position, id -> onCloseClick(position, id) }
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.word.context
        // Gets each text Value and updates the view accordingly
        updateTextView(holder.translation, holder.border2, item.getTranslationsAsString(), context.getString(R.string.word_translations_title))
        updateTextView(holder.exampleSentences, holder.border2, item.getExamplesAsString(), context.getString(R.string.examples_title))
        updateTextView(holder.directTranslation, holder.border3, item.directTranslation, context.getString(R.string.full_translation_title))
        holder.word.text = item.word
        holder.id = item.id
        Log.i("item position", position.toString())
        Log.i("item id", item.id.toString())
        holder.itemView.setOnClickListener {
            onCardClick.invoke(item, position) // Trigger the callback
            Log.i("item position", position.toString())
            Log.i("item id", item.id.toString())
        }
    }

    /**
     * If the text is not null, sets the given textview's text to that text.
     * If the text is null, sets both the given textview and border's visibility to View.GONE
      */
    fun updateTextView(view: TextView, border: View, text: String?, title: String){
        if (text != null) {
            view.visibility = View.VISIBLE
            border.visibility = View.VISIBLE
            view.text = "$title $text"
        }
        else {
            view.visibility = View.GONE
            border.visibility = View.GONE
        }
    }

    class FlashCardComparator : DiffUtil.ItemCallback<Flashcard>() {
        override fun areItemsTheSame(oldItem: Flashcard, newItem: Flashcard): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Flashcard, newItem: Flashcard): Boolean {
            return (oldItem.id == newItem.id && oldItem.word == newItem.word &&
                    oldItem.translations == newItem.translations && oldItem.examples == newItem.examples)
        }
    }
}
package com.example.koreantoenglishflashcardsaver.model

import android.content.Context
import androidx.lifecycle.Observer
import com.example.koreantoenglishflashcardsaver.activity.FlashCardAdapter
import com.example.koreantoenglishflashcardsaver.R
import com.example.koreantoenglishflashcardsaver.activity.MainActivity

class DatabaseUtils(contextApp: Context, databaseReference: DatabaseViewModel) {
    private val databaseViewModel: DatabaseViewModel
    lateinit var adapter: FlashCardAdapter
    val context: Context
    init {
        databaseViewModel = databaseReference
        context = contextApp
    }

    fun connectAdapter(flashcardAdapter: FlashCardAdapter){
        adapter = flashcardAdapter
    }

    fun connectRecyclerToData(mainActivity: MainActivity){
        databaseViewModel.allFlashcards.observe(mainActivity, Observer {
            it?.let {
                adapter.submitList(it)
            }
        })
    }

    /** Makes a new Flashcard based on the given fields and inserts it into the database and
     * RecyclerView Adapter
     */
    fun addCard(word: String,
                translations: MutableList<TranslationEntry>?,
                examples: MutableList<ExampleEntry>? = null,
                directTranslations: String? = null) {
        // First check if any of the fields have something.
        if((word != "") && (((translations != null) && translations.isNotEmpty()) || (directTranslations != null) || (examples != null && examples.isNotEmpty()))) {
            val card = Flashcard(word = word, translations = translations, examples = examples, directTranslation = directTranslations)
            databaseViewModel.insertFlashCard(card)
            adapter.notifyDataSetChanged()
        }
    }

    /**
     * Updates the provided flashcard, and the recyclerview at the provided position
     */
    fun updateCard(flashcard: Flashcard, position: Int){
        databaseViewModel.updateFlashcard(flashcard)
        adapter.notifyItemChanged(position)
    }

    fun onItemCloseClick(position: Int, id: Int) {
        databaseViewModel.deleteByFlashcardId(id)
        adapter.notifyItemRemoved(position)
    }

    fun getSelectedDeckName(): String {
        var selectedDeck = databaseViewModel.getSelectedDeck()
        if (selectedDeck == null){
            selectedDeck = handleMissingSelectedDeck()
        }
        return selectedDeck.deckName
    }

    fun convertFlashcardsToStringArray(): MutableList<Array<String>>{
        val flashcardArray = mutableListOf<Array<String>>()
        if(databaseViewModel.allFlashcards.value != null) {
            for (flashcard: Flashcard in databaseViewModel.allFlashcards.value!!) {
                val element = arrayOf<String>(
                    flashcard.word,
                    flashcard.getTranslationsAsString()?: "",
                    flashcard.getExamplesAsString()?: "",
                    flashcard.directTranslation?: "")
                flashcardArray.add(element)
            }
        }
        return flashcardArray
    }

    fun convertDecksToStringArray(): ArrayList<String>{
        val deckArray = ArrayList<String>()
        for (deck: Deck in databaseViewModel.getAllAlphabetizedDecks()){
            deckArray.add(deck.deckName)
        }
        return deckArray
    }

    fun clearArray(){
        databaseViewModel.deleteAllFlashcards()
        adapter.notifyDataSetChanged()
    }

    fun insertDeckIfNew(deckName: String){
        val deck = databaseViewModel.getDeckByName(deckName)
        if(deck == null){
            databaseViewModel.insertDeck(Deck(deckName, false))
        }
    }

    fun updateSelectedDeck(deckName: String): String{
        var oldSelectedDeck = databaseViewModel.getSelectedDeck()
        val newDeck = databaseViewModel.getDeckByName(deckName)

        if(oldSelectedDeck == null){
            oldSelectedDeck = handleMissingSelectedDeck()
        }
        if(oldSelectedDeck == newDeck){
            return oldSelectedDeck.deckName
        }
        if(newDeck != null) {
            oldSelectedDeck.selected = false
            newDeck.selected = true
            databaseViewModel.updateDecks(oldSelectedDeck, newDeck)
            return deckName
        }
        else {
            return oldSelectedDeck.deckName
        }
    }

    fun handleMissingSelectedDeck(): Deck{
        val allDecks = databaseViewModel.getAllAlphabetizedDecks()
        if(allDecks.size == 0){
            val newDeck = Deck(context.resources.getString(R.string.deck_name), true)
            databaseViewModel.insertDeck(newDeck)
            return newDeck
        }
        else{
            val firstDeck = allDecks.first()
            firstDeck.selected = true
            databaseViewModel.updateDecks(firstDeck)
            return firstDeck
        }
    }
}
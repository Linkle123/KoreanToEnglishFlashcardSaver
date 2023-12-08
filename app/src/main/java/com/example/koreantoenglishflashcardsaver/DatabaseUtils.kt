package com.example.koreantoenglishflashcardsaver

import android.content.Context
import androidx.lifecycle.Observer
import com.example.koreantoenglishflashcardsaver.model.DatabaseViewModel
import com.example.koreantoenglishflashcardsaver.model.Deck
import com.example.koreantoenglishflashcardsaver.model.Flashcard

class DatabaseUtils(contextApp: Context, databaseReference: DatabaseViewModel, flashcardAdapter: FlashCardAdapter) {
    private val databaseViewModel: DatabaseViewModel
    val adapter: FlashCardAdapter
    val context: Context
    init {
        databaseViewModel = databaseReference
        adapter = flashcardAdapter
        context = contextApp
    }

    fun connectRecyclerToData(mainActivity: MainActivity){
        databaseViewModel.allFlashcards.observe(mainActivity, Observer {
            it?.let {
                adapter.submitList(it)
            }
        })
    }

    fun addCard(word: String, translation: String) {
        if(word != "" && translation != "") {
            val card = Flashcard(word, translation)
            databaseViewModel.insertFlashCard(card)
            adapter.notifyDataSetChanged()
        }
    }

    fun onItemCloseClick(position: Int) {
        databaseViewModel.deleteByFlashcardId(position)
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
                val element = arrayOf<String>(flashcard.word, flashcard.translation)
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
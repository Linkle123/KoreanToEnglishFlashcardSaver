package com.example.koreantoenglishflashcardsaver.model


import androidx.lifecycle.*
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch

class DatabaseViewModel(private val database: AppRoomDatabase) : ViewModel() {

    val allFlashcards: LiveData<List<Flashcard>> = database.flashcardDao().getAlphabetizedFlashCardByTranslation().asLiveData()

    fun deleteByFlashcardId(journal: Int) = viewModelScope.launch {
        database.flashcardDao().deleteByFlashcardId(journal)
    }

    fun insertFlashCard(Flashcard: Flashcard) = viewModelScope.launch {
        database.flashcardDao().insert(Flashcard)
    }
    fun deleteAllFlashcards() = viewModelScope.launch {
        database.flashcardDao().deleteAll()
    }

    fun updateFlashcards(Flashcard: Flashcard) = viewModelScope.launch {
        database.flashcardDao().updateFlashcards(Flashcard)
    }

    fun getAllAlphabetizedDecks(): List<Deck>{
        return database.deckDao().getAlphabetizedDecks()
    }

    fun insertDeck(deck: Deck) = viewModelScope.launch {
        database.deckDao().insert(deck)
    }

    fun getSelectedDeck(): Deck{
        return database.deckDao().getSelectedDeck(true)
    }

    fun getDeckByName(deckName: String): Deck{
        return database.deckDao().getDeckByName(deckName)
    }

    fun deleteAllDecks() = viewModelScope.launch {
        database.deckDao().deleteAll()
    }

    fun updateDecks(vararg deck: Deck) = viewModelScope.launch {
        database.deckDao().updateDecks(*deck)
    }
}

class DatabaseViewModelFactory(private val database: AppRoomDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DatabaseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DatabaseViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
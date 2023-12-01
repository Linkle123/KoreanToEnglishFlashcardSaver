package com.example.koreantoenglishflashcardsaver.model

import androidx.lifecycle.*
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch

class DeckViewModel(private val database: AppRoomDatabase) : ViewModel() {

    val allDecks: LiveData<List<Deck>> = database.deckDao().getAlphabetizedDecks().asLiveData()

    fun insert(deck: Deck) = viewModelScope.launch {
        database.deckDao().insert(deck)
    }
    fun deleteAll() = viewModelScope.launch {
        database.deckDao().deleteAll()
    }

    fun updateDecks(deck: Deck) = viewModelScope.launch {
        database.deckDao().updateDecks(deck)
    }
}

class DeckViewModelFactory(private val database: AppRoomDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeckViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeckViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
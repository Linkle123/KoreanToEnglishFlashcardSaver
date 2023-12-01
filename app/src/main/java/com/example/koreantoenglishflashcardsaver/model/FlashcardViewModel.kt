package com.example.koreantoenglishflashcardsaver.model


import androidx.lifecycle.*
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch

class FlashcardViewModel(private val database: AppRoomDatabase) : ViewModel() {

    val allFlashcards: LiveData<List<Flashcard>> = database.flashcardDao().getAlphabetizedFlashCardByTranslation().asLiveData()

    fun deleteByFlashcardId(journal: Int) = viewModelScope.launch {
        database.flashcardDao().deleteByFlashcardId(journal)
    }

    fun insert(Flashcard: Flashcard) = viewModelScope.launch {
        database.flashcardDao().insert(Flashcard)
    }
    fun deleteAll() = viewModelScope.launch {
        database.flashcardDao().deleteAll()
    }

    fun updateFlashcards(Flashcard: Flashcard) = viewModelScope.launch {
        database.flashcardDao().updateFlashcards(Flashcard)
    }
}

class FlashcardViewModelFactory(private val database: AppRoomDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FlashcardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FlashcardViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
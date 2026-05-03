package com.example.koreantoenglishflashcardsaver.translate

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.koreantoenglishflashcardsaver.model.Flashcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jsoup.nodes.Document

class TranslateViewModel (translateRepository: TranslateRepository): ViewModel(){
    // Define the possible events
    sealed class TranslationEvent {
        data class Success(val card: Flashcard) : TranslationEvent()
        object ApiError : TranslationEvent()
        data class ResultIncomplete(val card: Flashcard) : TranslationEvent()
    }
    private val _events = Channel<TranslationEvent>()
    val events = _events.receiveAsFlow()
    val translateRepository: TranslateRepository
    init {
        this.translateRepository = translateRepository
    }
    /**
      *  Connects to the internet to scrape the Naver Korean-English Dictionary for results for the given word.
      *  If the result is found, it returns a Flashcard object with the base word, variations of the words with
      *  translations, and example sentences and meanings.
      *  If the result is not found, it returns a Flashcard object with the base word, the result of
      *  a direct translation, and no example sentences.
      */
    fun translate(word: String){
        viewModelScope.launch(Dispatchers.IO) {
            Log.i("Status update", "Inside TranslateViewModel")
            val result = translateRepository.getTranslation(word)
            when {
                result.word.isEmpty() -> {
                    _events.send(TranslationEvent.ApiError)
                }
                result.examples == null -> {
                    _events.send(TranslationEvent.ResultIncomplete(result))
                }
                else -> {
                    _events.send(TranslationEvent.Success(result))
                }
            }
        }
    }


}
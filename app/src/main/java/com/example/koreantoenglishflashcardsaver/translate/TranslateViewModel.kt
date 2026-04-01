package com.example.koreantoenglishflashcardsaver.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.koreantoenglishflashcardsaver.model.Flashcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jsoup.nodes.Document

class TranslateViewModel (translateRepository: TranslateRepository): ViewModel(){
    private val _cardState = MutableStateFlow(Flashcard())
    val cardState: StateFlow<Flashcard> = _cardState.asStateFlow()
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
            val result = translateRepository.getTranslation(word)
            _cardState.update { currentState ->
                currentState.copy(
                    word = result.word,
                    translations = result.translations,
                    examples = result.examples
                )
            }
        }
    }


}
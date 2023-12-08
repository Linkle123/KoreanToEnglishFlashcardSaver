package com.example.koreantoenglishflashcardsaver

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import com.example.koreantoenglishflashcardsaver.model.Deck
import com.ichi2.anki.api.AddContentApi
import com.ichi2.anki.api.NoteInfo
import java.util.concurrent.Executors

class AnkiApi(contextApp: Context, databaseUtils: DatabaseUtils) {
    val ankiAPI: AddContentApi
    val context: Context
    val databaseHelper: DatabaseUtils
    init {
        ankiAPI = AddContentApi(contextApp)
        context = contextApp
        databaseHelper = databaseUtils
    }

    fun saveAllCards(deckName: String) {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            val deckId: Long? = getOrGenerateDeckId(deckName)
            val modelId: Long? = getOrGenerateModelId()

            if ((deckId == null) || (modelId == null)) {
                executor.shutdown()
            }

            databaseHelper.updateSelectedDeck(deckName)
            val convertedCards = databaseHelper.convertFlashcardsToStringArray()
            val filteredCards = handleDuplicateCards(convertedCards, modelId!!).toList()
            if (filteredCards.size != 0) {
                ankiAPI.addNotes(modelId!!, deckId!!, filteredCards, null)
            }
            handler.post{
                databaseHelper.clearArray()
            }
        }
    }

    fun handleDuplicateCards(cards: MutableList<Array<String>>, modelId: Long): MutableList<Array<String>>{
        val wordsArray = mutableListOf<String>()
        for(card in cards){
            wordsArray.add(card[0])
        }
        val duplicatesArray: SparseArray<MutableList<NoteInfo?>>? = ankiAPI.findDuplicateNotes(modelId, wordsArray)
        if (duplicatesArray == null || duplicatesArray.size() == 0){
            return cards
        }
        val fieldIterator: MutableListIterator<Array<String>> = cards.listIterator()
        var listIndex = -1
        for (i in 0 until duplicatesArray.size()) {
            val duplicateIndex: Int = duplicatesArray.keyAt(i)
            while (listIndex < duplicateIndex) {
                fieldIterator.next()
                listIndex++
            }
            fieldIterator.remove()
        }
        return cards
    }

    fun getOrGenerateDeckId(deckName: String): Long? {
        var deckId: Long? = getDeckId(deckName)
        if (deckId == null) {
            deckId = generateDeckInAnki(deckName)
        }
        return deckId
    }

    fun getOrGenerateModelId(): Long?{
        var modelId: Long? = getModelId(context.resources.getString(R.string.model_name))
        if (modelId == null) {
            modelId = ankiAPI.addNewBasicModel(context.resources.getString(R.string.model_name))
        }
        return modelId
    }

    fun getModelId(modelName: String): Long? {
        val modelList: Map<Long, String>? = ankiAPI.modelList
        if (modelList != null) {
            for ((key, value) in modelList) {
                if (value.equals(modelName, ignoreCase = true)) {
                    return key
                }
            }
        }
        return null
    }

    fun getDeckId(deckName: String): Long? {
        val deckList: Map<Long, String>? = ankiAPI.deckList
        if (deckList != null) {
            for ((key, value) in deckList) {
                if (value.equals(deckName, ignoreCase = true)) {
                    return key
                }
            }
        }
        return null
    }


    fun generateDeckInAnki(deckName: String): Long? {
        val deckId = ankiAPI.addNewDeck(deckName)
        if(deckId != null){
            databaseHelper.insertDeckIfNew(deckName)
        }
        return deckId
    }

    fun generateMultipleDecksInAnki(decks: ArrayList<String>) {
        for(deck in decks) {
            val deckId = ankiAPI.addNewDeck(deck)
            if (deckId != null) {
                databaseHelper.insertDeckIfNew(deck)
            }
        }
    }

}
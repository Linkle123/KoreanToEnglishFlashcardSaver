package com.example.koreantoenglishflashcardsaver

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.SparseArray
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation
import com.ichi2.anki.api.AddContentApi
import com.ichi2.anki.api.NoteInfo
import java.util.Date
import java.util.Properties
import java.util.concurrent.Executors
import android.os.Handler
import android.util.Log

class MainActivity : ComponentActivity() {
    lateinit var adapter: FlashCardAdapter
    var flashcards: ArrayList<Flashcard> = ArrayList<Flashcard>()

    val dayInMilliseconds: Long = 86400000
    lateinit var translateService: Translate
    val sourceLanguage: String = "ko"
    val targetLanguage: String = "en"
    lateinit var inputText: EditText
    lateinit var outputText: EditText

    lateinit var ankiAPI: AddContentApi

    @SuppressLint("DirectDateInstantiation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)
        adapter = FlashCardAdapter(this, flashcards) { position -> onItemCloseClick(position) }
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = adapter

        /*
        val properties: Properties = Properties()
        val propertiesFile = Thread.currentThread().contextClassLoader?.getResourceAsStream("local.properties")
        properties.load(propertiesFile)
        val accessKey = properties.getProperty("TRANSLATION_API_KEY")
        */

        var accessKey: String? = null
        this.packageManager.getApplicationInfo(this.packageName, PackageManager.GET_META_DATA)
            .apply{
            accessKey = metaData.getString("com.google.android.translate.API_KEY")
        }

        if(accessKey != null) {
            //val expirationDate = Date(Date().getTime() + dayInMilliseconds)
           // val credentials = GoogleCredentials.newBuilder().setAccessToken(AccessToken(accessKey, expirationDate)).build()
            translateService = TranslateOptions.newBuilder().setApiKey(accessKey).build().service
        }
        else{
            Toast.makeText(this, getResources().getString(R.string.translation_api_fail), Toast.LENGTH_LONG).show()
        }


        // Initialize the textfields to null
        inputText = findViewById(R.id.translate_text)
        outputText = findViewById(R.id.translated_text)
        inputText.text = null
        outputText.text = null

        // Initialize buttons
        val translateButton = findViewById<Button>(R.id.translate_button)
        val addCardButton = findViewById<Button>(R.id.add_card_button)
        val saveCardsButton = findViewById<Button>(R.id.save_cards_button)

        translateButton.setOnClickListener{
            translate()
        }
        addCardButton.setOnClickListener {
            addCard()
        }
        saveCardsButton.setOnClickListener {
            saveAllCards()
        }

        ankiAPI = AddContentApi(this)
    }

    private fun onItemCloseClick(position: Int) {
        flashcards.removeAt(position)
        adapter.notifyItemRemoved(position)
    }


    fun translate(){
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        var succeededTranslating: Boolean = false
        var translatedText: String = ""
        executor.execute {
            if (internetIsConnected()) {
                val response: Translation = translateService.translate(
                    inputText.text.toString(),
                    Translate.TranslateOption.sourceLanguage(sourceLanguage),
                    Translate.TranslateOption.targetLanguage(targetLanguage),
                    Translate.TranslateOption.model("nmt"),
                    Translate.TranslateOption.format("text")
                )
                succeededTranslating = true
                translatedText = response.translatedText
            }
            handler.post {
                if (succeededTranslating)
                    outputText.setText(translatedText)
                else {
                    Toast.makeText(
                        this,
                        getResources().getString(R.string.internet_connection_fail),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    }

    fun internetIsConnected(): Boolean {
        return try {
            val command = "ping -c 1 google.com"
            Runtime.getRuntime().exec(command).waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun addCard() {
        if(inputText.text.toString() != "" && outputText.text.toString() != "") {
            val card = Flashcard(inputText.text.toString(), outputText.text.toString())
            inputText.text = null
            outputText.text = null
            flashcards.add(card)
            adapter.notifyItemInserted(flashcards.size - 1)
            for(card in flashcards) {
                Log.i("word", card.word)
                Log.i("translation", card.translation)
            }
        }
    }

    fun saveAllCards() {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            val deckId: Long? = getOrGenerateDeckId()
            val modelId: Long? = getOrGenerateModelId()
            var failedApi = false

            if ((deckId == null) || (modelId == null)) {
                failedApi = true
                executor.shutdown()
            }
            var convertedCards = convertFlashcardsToStringArray()
            val filteredCards = handleDuplicateCards(convertedCards, modelId!!).toList()
            if (filteredCards.size != 0) {
                //val tag = mutableListOf<Set<String>>()
                for (card in filteredCards) {
                    //tag.add(listOf("Translation_to_Flashcard_App").toSet())
                    for (field in card)
                        Log.i("field", field)
                }
                Log.i("deckId", deckId.toString())
                Log.i("modelid", modelId.toString())
                //val converterForApi = KotlintoJavaApiConverter(ankiAPI)
                //converterForApi.addNotesWithJavaObjects(deckId!!, modelId, filteredCards)
                ankiAPI.addNotes(deckId!!, modelId, filteredCards, null)
            }
            handler.post{
                if(failedApi){
                    Toast.makeText(
                        this,
                        getResources().getString(R.string.card_add_fail),
                        Toast.LENGTH_LONG
                    ).show()
                }
                else {
                    clearArray()
                }
            }
        }
    }

    fun getOrGenerateDeckId(): Long? {
        var deckId: Long? = getDeckId(this.resources.getString(R.string.deck_name))
        if (deckId == null) {
            deckId = ankiAPI.addNewDeck(this.resources.getString(R.string.deck_name))
        }
        return deckId
    }

    fun getOrGenerateModelId(): Long?{
        var modelId: Long? = getModelId(this.resources.getString(R.string.model_name))
        if (modelId == null) {
            modelId = ankiAPI.addNewBasicModel(this.resources.getString(R.string.model_name))
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

    fun convertFlashcardsToStringArray(): MutableList<Array<String>>{
        val array = mutableListOf<Array<String>>()
        for(flashcard: Flashcard in flashcards){
            val element = arrayOf<String>(flashcard.word, flashcard.translation)
            array.add(element)
        }
        return array
    }

    fun clearArray(){
        val cardCount: Int = flashcards.size
        flashcards.clear()
        adapter.notifyItemRangeRemoved(0, cardCount)
    }
}
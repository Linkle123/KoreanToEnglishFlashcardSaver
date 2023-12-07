package com.example.koreantoenglishflashcardsaver

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.SparseArray
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation
import com.ichi2.anki.api.AddContentApi
import com.ichi2.anki.api.NoteInfo
import java.util.concurrent.Executors
import android.os.Handler
import android.util.Log
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.example.koreantoenglishflashcardsaver.model.*

class MainActivity : ComponentActivity() {
    lateinit var adapter: FlashCardAdapter

    lateinit var translateService: Translate
    val sourceLanguage: String = "ko"
    val targetLanguage: String = "en"
    lateinit var inputText: EditText
    lateinit var outputText: EditText

    lateinit var ankiAPI: AddContentApi
    lateinit var selectedDeckName: String

    private val databaseViewModel: DatabaseViewModel by viewModels {
        DatabaseViewModelFactory((application as FlashCardDeckApplication).database)
    }

    @SuppressLint("DirectDateInstantiation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)
        adapter = FlashCardAdapter(this) { position -> onItemCloseClick(position) }
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = adapter

        val selectedDeck = databaseViewModel.getSelectedDeck()
        if(selectedDeck != null)
            selectedDeckName = databaseViewModel.getSelectedDeck().deckName
        else {
            selectedDeckName = resources.getString(R.string.deck_name)
        }

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
        val deckChangeButton = findViewById<Button>(R.id.deck_change_button)
        val deckTitle = findViewById<TextView>(R.id.deck_name)
        deckTitle.text = selectedDeckName

        translateButton.setOnClickListener{
            translate()
        }
        addCardButton.setOnClickListener {
            addCard()
        }
        saveCardsButton.setOnClickListener {
            saveAllCards()
        }
        deckChangeButton.setOnClickListener {
            startDeckChangeActivity()
        }

        ankiAPI = AddContentApi(this)

        databaseViewModel.allFlashcards.observe(this, Observer {
            it?.let {
                adapter.submitList(it)
            }
        })
    }

    private fun onItemCloseClick(position: Int) {
        databaseViewModel.deleteByFlashcardId(position)
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
            databaseViewModel.insertFlashCard(card)
            adapter.notifyDataSetChanged()
        }
    }

    fun saveAllCards() {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            val deckId: Long? = getOrGenerateDeckId(selectedDeckName)
            val modelId: Long? = getOrGenerateModelId()
            var failedApi = false

            if ((deckId == null) || (modelId == null)) {
                failedApi = true
                executor.shutdown()
            }
            var convertedCards = convertFlashcardsToStringArray()
            val filteredCards = handleDuplicateCards(convertedCards, modelId!!).toList()
            if (filteredCards.size != 0) {
                ankiAPI.addNotes(modelId!!, deckId!!, filteredCards, null)
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

    fun getOrGenerateDeckId(deckName: String): Long? {
        var deckId: Long? = getDeckId(deckName)
        if (deckId == null) {
            deckId = generateDeck(deckName)
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

    fun generateDeck(deckName: String): Long? {
        val deckId = ankiAPI.addNewDeck(deckName)
        val deck = databaseViewModel.getDeckByName(deckName)

        if(deckId != null){
            databaseViewModel.insertDeck(Deck(deckName, deck.selected))
        }
        return deckId
    }

    fun generateMultipleDecks(decks: ArrayList<String>) {
        for(deck in decks) {
            val deckId = ankiAPI.addNewDeck(deck)
            if (deckId != null) {
                databaseViewModel.insertDeck(Deck(deck, false))
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


    val openDeckChangeActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK) {
            if(result.data != null) {
                val deckTitle = result.data!!.getStringExtra("deck_title")
                val newDecks = result.data!!.getStringArrayListExtra("new_decks")
                if(newDecks != null){
                    generateMultipleDecks(newDecks)
                }
                if(deckTitle != null){
                    updateSelectedDeck(deckTitle)
                }
            }
        }

    }
    fun updateSelectedDeck(deckName: String){
        selectedDeckName = deckName
        val oldSelectedDeck = databaseViewModel.getSelectedDeck()
        val newDeck = databaseViewModel.getDeckByName(deckName)

        oldSelectedDeck.selected = false
        newDeck.selected = true
        databaseViewModel.updateDecks(oldSelectedDeck, newDeck)
    }

    fun startDeckChangeActivity(){
        val decks = convertDecksToStringArray()
        val intent = Intent(this, DeckChangeActivity::class.java).apply{
            putExtra("deck_list", decks)
        }
        openDeckChangeActivity.launch(intent)
    }

}
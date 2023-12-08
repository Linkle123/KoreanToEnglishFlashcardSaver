package com.example.koreantoenglishflashcardsaver

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.koreantoenglishflashcardsaver.model.*
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    lateinit var adapter: FlashCardAdapter
    lateinit var deckTitle: TextView

    lateinit var translateService: Translate
    val sourceLanguage: String = "ko"
    val targetLanguage: String = "en"
    lateinit var inputText: EditText
    lateinit var outputText: EditText

    lateinit var ankiHelper: AnkiApi
    lateinit var selectedDeckName: String

    private val databaseViewModel: DatabaseViewModel by viewModels {
        DatabaseViewModelFactory((application as FlashCardDeckApplication).database)
    }
    lateinit var databaseHelper: DatabaseUtils

    @SuppressLint("DirectDateInstantiation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)
        adapter = FlashCardAdapter(this) { position, id -> databaseHelper.onItemCloseClick(position, id) }
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = adapter

        databaseHelper = DatabaseUtils(this, databaseViewModel, adapter)
        ankiHelper = AnkiApi(this, databaseHelper)

        selectedDeckName = databaseHelper.getSelectedDeckName()

        var accessKey: String? = null
        this.packageManager.getApplicationInfo(this.packageName, PackageManager.GET_META_DATA)
            .apply{
                accessKey = metaData.getString("com.google.android.translate.API_KEY")
            }

        if(accessKey != null) {
            translateService = TranslateOptions.newBuilder().setApiKey(accessKey).build().service
        }
        else{
            Toast.makeText(this, resources.getString(R.string.translation_api_fail), Toast.LENGTH_LONG).show()
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
        deckTitle = findViewById<TextView>(R.id.deck_name)
        deckTitle.text = selectedDeckName


        translateButton.setOnClickListener{
            val translatedText = translate(inputText.text.toString())
            outputText.setText(translatedText)
        }
        addCardButton.setOnClickListener {
            databaseHelper.addCard(inputText.text.toString(), outputText.text.toString())
            inputText.text.clear()
            outputText.text.clear()
        }
        saveCardsButton.setOnClickListener {
            ankiHelper.saveAllCards(selectedDeckName)
        }
        deckChangeButton.setOnClickListener {
            startDeckChangeActivity()
        }

        databaseHelper.connectRecyclerToData(this)
    }

    fun translate(word: String): String {
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
                if (succeededTranslating) {
                    outputText.setText(translatedText)
                }
                else{
                    Toast.makeText(
                        this,
                        resources.getString(R.string.internet_connection_fail),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        return  translatedText
    }

    fun internetIsConnected(): Boolean {
        return try {
            val command = "ping -c 1 google.com"
            Runtime.getRuntime().exec(command).waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun updateSelectedDeck(newDeckName: String){
        selectedDeckName = databaseHelper.updateSelectedDeck(newDeckName)
        deckTitle.setText(newDeckName)
    }

    val openDeckChangeActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK) {
            if(result.data != null) {
                val deckTitle = result.data!!.getStringExtra("deck_title")
                val newDecks = result.data!!.getStringArrayListExtra("new_decks")
                if(newDecks != null){
                    ankiHelper.generateMultipleDecksInAnki(newDecks)
                }
                if(deckTitle != null){
                    updateSelectedDeck(deckTitle)
                }
            }
        }

    }

    fun startDeckChangeActivity(){
        val decks = databaseHelper.convertDecksToStringArray()
        val intent = Intent(this, DeckChangeActivity::class.java).apply{
            putExtra("deck_list", decks)
        }
        openDeckChangeActivity.launch(intent)
    }

}
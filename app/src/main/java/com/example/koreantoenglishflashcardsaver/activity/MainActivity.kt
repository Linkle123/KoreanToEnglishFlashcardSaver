package com.example.koreantoenglishflashcardsaver.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.koreantoenglishflashcardsaver.AnkiApi
import com.example.koreantoenglishflashcardsaver.model.DatabaseUtils
import com.example.koreantoenglishflashcardsaver.R
import com.example.koreantoenglishflashcardsaver.model.*
import com.example.koreantoenglishflashcardsaver.translate.TranslateRepository
import com.example.koreantoenglishflashcardsaver.translate.TranslateViewModel
import com.example.koreantoenglishflashcardsaver.translate.WebViewRenderer
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    lateinit var adapter: FlashCardAdapter
    lateinit var deckTitle: TextView

    lateinit var inputText: EditText

    lateinit var ankiHelper: AnkiApi

    private val databaseViewModel: DatabaseViewModel by viewModels {
        DatabaseViewModelFactory((application as FlashCardDeckApplication).database)
    }
    lateinit var databaseHelper: DatabaseUtils

    @SuppressLint("DirectDateInstantiation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupChildLayout(R.layout.main_activity)

        adapter = FlashCardAdapter(this) { position, id -> databaseHelper.onItemCloseClick(position, id) }
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.reverseLayout = true
        linearLayoutManager.stackFromEnd = true
        recyclerView.setLayoutManager(linearLayoutManager)

        databaseHelper = DatabaseUtils(this, databaseViewModel, adapter)
        ankiHelper = AnkiApi(this, databaseHelper)
        val translateService = TranslateViewModel(TranslateRepository(WebViewRenderer.getInstance(this)))


        val selectedDeckName = databaseHelper.getSelectedDeckName()


        // Initialize the textfields to null
        inputText = findViewById(R.id.translate_text)
        inputText.text = null

        // Initialize buttons
        val translateButton = findViewById<Button>(R.id.translate_button)
        deckTitle = findViewById<TextView>(R.id.deck_name)
        deckTitle.text = selectedDeckName


        translateButton.setOnClickListener{
            translateService.translate(inputText.text.toString())
        }
        /*
        addCardButton.setOnClickListener {
            databaseHelper.addCard(inputText.text.toString(), mutableListOf(Pair("", arrayOf(""))), null)
            inputText.text.clear()
            outputText.text.clear()
        }
        saveCardsButton.setOnClickListener {
            ankiHelper.saveAllCards(deckTitle.text.toString())
        }
        deckChangeButton.setOnClickListener {
            startDeckChangeActivity()
        }
        // Opens the activity to edit the FlashCard
        adapter.onCardClick = {item ->

        }

         */

        // Asynchronously fetch data from the translateService
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                translateService.events.collect{ event ->
                    Log.i("Status update", "collected translation")
                    when (event){
                        is TranslateViewModel.TranslationEvent.Success -> {
                            startTranslationEditActivity(event.card)
                        }
                        is TranslateViewModel.TranslationEvent.ApiError -> {
                            Toast.makeText(baseContext, R.string.translation_api_fail, Toast.LENGTH_LONG).show()
                        }
                        is TranslateViewModel.TranslationEvent.ResultIncomplete -> {
                            Toast.makeText(baseContext, R.string.translation_result_fail, Toast.LENGTH_LONG).show()
                            startTranslationEditActivity(event.card)
                        }

                    }
                }
            }
        }

        databaseHelper.connectRecyclerToData(this)
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
        val selectedDeckName = databaseHelper.updateSelectedDeck(newDeckName)
        deckTitle.setText(newDeckName)
    }

    val openDeckChangeActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK) {
            if(result.data != null) {
                val newDeckTitle = result.data!!.getStringExtra("deck_title")
                val newDecks = result.data!!.getStringArrayListExtra("new_decks")
                if(newDecks != null){
                    //Log.i("newDeck", newDecks.get(0))
                    ankiHelper.generateMultipleDecksInAnki(newDecks)
                }
                if(newDeckTitle != null){
                    updateSelectedDeck(newDeckTitle)
                }
            }
        }
    }

    val openTranslationEditActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK) {
            if(result.data != null) {
                if(intent.getSerializableExtra("flashcard") != null) {
                    val flashcard = (intent.getSerializableExtra("flashcard") as Flashcard?)!!
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

    fun startTranslationEditActivity(flashcard: Flashcard){
        Log.i("Launching translation edit activity:", "launched")
        val intent = Intent(this, TranslationEditActivity::class.java).apply{
            putExtra("flashcard", flashcard)
        }
        Log.i("Got Card", flashcard.word)
        flashcard.getTranslationsAsString()?.let { Log.i("Got Card", it) }
        flashcard.getExamplesAsString()?.let { Log.i("Got Card", it) }
        flashcard.directTranslation?.let { Log.i("Got Card", it) }

        openTranslationEditActivity.launch(intent)
    }

}
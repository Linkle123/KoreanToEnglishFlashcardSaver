package com.example.koreantoenglishflashcardsaver.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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



    @SuppressLint("DirectDateInstantiation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupChildLayout(R.layout.main_activity)

        adapter = FlashCardAdapter(this, onCloseClick= {position, id -> databaseHelper.onItemCloseClick(position, id) }, onCardClick = {flashcard, position -> startTranslationEditActivity(flashcard, position) } )
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.reverseLayout = true
        linearLayoutManager.stackFromEnd = true
        recyclerView.setLayoutManager(linearLayoutManager)

        databaseHelper.connectAdapter(adapter)

        val translateService = TranslateViewModel(TranslateRepository(WebViewRenderer.getInstance(this)))


        val selectedDeckName = databaseHelper.getSelectedDeckName()


        // Initialize the textfields to null
        inputText = findViewById(R.id.translate_text)
        inputText.text = null

        // Initialize buttons
        val translateButton = findViewById<Button>(R.id.translate_button)
        deckTitle = findViewById<TextView>(R.id.deck_name)
        val saveCardsButton = findViewById<ImageButton>(R.id.save_all_cards_button)
        deckTitle.text = selectedDeckName


        translateButton.setOnClickListener{
            translateService.translate(inputText.text.toString())
            inputText.text.clear()
        }

        saveCardsButton.setOnClickListener {
            ankiHelper.saveAllCards(deckTitle.text.toString())
        }

        // Asynchronously fetch data from the translateService
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                translateService.events.collect{ event ->
                    Log.i("Status update", "collected translation")
                    when (event){
                        is TranslateViewModel.TranslationEvent.Success -> {
                            databaseHelper.addCard(event.card.word, event.card.translations, event.card.examples, event.card.directTranslation)
                        }
                        is TranslateViewModel.TranslationEvent.ApiError -> {
                            Toast.makeText(baseContext, R.string.translation_api_fail, Toast.LENGTH_LONG).show()
                        }
                        is TranslateViewModel.TranslationEvent.ResultIncomplete -> {
                            Toast.makeText(baseContext, R.string.translation_result_fail, Toast.LENGTH_LONG).show()
                            databaseHelper.addCard(event.card.word, event.card.translations, event.card.examples, event.card.directTranslation)
                        }

                    }
                }
            }
        }

        databaseHelper.connectRecyclerToData(this)
    }

    override fun updateSelectedDeck(newDeckName: String){
        databaseHelper.updateSelectedDeck(newDeckName)
        deckTitle.setText(newDeckName)
    }




    val openTranslationEditActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK) {
            if(result.data != null) {
                if(result.data!!.getParcelableExtra<Flashcard>("flashcard") != null) {
                    val flashcard = (result.data!!.getParcelableExtra("flashcard") as Flashcard?)!!
                    val position = result.data!!.getIntExtra("position", 0)
                    Log.i("Got Card Back", flashcard.word)
                    flashcard.getTranslationsAsString()?.let { Log.i("Got Card Back", it) }
                    flashcard.getExamplesAsString()?.let { Log.i("Got Card Back", it) }
                    flashcard.directTranslation?.let { Log.i("Got Card Back", it) }
                    Log.i("Got Card Back", position.toString())
                    Log.i("item id", flashcard.id.toString())

                    databaseHelper.updateCard(flashcard, position)
                }
            }
        }
    }

    fun startTranslationEditActivity(flashcard: Flashcard, position: Int){
        Log.i("Launching translation edit activity:", "launched")
        val intent = Intent(this, TranslationEditActivity::class.java).apply{
            putExtra("flashcard", flashcard)
            putExtra("position", position)
        }
        Log.i("Found Card", flashcard.word)
        flashcard.getTranslationsAsString()?.let { Log.i("Found Card", it) }
        flashcard.getExamplesAsString()?.let { Log.i("Found Card", it) }
        flashcard.directTranslation?.let { Log.i("Found Card", it) }
        Log.i("Found Card", position.toString())
        Log.i("item id", flashcard.id.toString())
        openTranslationEditActivity.launch(intent)
    }

}
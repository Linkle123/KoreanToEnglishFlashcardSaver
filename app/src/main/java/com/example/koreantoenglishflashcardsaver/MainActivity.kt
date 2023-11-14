package com.example.koreantoenglishflashcardsaver

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.compose.ui.res.stringResource
import androidx.recyclerview.widget.RecyclerView
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation
import com.ichi2.anki.api.AddContentApi

class MainActivity : ComponentActivity() {
    lateinit var adapter: FlashCardAdapter
    lateinit var flashcards: ArrayList<Flashcard>
    var connected: Boolean = false
    lateinit var translate: Translate
    val sourceLanguage: String = "ko"
    val targetLanguage: String = "en"
    lateinit var inputText: EditText
    lateinit var outputText: EditText

    lateinit var ankiAPI: AddContentApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = FlashCardAdapter(flashcards) { position -> onItemCloseClick(position) }
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = adapter
        translate = TranslateOptions.getDefaultInstance().service

        // Initialize the textfields to null
        inputText = findViewById(R.id.translate_text)
        outputText = findViewById(R.id.translated_text)
        inputText.text = null
        outputText.text = null

        // Initialize buttons
        val translatebutton = findViewById<Button>(R.id.translate_button)
        val addcardbutton = findViewById<Button>(R.id.add_card_button)
        val savecardsbutton = findViewById<Button>(R.id.save_cards_button)

        translatebutton.setOnClickListener{
            translate()
        }
        addcardbutton.setOnClickListener {
            addCard()
        }

        ankiAPI = AddContentApi(this)
    }

    private fun onItemCloseClick(position: Int) {
        flashcards.removeAt(position)
        adapter.notifyItemRemoved(position)
    }


    fun translate(){
        val response: Translation = translate.translate(
            inputText.text.toString(),
            Translate.TranslateOption.sourceLanguage(sourceLanguage),
            Translate.TranslateOption.targetLanguage(targetLanguage),
            Translate.TranslateOption.model("nmt"),
            Translate.TranslateOption.format("text")
        )
        outputText.setText(response.translatedText)
    }

    fun checkInternetConnection(): Boolean {
        //Check internet connection:
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

        //Means that we are connected to a network (mobile or wi-fi)
        connected =
            connectivityManager!!.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state == NetworkInfo.State.CONNECTED ||
                    connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state == NetworkInfo.State.CONNECTED
        return connected
    }

    fun addCard() {
        if(inputText.text != null && outputText.text != null) {
            val card = Flashcard(inputText.text.toString(), outputText.text.toString())
            inputText.text = null
            outputText.text = null
            flashcards.add(card)
            adapter.notifyItemInserted(flashcards.size - 1);
        }
    }

    fun saveAllCards() {

    }

    private fun getOrGenerateDeckId(): Long {
        var did: Long? = getDeckId(this.resources.getString(R.string.deck_name))
        if (did == null) {
            did = ankiAPI.addNewDeck(this.resources.getString(R.string.deck_name))
        }
        return did!!
    }

    private fun getDeckId(deckName: String): Long? {
        val deckList: Map<Long, String> = ankiAPI.getDeckList()
        if (deckList != null) {
            for ((key, value) in deckList) {
                if (value.equals(deckName, ignoreCase = true)) {
                    return key
                }
            }
        }
        return null
    }
}
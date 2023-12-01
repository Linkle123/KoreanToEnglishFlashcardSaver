package com.example.koreantoenglishflashcardsaver

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.ComponentActivity

class DeckChangeActivity : ComponentActivity() {
    var newDecks = ArrayList<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.deck_change_popup_activity)

        val addButton = findViewById<Button>(R.id.add_new_deck_button)
        val saveButton = findViewById<Button>(R.id.save_deck_change_button)
        val cancelButton = findViewById<Button>(R.id.cancel_button)
        val deckNew = findViewById<EditText>(R.id.deck_new_input)
        val deckChosen = findViewById<Spinner>(R.id.deck_change_input)


        saveButton.setOnClickListener {
            val sendIntent = Intent(this, MainActivity::class.java)
            sendIntent.putExtra("deck_title", deckNew.text.toString())
            sendIntent.putExtra("new_decks", newDecks)
            setResult(RESULT_OK, sendIntent)
            finish()
        }

        cancelButton.setOnClickListener {
            val sendIntent = Intent(this, MainActivity::class.java)
            setResult(RESULT_CANCELED, sendIntent)
            finish()
        }
        
        addButton.setOnClickListener { 
            addNewDeck(deckNew.text.toString())
        }
    }

    private fun addNewDeck(deck: String) {

    }

}
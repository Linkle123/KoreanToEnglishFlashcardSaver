package com.example.koreantoenglishflashcardsaver

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.ComponentActivity

class DeckChangeActivity : ComponentActivity() {
    var newDecks = ArrayList<String>()
    lateinit var savedDecks: MutableList<String>
    lateinit var adapter: ArrayAdapter<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.deck_change_popup_activity)
        val intent = intent
        if(intent.getStringArrayListExtra("deck_list") != null) {
            savedDecks = intent.getStringArrayListExtra("deck_list")!!.toMutableList()
        }
        else{
            savedDecks = mutableListOf()
        }

        val addButton = findViewById<Button>(R.id.add_new_deck_button)
        val saveButton = findViewById<Button>(R.id.save_deck_change_button)
        val cancelButton = findViewById<Button>(R.id.cancel_button)
        val deckNew = findViewById<EditText>(R.id.deck_new_input)

        val deckChosen = findViewById<Spinner>(R.id.deck_change_input)
        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, savedDecks)
        deckChosen.adapter = adapter

        saveButton.setOnClickListener {
            val sendIntent = Intent(this, MainActivity::class.java)
            sendIntent.putExtra("deck_title", deckChosen.selectedItem.toString())
            sendIntent.putStringArrayListExtra("new_decks", newDecks)
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
            deckNew.text.clear()
        }
    }

    private fun addNewDeck(deck: String) {
        if (deck !in savedDecks && deck !in newDecks) {
            newDecks.add(deck)
            adapter.add(deck)
            Toast.makeText(
                this,
                getResources().getString(R.string.deck_add_success),
                Toast.LENGTH_LONG
            ).show()
        }
        else{
            Toast.makeText(
                this,
                getResources().getString(R.string.duplicate_deck_add_attempt),
                Toast.LENGTH_LONG
            ).show()
        }
    }

}
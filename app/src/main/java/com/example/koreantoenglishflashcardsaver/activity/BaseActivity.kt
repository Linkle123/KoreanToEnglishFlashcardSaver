package com.example.koreantoenglishflashcardsaver.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.koreantoenglishflashcardsaver.AnkiApi
import com.example.koreantoenglishflashcardsaver.R
import com.example.koreantoenglishflashcardsaver.model.DatabaseUtils
import com.example.koreantoenglishflashcardsaver.model.DatabaseViewModel
import com.example.koreantoenglishflashcardsaver.model.DatabaseViewModelFactory
import com.example.koreantoenglishflashcardsaver.model.FlashCardDeckApplication
import com.example.koreantoenglishflashcardsaver.translate.TranslateRepository
import com.example.koreantoenglishflashcardsaver.translate.TranslateViewModel
import com.example.koreantoenglishflashcardsaver.translate.WebViewRenderer

open class BaseActivity: AppCompatActivity() {

    lateinit var ankiHelper: AnkiApi

    private val databaseViewModel: DatabaseViewModel by viewModels {
        DatabaseViewModelFactory((application as FlashCardDeckApplication).database)
    }
    lateinit var databaseHelper: DatabaseUtils

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection.
        return when (item.itemId) {
            R.id.home_activity -> {
                if(this !is MainActivity){
                    startMainActivity()
                }
                true
            }
            R.id.deck_change_activity -> {
                if(this !is DeckChangeActivity) {
                    startDeckChangeActivity()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    /**
     * Starts the Deck Change Activity
     */
    fun startDeckChangeActivity(){
        Log.i("Starting Activity", "Deck change activity")
        val decks = databaseHelper.convertDecksToStringArray()
        val intent = Intent(this, DeckChangeActivity::class.java).apply{
            putExtra("deck_list", decks)
        }
        Log.i("Finished stuffing", "intents")
        openDeckChangeActivity.launch(intent)
    }

    /**
     * Starts the Main Activity
     */
    fun startMainActivity(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    /**
     * empty base function to override if the activity requires it
     */
    open fun updateSelectedDeck(string: String){}

    // Helper function for children to provide their layout
    protected fun setupChildLayout(layoutResId: Int) {
        setContentView(R.layout.base_activity)
        val button: ImageButton = findViewById(R.id.dropdown_menu)
        button.setOnClickListener { view ->
            val wrapper = ContextThemeWrapper(this, R.style.MenuButtonStyle)
            val popup = PopupMenu(wrapper, view) // 'view' is the anchor for the popup
            popup.menuInflater.inflate(R.menu.options_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                onOptionsItemSelected(item)
            }

            popup.show()
        }
        // Find the toolbar and set it as the ActionBar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Inflate the child layout into the FrameLayout
        val contentFrame = findViewById<FrameLayout>(R.id.content_frame)
        setupWebView(contentFrame)
        layoutInflater.inflate(layoutResId, contentFrame, true)

        databaseHelper = DatabaseUtils(this, databaseViewModel)
        ankiHelper = AnkiApi(this, databaseHelper)
    }

    fun setupWebView(layout: FrameLayout){
        val webViewRenderer = WebViewRenderer.getInstance(this)
        val translateRepository = TranslateRepository(webViewRenderer)

        // Re-initialize or attach the singleton's webView to this activity's window
        val wv = webViewRenderer.getWebView()
        (wv.parent as? ViewGroup)?.removeView(wv)

        // Add the webview to the activity layout to "wake it up"
        layout.addView(wv, FrameLayout.LayoutParams(1, 1)) // Tiny, invisible but "active"
    }
}
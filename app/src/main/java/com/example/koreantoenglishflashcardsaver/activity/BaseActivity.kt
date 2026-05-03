package com.example.koreantoenglishflashcardsaver.activity

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.koreantoenglishflashcardsaver.R
import com.example.koreantoenglishflashcardsaver.translate.TranslateRepository
import com.example.koreantoenglishflashcardsaver.translate.TranslateViewModel
import com.example.koreantoenglishflashcardsaver.translate.WebViewRenderer

open class BaseActivity: AppCompatActivity() {


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection.
        // Todo: Update this to actually go to the activities specified
        return when (item.itemId) {
            R.id.home_activity -> {
                Toast.makeText(
                    baseContext,
                    "clicked home activity",
                    Toast.LENGTH_LONG
                ).show()
                true
            }
            R.id.deck_change_activity -> {
                Toast.makeText(
                    baseContext,
                    "clicked deck change activity",
                    Toast.LENGTH_LONG
                ).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


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
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

    }
}
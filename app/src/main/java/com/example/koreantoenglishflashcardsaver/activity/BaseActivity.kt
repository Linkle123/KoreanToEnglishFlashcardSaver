package com.example.koreantoenglishflashcardsaver.activity

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.FrameLayout

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.koreantoenglishflashcardsaver.R

open class BaseActivity: AppCompatActivity() {
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.options_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection.
        // Todo: Update this to actually go to the activities specified
        return when (item.itemId) {
            R.id.home_activity -> {
                true
            }
            R.id.deck_change_activity -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    // Helper function for children to provide their layout
    protected fun setupChildLayout(layoutResId: Int) {
        setContentView(R.layout.base_activity)

        // Find the toolbar and set it as the ActionBar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Inflate the child layout into the FrameLayout
        val contentFrame = findViewById<FrameLayout>(R.id.content_frame)
        layoutInflater.inflate(layoutResId, contentFrame, true)
    }
}
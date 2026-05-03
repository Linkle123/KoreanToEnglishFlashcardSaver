package com.example.koreantoenglishflashcardsaver.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.example.koreantoenglishflashcardsaver.R
import com.example.koreantoenglishflashcardsaver.model.Flashcard

class TranslationEditActivity: Activity() {
    lateinit var flashcard: Flashcard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.translation_edit_popup_activity)
        if(intent.getSerializableExtra("flashcard") != null) {
            flashcard = (intent.getSerializableExtra("flashcard") as Flashcard?)!!
            Log.i("Got Card", flashcard.word)
            flashcard.getTranslationsAsString()?.let { Log.i("Got Card", it) }
            flashcard.getExamplesAsString()?.let { Log.i("Got Card", it) }
            flashcard.directTranslation?.let { Log.i("Got Card", it) }
        }
        else{
            flashcard = Flashcard()
            Log.i("Didn't Get Card", flashcard.word)
        }


        configureDisplayParameters()
        fillPage()

        val saveButton = findViewById<Button>(R.id.add_card_button)
        saveButton.setOnClickListener {
            val sendIntent = Intent(this, MainActivity::class.java)
            getAllTranslations(findViewById<LinearLayout>(R.id.translation_entry_container))
            sendIntent.putExtra("flashcard", flashcard)
            setResult(RESULT_OK, sendIntent)
            finish()
        }
    }

    /**
     * Configures the window to show up as a popup window
     */
    fun configureDisplayParameters(){
        val dm: DisplayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        window.setLayout((dm.widthPixels*.8).toInt(), (dm.heightPixels*.8).toInt())
        val params = window.attributes
        params.gravity = Gravity.CENTER
        params.x = 0
        params.y = 0
        window.attributes = params
    }

    fun getAllTranslations(root: View){
        val wordTranslationsLayout = root.findViewWithTag<ConstraintLayout>(resources.getString(R.string.word_translations_title))
        flashcard.translations = getMemberData(wordTranslationsLayout)
        val examplesLayout = root.findViewWithTag<ConstraintLayout>(resources.getString(R.string.examples_title))
        flashcard.examples = getMemberData(examplesLayout).mapNotNull{
                (sentence, definition) ->
            if (definition.size == 1) sentence to definition[0]
            else null}.toMutableList()
        val fullTranslationLayout = root.findViewWithTag<ConstraintLayout>(resources.getString(R.string.full_translation_title))
        val fullTranslation = getMemberData(fullTranslationLayout)
        flashcard.directTranslation = if (fullTranslation.size != 1 || fullTranslation[0].second.size != 1) null else fullTranslation[0].second[0]
    }
    /**
     * Gets all of the selected items as a MutableList
     */
    fun getMemberData(firstLevel: ConstraintLayout): MutableList<Pair<String, Array<String>>> {
        return firstLevel.children
            .filterIsInstance<ConstraintLayout>()
            .filter { it.isSelected } // NEW: Only process 2nd-level layouts that are selected
            .mapNotNull { secondLevel ->
                // 1. Grab the header title from the 2nd level
                val headerText = secondLevel.findViewById<TextView>(R.id.translation_entry)?.text?.toString()
                if (headerText == null || headerText == "") return@mapNotNull null
                // 2. Find the 3rd-level container inside this 2nd level
                val itemsContainer = secondLevel.findViewById<LinearLayout>(R.id.subItemContainer)
                if (itemsContainer == null || itemsContainer.children.none()) return@mapNotNull null

                // 3. Filter and map the 3rd-level children
                val selectedItems = itemsContainer.children
                    .filter { it.isSelected } // Only keep selected 3rd-level items
                    .mapNotNull { it.findViewById<TextView>(R.id.subTextView)?.text?.toString() }
                    .toList().toTypedArray()

                headerText to selectedItems
            }
            .toMutableList()
    }


    /**
     * Populates the screen with the data from the global flashcard variable
     */
    fun fillPage(){
        val progenitorContainer = findViewById<LinearLayout>(R.id.translation_entry_container)
        fillPageSection(progenitorContainer, flashcard.translations, resources.getString(R.string.word_translations_title))
        fillPageSection(progenitorContainer, flashcard.getExamplesAsArray(), resources.getString(R.string.examples_title))
        fillPageSection(progenitorContainer, flashcard.getDirectTranslationAsArray(), resources.getString(R.string.full_translation_title))
    }

    /**
     * Fills the provided linearlayout with the provided items.
     * If items is null or empty, it instead hides both the linearlayout and provided title.
     * The add button opens up an AlertDialog which lets the user insert their own translations,
     * and the selectAllButton lets a user tap once to select all of the members of one section
     */
    fun fillPageSection(progenitorContainer: LinearLayout, items: MutableList<Pair<String, Array<String>>>?, title: String){
        val layout = layoutInflater.inflate(R.layout.translation_entry_group_layout, progenitorContainer, false)
        layout.tag = title
        val body = layout.findViewById<LinearLayout>(R.id.translation_entry_body)
        val titleView = layout.findViewById<TextView>(R.id.translation_entry_title)
        val addButton = layout.findViewById<ImageButton>(R.id.add_translation_entry_button)
        val selectAllButton = layout.findViewById<CheckBox>(R.id.select_all_button)
        titleView.text = title
        if (items == null || items.isEmpty() ){
            body.visibility = View.GONE
        }
        else{
            inflateTranslationCategories(body, items, title != resources.getString(R.string.full_translation_title))
        }
        // Adds a custom translation input by the user. Changes the behavior based on which one it is.
        addButton.setOnClickListener {
            when(title){
                resources.getString(R.string.word_translations_title) -> showAddDialog(true) { (word, definitions) ->
                    inflateTranslationCategories(body, mutableListOf(Pair(word, definitions)), title != resources.getString(R.string.full_translation_title))
                    body.visibility = View.VISIBLE
                }
                resources.getString(R.string.examples_title) -> showAddDialog(false) { (word, definitions) ->
                    inflateTranslationCategories(body, mutableListOf(Pair(word, definitions)), title != resources.getString(R.string.full_translation_title))
                    body.visibility = View.VISIBLE
                }
                resources.getString(R.string.full_translation_title) -> if(body.children.none()) {
                    showAddDialog(false, flashcard.word) { (word, definitions) ->
                        inflateTranslationCategories(body, mutableListOf(Pair(word, definitions)), title != resources.getString(R.string.full_translation_title))
                        body.visibility = View.VISIBLE
                    }
                }
                else{
                        Toast.makeText(
                            baseContext,
                            resources.getString(R.string.full_translation_full_response),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }

        }
        selectAllButton.setOnCheckedChangeListener { _, isChecked ->
            body.children.forEach {
                updateSelection(it, body, isChecked)
            }
        }
        progenitorContainer.addView(layout)
    }
    /**
     * Dynamically fills the given LinearLayout with translations using the items given.
     * Each Pair represents a word and a collection of potential translations, which the user
     * can select or unselect to save to their flashcard.
     */
    fun inflateTranslationCategories(container: LinearLayout, items: MutableList<Pair<String, Array<String>>>, isEditable: Boolean){
        items.forEach { (word, definition) ->
            val itemView = layoutInflater.inflate(R.layout.translation_entry_layout, container, false)
            val textView = itemView.findViewById<TextView>(R.id.translation_entry)
            val subContainer = itemView.findViewById<LinearLayout>(R.id.subItemContainer)
            configureChildren(
                itemView = itemView,
                textView = textView,
                container = container,
                isEditable = isEditable,
                item = word
                ){
                itemView.isSelected = !itemView.isSelected
                updateSelection(itemView, subContainer, itemView.isSelected)
            }

            definition.forEach { childText ->
                val childView = layoutInflater.inflate(R.layout.translation_entry_subitem, subContainer, false)
                val childTextView = childView.findViewById<TextView>(R.id.subTextView)

                configureChildren(
                    itemView = childView,
                    textView = childTextView,
                    container = subContainer,
                    isEditable = false,
                    item = childText
                ){
                    childView.isSelected = !childView.isSelected
                    // In the case that deselecting a child makes it so every child is deselected, deselect the parent as well and disable all children
                    if(subContainer.children.all {!it.isSelected}){
                        updateSelection(itemView, subContainer, false)
                    }
                    updateVisuals(childView)
                }
            }
        }
    }

    /**
     * Configures the children views and adds them to the provided container object.
     * Sets the onClickListener based on the function provided.
     * Sets the onLongClick behavior to pop up a dialog to edit the text in the textview
     * Sets the text of textView to the item parameter
     */
    private fun configureChildren(itemView: View, textView: TextView, container: LinearLayout, isEditable: Boolean,
                                     item: String, onClick: () -> Unit){
        // All are selected by default
        itemView.isSelected = true
        updateVisuals(itemView)

        // CLICK: Toggle "On/Off" visual indication
        itemView.setOnClickListener {
            onClick()
        }
        if(isEditable) {
            // LONG CLICK: Edit text (via simple AlertDialog)
            itemView.setOnLongClickListener {
                showEditDialog(textView)
                it.isSelected = true
                updateVisuals(it)
                true // consumes the long click
            }
        }
        textView.text = item
        container.addView(itemView)
    }

    private fun updateVisuals(view: View) {
        view.setBackgroundResource(if (view.isSelected) R.drawable.basic_menu_button else R.drawable.unselected_menu_button)
    }

    /**
     * Loop through all children in this parent's subContainer and auto select or
     * deselect all according to the selected parameter
     */
    private fun updateSelection(view: View, container: LinearLayout, selected: Boolean){
        view.isSelected = selected
        container.children.forEach {
            it.isEnabled = selected
            it.isSelected = selected
            updateVisuals(it)
        }
        updateVisuals(view)
    }

    /**
     * Creates an AlertDialog to let the user edit the text of the textView provided
     */
    private fun showEditDialog(view: TextView){
        val input = EditText(this)
        input.hint = "Enter text here"
        input.setText(view.text)

        val builder = AlertDialog.Builder(this, R.style.Alert)
        builder.setTitle("Edit Translation")
        builder.setView(input)

        // 3. Handle buttons
        builder.setPositiveButton("OK") { dialog, _ ->
            view.text = input.text.toString()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    /**
     * Creates an AlertDialog to let the user to add their own members
     */
    @SuppressLint("CutPasteId")
    private fun showAddDialog(addMultiples: Boolean, defaultWord: String? = null, onResult: (Pair<String, Array<String>>) -> Unit){
        val builder = AlertDialog.Builder(this, R.style.Alert)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_translation_entry, null)

        val container = dialogView.findViewById<LinearLayout>(R.id.temporary_translations_container)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAddTranslation)
        val editEntry = dialogView.findViewById<EditText>(R.id.edit_translation_entry_name)
        if(defaultWord != null){
            editEntry.isEnabled = false
            editEntry.setText(defaultWord)
        }

        // Helper function to add a clothing row
        fun addEntryRow(initialText: String = "") {
            val row = layoutInflater.inflate(R.layout.dialog_add_translation_entry_subitem, container, false)
            val edit = row.findViewById<EditText>(R.id.edit_translation_entry_body)
            val btnRemove = row.findViewById<ImageButton>(R.id.btnRemove)

            edit.setText(initialText)
            btnRemove.setOnClickListener { container.removeView(row) }

            container.addView(row)
        }

        // Add one row by default, and allow the btn to be visible if specified by param addMultiples
        addEntryRow()
        if(addMultiples) {
            btnAdd.setOnClickListener { addEntryRow() }
        }
        else{
            btnAdd.visibility = View.GONE
        }

        builder.setView(dialogView)
            .setTitle("Create Entry")
            .setPositiveButton("Save") { _, _ ->
                val word = editEntry.text.toString()

                // Gather all translations from the dynamic rows
                val entries = mutableListOf<String>()
                for (i in 0 until container.childCount) {
                    val row = container.getChildAt(i)
                    val entryName = row.findViewById<EditText>(R.id.edit_translation_entry_body).text.toString()
                    if (entryName.isNotBlank()) entries.add(entryName)
                }

                onResult(Pair(word, entries.toTypedArray()))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
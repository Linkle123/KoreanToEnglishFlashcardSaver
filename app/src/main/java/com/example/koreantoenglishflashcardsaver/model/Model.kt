package com.example.koreantoenglishflashcardsaver.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "flashcards")
data class Flashcard(
    var word: String,
    var translation: String) : Serializable{
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}

@Entity(tableName = "decks")
data class Deck(
    var deckName: String,
    var selected: Boolean) : Serializable{
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}


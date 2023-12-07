package com.example.koreantoenglishflashcardsaver.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    @Query("SELECT * FROM decks ORDER BY deckName ASC")
    fun getAlphabetizedDecks(): List<Deck>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(deck: Deck)

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: Int)

    @Query("SELECT * FROM decks WHERE selected = :selected LIMIT 1")
    fun getSelectedDeck(selected: Boolean = true): Deck

    @Query("SELECT * FROM decks WHERE deckName = :deckName")
    fun getDeckByName(deckName: String): Deck

    @Query("DELETE FROM flashcards")
    suspend fun deleteAll()

    @Update
    fun updateDecks(vararg deck: Deck)
}
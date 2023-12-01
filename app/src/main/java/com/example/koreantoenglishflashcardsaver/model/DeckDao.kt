package com.example.koreantoenglishflashcardsaver.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    @Query("SELECT * FROM decks ORDER BY deckName ASC")
    fun getAlphabetizedDecks(): Flow<List<Deck>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(deck: Deck)

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteByDeckId(deckId: Int)

    @Query("DELETE FROM flashcards")
    suspend fun deleteAll()

    @Update
    fun updateDecks(deck: Deck)
}
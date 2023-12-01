package com.example.koreantoenglishflashcardsaver.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {

    @Query("SELECT * FROM flashcards ORDER BY id ASC")
    fun getAlphabetizedFlashCardByTranslation(): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(journal: Flashcard)

    @Query("DELETE FROM flashcards WHERE id = :flashcardId")
    suspend fun deleteByFlashcardId(flashcardId: Int)

    @Query("DELETE FROM flashcards")
    suspend fun deleteAll()

    @Update
    fun updateFlashcards(flashcard: Flashcard)
}
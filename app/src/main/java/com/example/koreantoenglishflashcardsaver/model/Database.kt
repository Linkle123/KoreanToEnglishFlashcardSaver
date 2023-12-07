package com.example.koreantoenglishflashcardsaver.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.koreantoenglishflashcardsaver.R
import kotlinx.coroutines.*

@Database(entities = arrayOf(Deck::class, Flashcard::class), version = 1, exportSchema = false)
public abstract class AppRoomDatabase : RoomDatabase() {

    abstract fun flashcardDao(): FlashcardDao

    abstract fun deckDao(): DeckDao

    private class FlashcardDatabaseCallback(
        private val scope: CoroutineScope,
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database: AppRoomDatabase ->
                scope.launch {
                    populateDatabase(database.flashcardDao(), database.deckDao())
                }
            }
        }
        suspend fun populateDatabase(flashcardDao: FlashcardDao, deckDao: DeckDao) {
            // Delete all content here.
            flashcardDao.deleteAll()
            deckDao.deleteAll()

            val deck = Deck(this.context.resources.getString(R.string.deck_name), true)
            deckDao.insert(deck)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppRoomDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppRoomDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppRoomDatabase::class.java,
                    "flashcarddeck_database"
                )
                    .allowMainThreadQueries()
                    .addCallback(FlashcardDatabaseCallback(scope, context))
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }


    }
}
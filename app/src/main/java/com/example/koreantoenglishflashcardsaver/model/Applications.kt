package com.example.koreantoenglishflashcardsaver.model

import android.app.Application
import kotlinx.coroutines.*

class FlashCardDeckApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())
    val database by lazy { AppRoomDatabase.getDatabase(this, applicationScope) }
}
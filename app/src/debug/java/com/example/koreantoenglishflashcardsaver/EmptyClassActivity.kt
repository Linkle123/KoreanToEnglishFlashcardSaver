package com.example.koreantoenglishflashcardsaver

import android.os.Bundle
import com.example.koreantoenglishflashcardsaver.activity.BaseActivity

// This is just a blank container for the WebView
class EmptyTestActivity : BaseActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupChildLayout(R.layout.main_activity)
    }
}
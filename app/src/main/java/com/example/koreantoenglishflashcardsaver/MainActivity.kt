package com.example.koreantoenglishflashcardsaver

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation


class MainActivity : ComponentActivity() {
    lateinit var adapter: FlashCardAdapter
    var connected: Boolean = false
    lateinit var translate: Translate
    val targetLanguage: String = "Korean"
    lateinit var inputText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = FlashCardAdapter(this)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = adapter
        translate = TranslateOptions.getDefaultInstance().service
        inputText = findViewById(R.id.translate_text)
    }


    fun translate() {
        val translation: Translation = translate.translate(
            inputText.text.toString(),
            Translate.TranslateOption.sourceLanguage("ko"),
            Translate.TranslateOption.targetLanguage("en"),
            Translate.TranslateOption.model("nmt"),
            Translate.TranslateOption.format("text")
        )
    }

    fun checkInternetConnection(): Boolean {
        //Check internet connection:
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

        //Means that we are connected to a network (mobile or wi-fi)
        connected =
            connectivityManager!!.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state == NetworkInfo.State.CONNECTED ||
                    connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state == NetworkInfo.State.CONNECTED
        return connected
    }
}
package com.example.koreantoenglishflashcardsaver

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.collection.LruCache
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.jsoup.Jsoup
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.example.koreantoenglishflashcardsaver.model.Flashcard
import com.google.cloud.Tuple
import org.jsoup.nodes.Document

class TranslateApi(contextApp: Context){
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: TranslateApi? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TranslateApi(context).also {
                    INSTANCE = it
                }
            }
    }
    val imageLoader: ImageLoader by lazy {
        ImageLoader(requestQueue,
            object : ImageLoader.ImageCache {
                private val cache = LruCache<String, Bitmap>(20)
                override fun getBitmap(url: String): Bitmap? {
                    return cache.get(url)
                }
                override fun putBitmap(url: String, bitmap: Bitmap) {
                    cache.put(url, bitmap)
                }
            })
    }
    val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }
    val context: Context
    init {
        context = contextApp
    }

    /*
        Connects to the internet to scrape the Naver Korean-English Dictionary for results for the given word.
        If the result is found, it returns a Flashcard object with the base word, variations of the words with
        translations, and example sentences and meanings.
        If the result is not found, it returns a Flashcard object with the base word, the result of
        a direct translation, and no example sentences.
     */
    suspend fun translate(word: String): Flashcard?{
        val html = getData(context.resources.getString(R.string.naver_dic_url) + word)
        if(html != null) {
            // First check if the search result found anything.
            val doc = Jsoup.parse(html)
            val notFound = doc.getElementsByClass("section_empty").isEmpty()
            if (notFound){
                val translation = parseHTMLNotFound(doc)
                Toast.makeText(context, context.resources.getString(R.string.translation_result_fail), Toast.LENGTH_LONG).show()
                if (translation != null) {
                    return Flashcard(word, mutableListOf(Pair(word, arrayOf(translation))), null)
                }
                else return null
            }
            else {
                val (translation, examples) = parseHTMLFound(doc)
                return Flashcard(word, translation, examples)
            }
        }
        else{
            Toast.makeText(context, context.resources.getString(R.string.translation_api_fail), Toast.LENGTH_LONG).show()
            return null
        }
    }

    // Source - https://stackoverflow.com/a/60246972
    // Posted by Animesh Sahu, modified by community. See post 'Timeline' for change history
    // Retrieved 2026-03-23, License - CC BY-SA 4.0
    suspend fun getData(url: String) = suspendCoroutine<String?> { cont ->
        val queue = Volley.newRequestQueue(context)

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->

                cont.resume(response)
            },
            { cont.resume(null) })

        queue.add(stringRequest)
    }

    // In the event the word is not found, parses the html only for the translation from papago
    fun parseHTMLNotFound(doc: Document): String? {
        val translation = doc.getElementById("sectionTranslate")
        if (translation != null) {
            val translationResult = translation.getElementsByClass("mean_list").first()
            if (translationResult != null) return translationResult.text()
            else return null
        }
        else
            return null
    }

    // In the event the word is found, parses the HTML for the word, related words with expanded context, and multiple definitions
    fun parseHTMLFound(doc: Document): Pair<MutableList<Pair<String, Array<String>>>, MutableList<Pair<String, String>>>{
        val translation : MutableList<Pair<String, Array<String>>> = mutableListOf()
        var examples: MutableList<Pair<String, String>> = mutableListOf()
        val definitionContainer = doc.getElementsByClass("component_keyword has-saving-function").first()
        val exampleContainer = doc.getElementById("searchPage_example")

        // Add each pair of expanded words and definitions
        val definitionElements = definitionContainer?.getElementsByClass("row")
        if (definitionElements != null) {
            for (definition in definitionElements) {
                val word = definition.select("a.link")[0].text()
                val subDefinitions = definition.select("p.mean").map { it.text() }.toTypedArray()
                translation.add(Pair<String, Array<String>>(word, subDefinitions))
            }
        }

        // Add each pair of sentences and definitions
        val exampleElements = exampleContainer?.getElementsByClass("row")
        if (exampleElements != null) {
            for (example in exampleElements){
                val sentence = example.getElementsByClass("origin")[0].text()
                val meaning = example.getElementsByClass("translate")[0].text()
                examples.add(Pair<String, String>(sentence, meaning))
            }
        }

        return Pair(translation, examples)
    }
}
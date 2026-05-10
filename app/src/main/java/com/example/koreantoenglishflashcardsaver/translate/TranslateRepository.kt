package com.example.koreantoenglishflashcardsaver.translate

import android.util.Log
import com.example.koreantoenglishflashcardsaver.model.ExampleEntry
import com.example.koreantoenglishflashcardsaver.model.Flashcard
import com.example.koreantoenglishflashcardsaver.model.TranslationEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class TranslateRepository(private val webViewRenderer: WebViewRenderer) {

    suspend fun getTranslation(word: String): Flashcard{
        try{
            val html = webViewRenderer.fetchRenderedHtml("https://korean.dict.naver.com/koendict/#/search?query=$word")
            Log.i("Success", "Successfully found html")
        return withContext(Dispatchers.IO) {
            // First check if the search result found anything.
            val doc = Jsoup.parse(html)
            val notFound = doc.getElementsByClass("section_empty").isNotEmpty()
            // If it didn't, return only the result of machine translation
            if (notFound) {
                val directTranslation = parseHTMLNotFound(doc)
                if (directTranslation != null) {
                    Flashcard(
                        word = word,
                        translations = null,
                        examples = null,
                        directTranslation = directTranslation
                    )
                } else Flashcard(
                    word = "",
                    translations = null,
                    examples = null,
                    directTranslation = null)
            }
            // If it did, return all of the requested materials
            else {
                val (translation, examples, directTranslation) = parseHTMLFound(doc)
                Flashcard(
                    word = word,
                    translations = translation,
                    examples = examples,
                    directTranslation = directTranslation)
            }
        }
        } catch (e: Exception) {
            Log.e("CRASH", "Full stack trace:", e)
            throw e
    }
    }

    /** In the event the word is not found, parses the html only for the translation from papago
     *
     */
    fun parseHTMLNotFound(doc: Document): String? {
        return getDirectTranslation(doc)
    }

    /** In the event the word is found, parses the HTML for the word, related words with expanded context, and multiple definitions
     *
     */
    fun parseHTMLFound(doc: Document): Triple<MutableList<TranslationEntry>, MutableList<ExampleEntry>, String?>{
        val translation : MutableList<TranslationEntry> = mutableListOf()
        val examples: MutableList<ExampleEntry> = mutableListOf()
        val definitionContainer = doc.getElementsByClass("component_keyword has-saving-function").first()
        val exampleContainer = doc.getElementById("searchPage_example")

        // Add each pair of expanded words and definitions
        val definitionElements = definitionContainer?.getElementsByClass("row")
        if (definitionElements != null) {
            for (definition in definitionElements) {
                val word = definition.select("a.link")[0].text()
                doc.select("p.mean").select(".word_class ").remove()
                val subDefinitions = definition.select("p.mean").map { it.text() }.toList()

                if(word.containsKorean()) translation.add(TranslationEntry(word, subDefinitions))
            }
        }

        // Add each pair of sentences and definitions
        val exampleElements = exampleContainer?.getElementsByClass("row")
        if (exampleElements != null) {
            for (example in exampleElements){
                val sentence = example.getElementsByClass("origin")[0].getElementsByClass("text").text()
                val meaning = example.getElementsByClass("translate")[0].text()
                examples.add(ExampleEntry(sentence, meaning))
            }
        }

        // Add the direct translation if it exists
        val directTranslation = getDirectTranslation(doc)

        return Triple(translation, examples, directTranslation)
    }

    /**
     * Returns the direct translation if it exists
     */
    fun getDirectTranslation(doc: Document): String?{
        val translation = doc.getElementById("sectionTranslate")
        if (translation != null) {
            val translationResult = translation.getElementsByClass("mean_list").first()
            return if (translationResult != null) translationResult.text()
            else "you fucked up"
        }
        else
            return null
    }
    fun String.containsKorean(): Boolean = any { it.code in 0xAC00..0xD7AF || it.code in 0x1100..0x11FF || it.code in 0x3130..0x318F }
}
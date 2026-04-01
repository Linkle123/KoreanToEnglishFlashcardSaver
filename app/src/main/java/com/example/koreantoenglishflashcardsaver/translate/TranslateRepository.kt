package com.example.koreantoenglishflashcardsaver.translate

import com.example.koreantoenglishflashcardsaver.model.Flashcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class TranslateRepository(private  val webViewRenderer: WebViewRenderer) {
    suspend fun getTranslation(word: String): Flashcard{
        val html = webViewRenderer.fetchRenderedHtml("https://korean.dict.naver.com/koendict/#/search?query=$word")
        val result: Flashcard = Flashcard("", null, null)
        return withContext(Dispatchers.IO) {
            // First check if the search result found anything.
            val doc = Jsoup.parse(html)
            val found = doc.getElementsByClass("section_empty").isEmpty()
            // If it didn't, return only the result of machine translation
            if (!found) {
                val translation = parseHTMLNotFound(doc)
                if (translation != null) {
                    result.copy(
                        word = word,
                        translations = mutableListOf(Pair(word, arrayOf(translation))),
                        examples = null
                    )
                } else result.copy(
                    word = "",
                    translations = null,
                    examples = null)
            }
            // If it did, return all of the requested materials
            else {
                val (translation, examples) = parseHTMLFound(doc)
                result.copy(
                    word = word,
                    translations = translation,
                    examples = examples)
            }
            return@withContext result
        }
    }

    /** In the event the word is not found, parses the html only for the translation from papago
     *
     */
    fun parseHTMLNotFound(doc: Document): String? {
        val translation = doc.getElementById("sectionTranslate")
        if (translation != null) {
            val translationResult = translation.getElementsByClass("mean_list").first()
            return if (translationResult != null) translationResult.text()
            else "you fucked up"
        }
        else
            return null
    }

    /** In the event the word is found, parses the HTML for the word, related words with expanded context, and multiple definitions
     *
     */
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
                doc.select("p.mean").select(".word_class ").remove();
                val subDefinitions = definition.select("p.mean").map { it.text() }.toTypedArray()

                if(word.containsKorean()) translation.add(Pair<String, Array<String>>(word, subDefinitions))
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
    fun String.containsKorean(): Boolean = any { it.code in 0xAC00..0xD7AF || it.code in 0x1100..0x11FF || it.code in 0x3130..0x318F }
}
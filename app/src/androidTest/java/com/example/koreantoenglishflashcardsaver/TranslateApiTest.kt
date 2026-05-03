package com.example.koreantoenglishflashcardsaver

import android.util.Log
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.koreantoenglishflashcardsaver.model.Flashcard
import com.example.koreantoenglishflashcardsaver.translate.TranslateRepository
import com.example.koreantoenglishflashcardsaver.translate.TranslateViewModel
import com.example.koreantoenglishflashcardsaver.translate.WebViewRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class TranslateApiTest {

    private lateinit var translateService: TranslateViewModel
    private lateinit var webViewRenderer: WebViewRenderer
    private lateinit var translateRepository: TranslateRepository
    @get:Rule
    val activityRule = ActivityScenarioRule(EmptyTestActivity::class.java) // Use a simple empty activity

    @Before
    fun createServices() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activityRule.scenario.onActivity { activity ->
                webViewRenderer = WebViewRenderer.getInstance(activity)
                translateRepository = TranslateRepository(webViewRenderer)
                translateService = TranslateViewModel(translateRepository)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getData() = runTest(UnconfinedTestDispatcher()) {
        val response = withContext(Dispatchers.Main) {
            webViewRenderer.fetchRenderedHtml("https://korean.dict.naver.com/koendict/#/search?query=바나나")
        }
        Log.i("response", response)

        assert(response.contains("a bunch of <strong class=\"highlight\">bananas</strong> / grapes, etc."))
    }


    @Test
    fun parseHTMLNotFound() = runTest {
        val response = withContext(Dispatchers.Main) {
            webViewRenderer.fetchRenderedHtml("https://korean.dict.naver.com/koendict/#/search?query=ㅈㄷㅂ규ㅕㄷ")
        }
        val doc = Jsoup.parse(response)
        val translation = translateRepository.parseHTMLNotFound(doc)
        if(translation != null) {
            assert(translation.equals("ㅈㄷㅂ규ㅕㄷ"))
        }

    }

    @Test
    fun parseHTMLFound() = runTest{
        val response = withContext(Dispatchers.Main) {
            webViewRenderer.fetchRenderedHtml("https://korean.dict.naver.com/koendict/#/search?query=바나나")
        }
        val doc = Jsoup.parse(response)
        val (translation, examples) = translateRepository.parseHTMLFound(doc)
        val flashcard = Flashcard("바나나", translation, examples)
        Log.i("translations", flashcard.getTranslationsAsString()!!)
        val exists = flashcard.translations!!.any { pair ->
            pair.first == "바나나" && pair.second.contentEquals(arrayOf("banana"))
        }
        assert(exists)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getTranslation() = runTest {
        val flashcard = withContext(Dispatchers.Main){
            translateRepository.getTranslation("바나나")
        }
        Log.i("translations", flashcard.getTranslationsAsString()!!)
        val exists = flashcard.translations!!.any { pair ->
            pair.first == "바나나" && pair.second.contentEquals(arrayOf("banana"))
        }
        assert(exists)
    }


}
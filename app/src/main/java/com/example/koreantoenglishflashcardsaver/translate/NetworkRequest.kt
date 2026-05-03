package com.example.koreantoenglishflashcardsaver.translate

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class WebViewRenderer private constructor(context: Context){
    private val webView: WebView = WebView(context.applicationContext).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true // Required for many modern sites
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"

        // Important: WebViews must stay on the Main Thread
        webViewClient = WebViewClient()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: WebViewRenderer? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebViewRenderer(context.applicationContext).also {
                    INSTANCE = it
                }
            }
    }

    fun getWebView(): WebView {
        return webView
    }


    suspend fun fetchRenderedHtml(url: String): String = withTimeout(10_000) {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Give JS 2 seconds to finish building the DOM
                        view?.postDelayed({
                            view.evaluateJavascript("document.documentElement.outerHTML") { rawHtml ->
                                if (rawHtml == null || rawHtml == "null") {
                                    if (continuation.isActive) continuation.resumeWithException(Exception("Empty HTML"))
                                    return@evaluateJavascript
                                }
                                // Use JSONTokener to properly unescape the JSON string
                                val cleanedHtml = org.json.JSONTokener(rawHtml).nextValue().toString()

                                if (continuation.isActive) {
                                    continuation.resume(cleanedHtml)
                                }
                            }
                        }, 2000)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (continuation.isActive) continuation.resumeWithException(Exception("WebView Error: ${error?.description}"))
                    }
                }
                webView.loadUrl(url)
                // If the coroutine is cancelled (timeout), stop the webview
                continuation.invokeOnCancellation {
                    webView.stopLoading()
                }
            }
        }
    }
}

package com.example.koreantoenglishflashcardsaver.translate

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
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


class WebViewRenderer constructor(context: Context){
    private var webView: WebView? = null

    init {
        webView = getOrCreateWebView(context)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: WebViewRenderer? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebViewRenderer(context).also {
                    INSTANCE = it
                }
            }
    }

    // Initialize once in your Application class or the first time it's needed
    fun getOrCreateWebView(context: Context): WebView {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalStateException("WebView must be initialized on the Main Thread")
        }
        if (webView == null) {
            // Use ApplicationContext to prevent memory leaks with the Singleton
            webView = WebView(context.applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true // Required for many modern sites
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"

                // Important: WebViews must stay on the Main Thread
                webViewClient = WebViewClient()
            }
        }
        return webView!!
    }

    suspend fun fetchRenderedHtml(url: String): String = withTimeout(10_000) {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                webView!!.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Give Naver's JS 2 seconds to finish building the DOM
                        view?.postDelayed({
                            view.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { html ->
                                // WebView returns a JSON-encoded string (wrapped in quotes with escapes)
                                val cleanedHtml = html.removeSurrounding("\"")
                                    .replace("\\u003C", "<")
                                    .replace("\\\"", "\"")
                                    .replace("\\n", "\n")
                                    .replace("\\t","")
                                if (continuation.isActive) continuation.resume(cleanedHtml)
                                else continuation.resume(cleanedHtml)
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
                webView!!.loadUrl(url)
            }
        }
    }
}

package com.sgvdev.autostart

import android.os.Bundle
import android.os.PersistableBundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MyActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        setContentView(R.layout.activity_my)
        webView = findViewById(R.id.webView)
        webView.apply {
            webViewClient = WebViewClient()
            loadUrl("https://pikabu.ru/")
            settings.javaScriptEnabled = true
        }
    }
}

package com.example.rentradar

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Rent Radar Help"

        val webView = WebView(this).apply {

            webViewClient = WebViewClient()

            settings.javaScriptEnabled = false
            settings.domStorageEnabled = true

            loadUrl(
                "https://www.rta.qld.gov.au/before-renting/preparing-for-a-tenancy"
            )
        }

        setContentView(webView)
    }
}

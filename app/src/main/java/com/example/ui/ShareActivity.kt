package com.example.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.data.DownloadService

class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
        finish()
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrEmpty()) {
                    val url = extractUrl(sharedText)
                    if (url != null) {
                        DownloadService.startDownload(this, url)
                    } else {
                        Toast.makeText(this, "No valid URL found in shared text", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Empty shared text", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun extractUrl(text: String): String? {
        val urlRegex = "(https?://[\\w-]+(\\.[\\w-]+)+(/\\S*)?)".toRegex()
        val matchResult = urlRegex.find(text)
        return matchResult?.value
    }
}

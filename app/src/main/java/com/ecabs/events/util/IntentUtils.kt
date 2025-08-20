package com.ecabs.events.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Utility class for handling system intents
 */
object IntentUtils {
    
    /**
     * Opens a URL in the default browser
     * 
     * @param context Android context for starting the intent
     * @param url The URL to open
     */
    fun openUrl(context: Context, url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }
    }
    
    /**
     * Shares text content using system share intent
     * 
     * @param context Android context for starting the intent
     * @param text The text content to share
     */
    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }
}

package com.snapreceipt.io.ui.common

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.snapreceipt.io.R

class PolicyWebViewActivity : EdgeToEdgeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL).orEmpty().trim()
        if (url.isBlank()) {
            Toast.makeText(this, getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .build()
        try {
            customTabsIntent.launchUrl(this, Uri.parse(url))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
        } finally {
            finish()
        }
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        /** 保留供调用方传入，Custom Tabs 不展示该标题。 */
        const val EXTRA_TITLE = "extra_title"
    }
}

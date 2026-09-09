package com.autohub.android.browser

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class BrowserActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var addressField: EditText
    private lateinit var backButton: Button
    private lateinit var forwardButton: Button
    private lateinit var reloadButton: Button
    private lateinit var statusText: TextView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val title = TextView(this).apply {
            text = "AutoHub Browser"
            textSize = 22f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        val addressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        addressField = EditText(this).apply {
            setSingleLine(true)
            hint = "Address"
            setText(savedInstanceState?.getString(KEY_ADDRESS) ?: DEFAULT_HOME_URL)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    loadAddressField()
                    true
                } else {
                    false
                }
            }
        }
        addressRow.addView(
            addressField,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )

        val goButton = Button(this).apply {
            text = "Go"
            setOnClickListener { loadAddressField() }
        }
        addressRow.addView(
            goButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginStart = dp(8)
            },
        )
        root.addView(
            addressRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        statusText = TextView(this).apply {
            text = "Ready"
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(4), 0, dp(4))
        }
        root.addView(
            statusText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        val navigationRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        backButton = Button(this).apply {
            text = "Back"
            isEnabled = false
            setOnClickListener { webView.goBack() }
        }
        forwardButton = Button(this).apply {
            text = "Forward"
            isEnabled = false
            setOnClickListener { webView.goForward() }
        }
        reloadButton = Button(this).apply {
            text = "Reload"
            isEnabled = false
            setOnClickListener { webView.reload() }
        }

        navigationRow.addView(backButton, weightedButtonParams())
        navigationRow.addView(forwardButton, weightedButtonParams(dp(8)))
        navigationRow.addView(reloadButton, weightedButtonParams(dp(8)))
        root.addView(
            navigationRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.safeBrowsingEnabled = true

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean {
                    val requestedUrl = request?.url?.toString() ?: return true
                    val allowed = BrowserNavigationPolicy.isAllowedAbsoluteUrl(requestedUrl)
                    statusText.text = if (allowed) {
                        "Loading…"
                    } else {
                        "Blocked non-HTTP(S) navigation."
                    }
                    return !allowed
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    statusText.text = "Loading…"
                    syncNavigationState(view)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    statusText.text = "Ready"
                    syncNavigationState(view)
                }

                override fun doUpdateVisitedHistory(
                    view: WebView?,
                    url: String?,
                    isReload: Boolean,
                ) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    syncNavigationState(view)
                }
            }
        }
        root.addView(
            webView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        setContentView(root)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )

        val restored = savedInstanceState
            ?.getBundle(KEY_WEBVIEW_STATE)
            ?.let { webView.restoreState(it) != null }
            ?: false

        if (restored) {
            statusText.text = "Ready"
            syncNavigationState(webView)
        } else {
            webView.loadUrl(DEFAULT_HOME_URL)
        }
    }

    private fun loadAddressField() {
        val normalized = BrowserNavigationPolicy.normalizeUserInput(addressField.text.toString())
        if (normalized == null) {
            statusText.text = "Only valid HTTP(S) addresses are allowed."
            return
        }

        statusText.text = "Loading…"
        addressField.setText(normalized)
        webView.loadUrl(normalized)
    }

    private fun syncNavigationState(view: WebView?) {
        val activeView = view ?: return
        activeView.url?.takeIf { it.isNotBlank() }?.let(addressField::setText)
        backButton.isEnabled = activeView.canGoBack()
        forwardButton.isEnabled = activeView.canGoForward()
        reloadButton.isEnabled = activeView.url?.isNotBlank() == true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val webViewState = Bundle()
        webView.saveState(webViewState)
        outState.putBundle(KEY_WEBVIEW_STATE, webViewState)
        outState.putString(KEY_ADDRESS, addressField.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.webChromeClient = null
        webView.webViewClient = WebViewClient()
        webView.removeAllViews()
        webView.destroy()
        super.onDestroy()
    }

    private fun weightedButtonParams(startMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = startMargin
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val DEFAULT_HOME_URL = "https://example.com"
        private const val KEY_WEBVIEW_STATE = "browser.webview_state"
        private const val KEY_ADDRESS = "browser.address"
    }
}

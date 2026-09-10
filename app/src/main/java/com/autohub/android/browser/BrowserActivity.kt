package com.autohub.android.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class BrowserActivity : ComponentActivity() {
    private lateinit var rootContainer: FrameLayout
    private lateinit var browserContent: LinearLayout
    private lateinit var webView: WebView
    private lateinit var addressField: EditText
    private lateinit var backButton: Button
    private lateinit var forwardButton: Button
    private lateinit var reloadButton: Button
    private lateinit var desktopModeButton: Button
    private lateinit var statusText: TextView
    private lateinit var cookieManager: CookieManager
    private lateinit var browserStateStore: BrowserStateStore
    private lateinit var mobileUserAgent: String
    private var userAgentMode = BrowserUserAgentMode.MOBILE
    private var fullscreenView: View? = null
    private var fullscreenContainer: FrameLayout? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        browserStateStore = SharedPreferencesBrowserStateStore(this)
        cookieManager = CookieManager.getInstance()
        userAgentMode = browserStateStore.loadUserAgentMode()
        val persistedState = if (savedInstanceState == null) {
            browserStateStore.load()
        } else {
            null
        }

        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        browserContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        rootContainer.addView(
            browserContent,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        val title = TextView(this).apply {
            text = "AutoHub Browser"
            textSize = 22f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, dp(8))
        }
        browserContent.addView(
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
            setText(
                savedInstanceState?.getString(KEY_ADDRESS)
                    ?: persistedState?.currentUrl
                    ?: DEFAULT_HOME_URL,
            )
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
        browserContent.addView(
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
        browserContent.addView(
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
        browserContent.addView(
            navigationRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        desktopModeButton = Button(this).apply {
            setOnClickListener { toggleUserAgentMode() }
        }
        updateUserAgentModeButton()
        browserContent.addView(
            desktopModeButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        val clearBrowserDataButton = Button(this).apply {
            text = "Clear browser data"
            setOnClickListener { clearBrowserData() }
        }
        browserContent.addView(
            clearBrowserDataButton,
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

            mobileUserAgent = settings.userAgentString.orEmpty()
            applyUserAgentMode(settings)

            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, false)

            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                enqueueDownload(
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                )
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(
                    view: View?,
                    callback: CustomViewCallback?,
                ) {
                    if (view == null || callback == null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    enterFullscreenMedia(view, callback)
                }

                override fun onHideCustomView() {
                    exitFullscreenMedia()
                }
            }

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

                override fun onPageStarted(
                    view: WebView?,
                    url: String?,
                    favicon: android.graphics.Bitmap?,
                ) {
                    super.onPageStarted(view, url, favicon)
                    statusText.text = "Loading…"
                    syncNavigationState(view)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    statusText.text = "Ready"
                    syncNavigationState(view)
                    persistCurrentPage(url)
                    cookieManager.flush()
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
        browserContent.addView(
            webView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        setContentView(rootContainer)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        fullscreenView != null -> exitFullscreenMedia()
                        webView.canGoBack() -> webView.goBack()
                        else -> {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            },
        )

        val restoredConfigurationState = savedInstanceState
            ?.getBundle(KEY_WEBVIEW_STATE)
            ?.let { webView.restoreState(it) != null }
            ?: false

        when {
            restoredConfigurationState -> {
                statusText.text = "Ready"
                syncNavigationState(webView)
            }

            persistedState != null -> {
                statusText.text = "Restoring last page…"
                webView.loadUrl(persistedState.currentUrl)
            }

            else -> webView.loadUrl(DEFAULT_HOME_URL)
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

    private fun enqueueDownload(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
    ) {
        val trimmedUrl = url?.trim()
        val suggestedFileName = trimmedUrl?.let {
            URLUtil.guessFileName(it, contentDisposition, mimeType)
        }
        val spec = BrowserDownloadPolicy.create(
            url = trimmedUrl,
            suggestedFileName = suggestedFileName,
            mimeType = mimeType,
        )
        if (spec == null) {
            statusText.text = "Blocked unsupported download URL."
            return
        }

        val request = DownloadManager.Request(Uri.parse(spec.url))
            .setTitle(spec.fileName)
            .setDescription("AutoHub Browser download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        spec.mimeType?.let(request::setMimeType)
        userAgent
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { request.addRequestHeader("User-Agent", it) }
        cookieManager
            .getCookie(spec.url)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { request.addRequestHeader("Cookie", it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, spec.fileName)
        } else {
            request.setDestinationInExternalFilesDir(
                this,
                Environment.DIRECTORY_DOWNLOADS,
                spec.fileName,
            )
        }

        runCatching {
            getSystemService(DownloadManager::class.java).enqueue(request)
        }.onSuccess {
            statusText.text = "Download queued: ${spec.fileName}"
        }.onFailure {
            statusText.text = "Could not start download."
        }
    }

    private fun enterFullscreenMedia(
        view: View,
        callback: WebChromeClient.CustomViewCallback,
    ) {
        if (fullscreenView != null) {
            callback.onCustomViewHidden()
            return
        }

        fullscreenView = view
        fullscreenCallback = callback
        val container = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER,
                ),
            )
        }
        fullscreenContainer = container
        browserContent.visibility = View.GONE
        rootContainer.addView(
            container,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        hideFullscreenSystemBars()
    }

    private fun exitFullscreenMedia() {
        val activeView = fullscreenView ?: return
        fullscreenContainer?.removeView(activeView)
        fullscreenContainer?.let(rootContainer::removeView)
        fullscreenContainer = null
        fullscreenView = null
        browserContent.visibility = View.VISIBLE
        showSystemBars()
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
        webView.requestFocus()
    }

    private fun hideFullscreenSystemBars() {
        WindowInsetsControllerCompat(window, rootContainer).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemBars() {
        WindowInsetsControllerCompat(window, rootContainer)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    private fun toggleUserAgentMode() {
        userAgentMode = when (userAgentMode) {
            BrowserUserAgentMode.MOBILE -> BrowserUserAgentMode.DESKTOP
            BrowserUserAgentMode.DESKTOP -> BrowserUserAgentMode.MOBILE
        }
        browserStateStore.saveUserAgentMode(userAgentMode)
        applyUserAgentMode(webView.settings)
        updateUserAgentModeButton()

        statusText.text = when (userAgentMode) {
            BrowserUserAgentMode.MOBILE -> "Mobile mode enabled. Reloading…"
            BrowserUserAgentMode.DESKTOP -> "Desktop mode enabled. Reloading…"
        }
        if (webView.url?.isNotBlank() == true) {
            webView.reload()
        }
    }

    private fun applyUserAgentMode(settings: WebSettings) {
        val desktop = userAgentMode == BrowserUserAgentMode.DESKTOP
        settings.userAgentString = BrowserUserAgentPolicy.userAgentFor(
            mode = userAgentMode,
            mobileUserAgent = mobileUserAgent,
        )
        settings.useWideViewPort = desktop
        settings.loadWithOverviewMode = desktop
    }

    private fun updateUserAgentModeButton() {
        desktopModeButton.text = when (userAgentMode) {
            BrowserUserAgentMode.MOBILE -> "Desktop mode: OFF"
            BrowserUserAgentMode.DESKTOP -> "Desktop mode: ON"
        }
    }

    private fun syncNavigationState(view: WebView?) {
        val activeView = view ?: return
        activeView.url?.takeIf { it.isNotBlank() }?.let(addressField::setText)
        backButton.isEnabled = activeView.canGoBack()
        forwardButton.isEnabled = activeView.canGoForward()
        reloadButton.isEnabled = activeView.url?.isNotBlank() == true
    }

    private fun persistCurrentPage(url: String?) {
        BrowserPersistencePolicy
            .sanitizePersistedUrl(url)
            ?.let(browserStateStore::saveCurrentUrl)
    }

    private fun clearBrowserData() {
        browserStateStore.clear()
        WebStorage.getInstance().deleteAllData()
        webView.stopLoading()
        webView.clearHistory()
        webView.clearCache(true)
        cookieManager.removeAllCookies {
            cookieManager.flush()
            addressField.setText(DEFAULT_HOME_URL)
            statusText.text = "Browser data cleared."
            webView.loadUrl(DEFAULT_HOME_URL)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && fullscreenView != null) {
            hideFullscreenSystemBars()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val webViewState = Bundle()
        webView.saveState(webViewState)
        outState.putBundle(KEY_WEBVIEW_STATE, webViewState)
        outState.putString(KEY_ADDRESS, addressField.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        if (fullscreenView != null) {
            exitFullscreenMedia()
        }
        persistCurrentPage(webView.url)
        cookieManager.flush()
        super.onStop()
    }

    override fun onDestroy() {
        if (fullscreenView != null) {
            exitFullscreenMedia()
        }
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

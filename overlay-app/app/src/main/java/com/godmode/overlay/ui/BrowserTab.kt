package com.godmode.overlay.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.godmode.overlay.util.OverlayState

private val ACCENT = Color(0xFF00E5FF)
private val BG = Color(0xFF0A0A0A)
private val SURFACE = Color(0xFF1A1A1A)

// Ad/tracker block list — common domains
private val BLOCKED_HOSTS = setOf(
    "doubleclick.net", "googleadservices.com", "googlesyndication.com",
    "ads.google.com", "pagead2.googlesyndication.com", "adservice.google.com",
    "amazon-adsystem.com", "facebook.com/plugins", "connect.facebook.net",
    "analytics.google.com", "google-analytics.com", "hotjar.com",
    "scorecardresearch.com", "omtrdc.net", "demdex.net"
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserTab(state: OverlayState) {
    var urlInput by remember { mutableStateOf(state.browserUrl.value) }
    var progress by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize().background(BG)) {
        // URL bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SURFACE)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f).height(44.dp),
                singleLine = true,
                placeholder = { Text("URL or search...", fontSize = 12.sp, color = Color(0xFF666666)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ACCENT,
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = ACCENT,
                    focusedContainerColor = Color(0xFF111111),
                    unfocusedContainerColor = Color(0xFF111111)
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    focusManager.clearFocus()
                    val url = resolveUrl(urlInput)
                    state.browserUrl.value = url
                    webView?.loadUrl(url)
                })
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = { webView?.goBack() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = { webView?.reload() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (isLoading) Icons.Filled.Close else Icons.Filled.Refresh,
                    "Reload",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Progress bar
        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = ACCENT,
                trackColor = Color(0xFF222222)
            )
        }

        // WebView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = state.jsEnabled.value
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        userAgentString = userAgentString.replace("Mobile", "").replace("Android", "")
                            .trim() // desktop UA for better content
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        mediaPlaybackRequiresUserGesture = false
                    }
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            urlInput = url ?: urlInput
                        }
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            state.browserUrl.value = url ?: state.browserUrl.value
                            // Inject dark mode CSS if enabled
                            if (state.darkWebViewEnabled.value) injectDarkMode(view)
                            // Inject ad blocker if enabled
                            if (state.adBlockEnabled.value) injectAdBlock(view)
                            // Reader mode
                            if (state.readerModeEnabled.value) injectReaderMode(view)
                        }
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val host = request?.url?.host ?: return false
                            // Block ad/tracker requests
                            if (state.adBlockEnabled.value &&
                                BLOCKED_HOSTS.any { host.contains(it) }) {
                                return true
                            }
                            return false
                        }
                    }
                    loadUrl(state.browserUrl.value)
                    webView = this
                }
            },
            update = { view ->
                view.settings.javaScriptEnabled = state.jsEnabled.value
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun resolveUrl(input: String): String {
    val trimmed = input.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
        else -> "https://www.google.com/search?q=${android.net.Uri.encode(trimmed)}"
    }
}

private fun injectDarkMode(view: WebView?) {
    val css = """
        html { filter: invert(1) hue-rotate(180deg) !important; }
        img, video, iframe, canvas { filter: invert(1) hue-rotate(180deg) !important; }
    """.trimIndent().replace("\n", " ")
    view?.evaluateJavascript(
        """(function(){
            var s=document.getElementById('__gm_dark');
            if(!s){s=document.createElement('style');s.id='__gm_dark';document.head.appendChild(s);}
            s.textContent='$css';
        })();""", null
    )
}

private fun injectAdBlock(view: WebView?) {
    // Hide common ad container selectors
    val css = """
        .ad,.ads,.advertisement,[id*='ad-'],[class*='ad-'],[id*='ads'],[class*='ads'],
        [id*='banner'],[class*='banner'],[id*='sponsor'],[class*='sponsor'],
        iframe[src*='ads'],iframe[src*='doubleclick'] { display:none!important; }
    """.trimIndent().replace("\n", " ")
    view?.evaluateJavascript(
        """(function(){
            var s=document.getElementById('__gm_ab');
            if(!s){s=document.createElement('style');s.id='__gm_ab';document.head.appendChild(s);}
            s.textContent='$css';
        })();""", null
    )
}

private fun injectReaderMode(view: WebView?) {
    // Strip page to main content, clean readable view
    view?.evaluateJavascript(
        """(function(){
            var body = document.body;
            var article = document.querySelector('article') ||
                          document.querySelector('[role="main"]') ||
                          document.querySelector('main') || body;
            var content = article.innerHTML;
            document.body.innerHTML = '<div style="max-width:680px;margin:0 auto;padding:20px;font-family:Georgia,serif;font-size:18px;line-height:1.7;color:#e0e0e0;background:#111">' + content + '</div>';
        })();""", null
    )
}

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
import com.godmode.overlay.util.NetworkEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.json.JSONArray
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

        // Poll network interceptor log every 2 seconds
        LaunchedEffect(Unit) {
            while (isActive) {
                delay(2000)
                webView?.evaluateJavascript(
                    "(function(){ var l=window.__gm_log||[]; window.__gm_log=[]; return JSON.stringify(l); })()"
                ) { raw ->
                    if (raw != null && raw != "null" && raw != "\"null\"") {
                        runCatching {
                            val arr = JSONArray(raw.trim('"').replace("\\\"", "\"").replace("\\n", ""))
                            // raw comes back as a JSON string; need to re-parse
                        }
                        runCatching {
                            // evaluateJavascript returns the value as a JSON-encoded string
                            val cleaned = if (raw.startsWith("\"")) {
                                org.json.JSONArray(
                                    raw.substring(1, raw.length - 1)
                                        .replace("\\\"", "\"")
                                        .replace("\\\\", "\\")
                                )
                            } else {
                                org.json.JSONArray(raw)
                            }
                            for (i in 0 until cleaned.length()) {
                                val obj = cleaned.getJSONObject(i)
                                state.networkLogs.add(
                                    0, NetworkEntry(
                                        url = obj.optString("url"),
                                        type = obj.optString("type"),
                                        status = obj.optInt("status"),
                                        body = obj.optString("body")
                                    )
                                )
                            }
                            if (state.networkLogs.size > 200) {
                                repeat(state.networkLogs.size - 200) { state.networkLogs.removeLast() }
                            }
                        }
                    }
                }
            }
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
                            if (state.darkWebViewEnabled.value) injectDarkMode(view)
                            if (state.adBlockEnabled.value) injectAdBlock(view)
                            if (state.readerModeEnabled.value) injectReaderMode(view)
                            if (state.networkInterceptEnabled.value) injectNetworkInterceptor(view)
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

private fun injectNetworkInterceptor(view: WebView?) {
    val js = """
    (function(){
      if(window.__gm_hooked) return;
      window.__gm_hooked = true;
      window.__gm_log = window.__gm_log || [];

      var _fetch = window.fetch;
      window.fetch = async function() {
        var args = arguments;
        try {
          var resp = await _fetch.apply(this, args);
          var clone = resp.clone();
          var text = '';
          try { text = await clone.text(); } catch(e){}
          window.__gm_log.push({type:'fetch',url:String(args[0]),status:resp.status,body:text.substring(0,8000)});
          return resp;
        } catch(e) {
          window.__gm_log.push({type:'fetch',url:String(args[0]),status:0,body:'ERROR: '+e.message});
          throw e;
        }
      };

      var _open = XMLHttpRequest.prototype.open;
      var _send = XMLHttpRequest.prototype.send;
      XMLHttpRequest.prototype.open = function(m,u){
        this.__gm_url = u;
        return _open.apply(this, arguments);
      };
      XMLHttpRequest.prototype.send = function(){
        var xhr = this;
        this.addEventListener('loadend', function(){
          window.__gm_log.push({type:'xhr',url:String(xhr.__gm_url||''),status:xhr.status,body:(xhr.responseText||'').substring(0,8000)});
        });
        return _send.apply(this, arguments);
      };
    })();
    """.trimIndent()
    view?.evaluateJavascript(js, null)
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

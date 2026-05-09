package com.godmode.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.godmode.overlay.scraper.WebScraper
import com.godmode.overlay.util.NetworkEntry
import com.godmode.overlay.util.OverlayState
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val ACCENT   = Color(0xFF00E5FF)
private val BG       = Color(0xFF0A0A0A)
private val SURFACE  = Color(0xFF1A1A1A)
private val CARD     = Color(0xFF141414)
private val GREEN    = Color(0xFF69FF47)
private val YELLOW   = Color(0xFFFFD740)
private val RED      = Color(0xFFFF5252)
private val PURPLE   = Color(0xFFCF6EFF)

enum class InspectorMode { NETWORK, JSON, LOCAL }

@Composable
fun InspectorTab(state: OverlayState) {
    var mode by remember { mutableStateOf(InspectorMode.NETWORK) }

    Column(modifier = Modifier.fillMaxSize().background(BG)) {
        // Mode selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SURFACE)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            InspectorMode.entries.forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = {
                        Text(
                            when (m) {
                                InspectorMode.NETWORK -> "Network Log"
                                InspectorMode.JSON    -> "JSON Viewer"
                                InspectorMode.LOCAL   -> "Local HTML"
                            },
                            fontSize = 10.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ACCENT.copy(alpha = 0.15f),
                        selectedLabelColor = ACCENT,
                        containerColor = Color(0xFF1A1A1A),
                        labelColor = Color(0xFF888888)
                    )
                )
            }
        }

        when (mode) {
            InspectorMode.NETWORK -> NetworkLogView(state)
            InspectorMode.JSON    -> JsonViewerPane()
            InspectorMode.LOCAL   -> LocalHtmlPane(state)
        }
    }
}

// ── Network Log ──────────────────────────────────────────────────────────────

@Composable
private fun NetworkLogView(state: OverlayState) {
    val logs = state.networkLogs
    val clipboard = LocalClipboardManager.current

    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.NetworkCheck, null, tint = Color(0xFF333333), modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(8.dp))
                Text("No requests captured yet", color = Color(0xFF444444), fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Browse in the Browser tab — every fetch/XHR\nrequest will appear here with its full response.",
                    color = Color(0xFF333333),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${logs.size} requests", color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = { state.networkLogs.clear() }) {
                Text("Clear", color = Color(0xFFFF4444), fontSize = 11.sp)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            itemsIndexed(logs) { _, entry ->
                NetworkEntryCard(entry, clipboard)
            }
        }
    }
}

@Composable
private fun NetworkEntryCard(entry: NetworkEntry, clipboard: androidx.compose.ui.platform.ClipboardManager) {
    var expanded by remember { mutableStateOf(false) }
    var showJson by remember { mutableStateOf(false) }
    val prettyBody = remember(entry.body) { prettyJson(entry.body) }
    val isJson = prettyBody != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CARD)
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickableIf(true) { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Method badge
            Text(
                entry.type.uppercase(),
                color = if (entry.type == "fetch") ACCENT else YELLOW,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Spacer(Modifier.width(6.dp))
            // Status
            Text(
                entry.status.toString(),
                color = when {
                    entry.status in 200..299 -> GREEN
                    entry.status in 400..499 -> YELLOW
                    entry.status >= 500      -> RED
                    else                     -> Color(0xFF888888)
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(6.dp))
            // URL (truncated)
            Text(
                entry.url.removePrefix("https://").removePrefix("http://").take(60),
                color = Color(0xFFCCCCCC),
                fontSize = 10.sp,
                modifier = Modifier.weight(1f)
            )
            if (isJson) {
                Icon(
                    Icons.Filled.DataObject, "JSON",
                    tint = PURPLE,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                null,
                tint = Color(0xFF555555),
                modifier = Modifier.size(16.dp)
            )
        }

        if (expanded) {
            Divider(color = Color(0xFF222222), thickness = 1.dp)
            // Full URL
            Text(
                entry.url,
                color = Color(0xFF666666),
                fontSize = 9.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState())
            )

            // Toggle between raw and formatted JSON
            if (isJson) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)) {
                    FilterChip(
                        selected = !showJson,
                        onClick = { showJson = false },
                        label = { Text("Raw", fontSize = 9.sp) },
                        modifier = Modifier.height(24.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2A2A2A),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF111111),
                            labelColor = Color(0xFF666666)
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    FilterChip(
                        selected = showJson,
                        onClick = { showJson = true },
                        label = { Text("Formatted", fontSize = 9.sp) },
                        modifier = Modifier.height(24.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PURPLE.copy(alpha = 0.2f),
                            selectedLabelColor = PURPLE,
                            containerColor = Color(0xFF111111),
                            labelColor = Color(0xFF666666)
                        )
                    )
                }
            }

            // Body
            val bodyText = if (showJson && prettyBody != null) prettyBody else entry.body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .background(Color(0xFF0D0D0D))
                    .padding(10.dp)
            ) {
                val annotated = if (showJson && prettyBody != null) {
                    syntaxHighlight(prettyBody)
                } else {
                    AnnotatedString(bodyText.take(4000))
                }
                Text(
                    annotated,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                )
            }

            // Actions
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(entry.body)) },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
                ) {
                    Text("Copy body", color = Color(0xFF888888), fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(entry.url)) },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
                ) {
                    Text("Copy URL", color = Color(0xFF888888), fontSize = 10.sp)
                }
            }
        }
    }
}

// ── JSON Viewer ───────────────────────────────────────────────────────────────

@Composable
private fun JsonViewerPane() {
    var input by remember { mutableStateOf("") }
    var formatted by remember { mutableStateOf<AnnotatedString?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            placeholder = { Text("Paste raw JSON here...", fontSize = 11.sp, color = Color(0xFF555555)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ACCENT,
                unfocusedBorderColor = Color(0xFF333333),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = ACCENT,
                focusedContainerColor = Color(0xFF111111),
                unfocusedContainerColor = Color(0xFF111111)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val pretty = prettyJson(input)
                    if (pretty != null) {
                        formatted = syntaxHighlight(pretty)
                        error = null
                    } else {
                        error = "Not valid JSON"
                        formatted = null
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ACCENT),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Format", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            // Paste from clipboard button
            OutlinedButton(
                onClick = {
                    val clip = clipboard.getText()?.text ?: return@OutlinedButton
                    input = clip
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Text("Paste", color = Color(0xFF888888), fontSize = 11.sp)
            }
            if (formatted != null) {
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(prettyJson(input) ?: input)) },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
                ) {
                    Text("Copy", color = Color(0xFF888888), fontSize = 11.sp)
                }
            }
        }

        error?.let {
            Text("$it", color = RED, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }

        formatted?.let { ann ->
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D0D0D))
                    .padding(10.dp)
            ) {
                Text(
                    ann,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}

// ── Local HTML Pane ───────────────────────────────────────────────────────────

@Composable
private fun LocalHtmlPane(state: OverlayState) {
    var htmlInput by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<Map<String, List<String>>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scraper = remember { WebScraper() }
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text(
            "Paste saved HTML source — rips out prices, offers, data attributes, and JSON blobs. Works completely offline.",
            color = Color(0xFF555555),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = htmlInput,
            onValueChange = { htmlInput = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            placeholder = { Text("Paste page HTML here (long-press → Paste)...", fontSize = 10.sp, color = Color(0xFF444444)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ACCENT,
                unfocusedBorderColor = Color(0xFF333333),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = ACCENT,
                focusedContainerColor = Color(0xFF111111),
                unfocusedContainerColor = Color(0xFF111111)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = {
                    if (htmlInput.isBlank()) return@Button
                    scope.launch {
                        isLoading = true
                        results = scraper.extractFromHtml(htmlInput)
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = ACCENT),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Extract", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedButton(
                onClick = { htmlInput = clipboard.getText()?.text ?: "" },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Text("Paste", color = Color(0xFF888888), fontSize = 11.sp)
            }
            if (!results.isNullOrEmpty()) {
                OutlinedButton(
                    onClick = { results = null; htmlInput = "" },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
                ) {
                    Text("Clear", color = Color(0xFF888888), fontSize = 11.sp)
                }
            }
        }

        results?.let { data ->
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val order = listOf("prices", "offers", "json_blobs", "tables", "data_attributes")
                order.forEach { key ->
                    val items = data[key] ?: return@forEach
                    if (items.isEmpty()) return@forEach
                    item {
                        Text(
                            key.uppercase().replace("_", " "),
                            color = ACCENT,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(items) { value ->
                        val isJson = prettyJson(value) != null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CARD)
                                .padding(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            if (isJson) {
                                Text(
                                    syntaxHighlight(prettyJson(value)!!),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Text(value, color = Color(0xFFDDDDDD), fontSize = 11.sp, modifier = Modifier.weight(1f))
                            }
                            IconButton(
                                onClick = { clipboard.setText(AnnotatedString(value)) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, "Copy", tint = Color(0xFF555555), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun prettyJson(raw: String): String? {
    val s = raw.trim()
    return runCatching {
        when {
            s.startsWith("{") -> JSONObject(s).toString(2)
            s.startsWith("[") -> JSONArray(s).toString(2)
            else -> null
        }
    }.getOrNull()
}

fun syntaxHighlight(json: String): AnnotatedString = buildAnnotatedString {
    val keyColor    = Color(0xFF82AAFF)
    val strColor    = Color(0xFFC3E88D)
    val numColor    = Color(0xFFFF9D7E)
    val boolColor   = Color(0xFFFFCB6B)
    val nullColor   = Color(0xFFFF5370)
    val braceColor  = Color(0xFFBBBBBB)

    val tokenRegex = Regex(
        """"((?:[^"\\]|\\.)*)"\s*:""" +         // key
        """|"((?:[^"\\]|\\.)*)"""" +             // string value
        """|\b(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\b""" +  // number
        """|\b(true|false)\b""" +                // boolean
        """|\b(null)\b""" +                      // null
        """|([{}\[\],])"""                       // brace/bracket/comma
    )

    var last = 0
    tokenRegex.findAll(json).forEach { match ->
        if (match.range.first > last) {
            append(json.substring(last, match.range.first))
        }
        when {
            match.groups[1] != null -> {
                withStyle(SpanStyle(color = keyColor)) { append("\"${match.groups[1]!!.value}\":") }
            }
            match.groups[2] != null -> {
                withStyle(SpanStyle(color = strColor)) { append("\"${match.groups[2]!!.value}\"") }
            }
            match.groups[3] != null -> {
                withStyle(SpanStyle(color = numColor)) { append(match.groups[3]!!.value) }
            }
            match.groups[4] != null -> {
                withStyle(SpanStyle(color = boolColor)) { append(match.groups[4]!!.value) }
            }
            match.groups[5] != null -> {
                withStyle(SpanStyle(color = nullColor)) { append(match.groups[5]!!.value) }
            }
            match.groups[6] != null -> {
                withStyle(SpanStyle(color = braceColor)) { append(match.groups[6]!!.value) }
            }
        }
        last = match.range.last + 1
    }
    if (last < json.length) append(json.substring(last))
}

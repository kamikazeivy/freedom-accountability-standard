package com.godmode.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.godmode.overlay.scraper.WebScraper
import com.godmode.overlay.util.OverlayState
import com.godmode.overlay.util.ScrapedItem
import kotlinx.coroutines.launch

private val ACCENT = Color(0xFF00E5FF)
private val BG = Color(0xFF0A0A0A)
private val SURFACE = Color(0xFF1A1A1A)
private val CARD = Color(0xFF141414)

@Composable
fun ScraperTab(state: OverlayState) {
    val scope = rememberCoroutineScope()
    val scraper = remember { WebScraper() }
    var urlInput by remember { mutableStateOf(state.browserUrl.value) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf(ScrapeMode.CONTENT) }
    var structuredData by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize().background(BG)) {
        // URL input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SURFACE)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f).height(44.dp),
                singleLine = true,
                placeholder = { Text("URL to scrape...", fontSize = 11.sp, color = Color(0xFF555555)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ACCENT,
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = ACCENT,
                    focusedContainerColor = Color(0xFF111111),
                    unfocusedContainerColor = Color(0xFF111111)
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    focusManager.clearFocus()
                    runScrape(scope, scraper, urlInput, mode, state,
                        onLoading = { isLoading = it },
                        onError = { error = it },
                        onStructured = { structuredData = it })
                })
            )
            Spacer(Modifier.width(6.dp))
            // Use current browser URL
            IconButton(
                onClick = { urlInput = state.browserUrl.value },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Filled.Link, "Use browser URL", tint = ACCENT, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = {
                    focusManager.clearFocus()
                    runScrape(scope, scraper, urlInput, mode, state,
                        onLoading = { isLoading = it },
                        onError = { error = it },
                        onStructured = { structuredData = it })
                },
                modifier = Modifier.size(36.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ACCENT, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Search, "Scrape", tint = ACCENT, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Mode toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ScrapeMode.entries.forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(m.label, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ACCENT.copy(alpha = 0.15f),
                        selectedLabelColor = ACCENT,
                        containerColor = Color(0xFF1A1A1A),
                        labelColor = Color(0xFF888888)
                    )
                )
            }
        }

        error?.let {
            Text(
                "Error: $it",
                color = Color(0xFFFF4444),
                fontSize = 11.sp,
                modifier = Modifier.padding(8.dp)
            )
        }

        when (mode) {
            ScrapeMode.CONTENT -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    items(state.scrapedItems) { item ->
                        ScrapedItemCard(item, onCopy = {
                            clipboard.setText(AnnotatedString(item.text))
                        })
                    }
                }
            }
            ScrapeMode.OFFERS -> {
                StructuredDataView(structuredData, keys = listOf("prices", "offers"), clipboard)
            }
            ScrapeMode.DATA -> {
                StructuredDataView(structuredData, keys = listOf("tables", "data_attributes"), clipboard)
            }
        }
    }
}

@Composable
private fun ScrapedItemCard(item: ScrapedItem, onCopy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CARD)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.title,
                color = ACCENT,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.CopyAll, "Copy", tint = Color(0xFF888888), modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            item.text.take(600) + if (item.text.length > 600) "..." else "",
            color = Color(0xFFCCCCCC),
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
        if (item.images.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("${item.images.size} images captured", color = Color(0xFF666666), fontSize = 10.sp)
        }
    }
}

@Composable
private fun StructuredDataView(
    data: Map<String, List<String>>,
    keys: List<String>,
    clipboard: androidx.compose.ui.platform.ClipboardManager
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        keys.forEach { key ->
            val items = data[key] ?: emptyList()
            item {
                Text(
                    key.uppercase().replace("_", " "),
                    color = ACCENT,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            if (items.isEmpty()) {
                item {
                    Text("Nothing found", color = Color(0xFF555555), fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
            } else {
                items(items) { value ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CARD)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(value, color = Color(0xFFDDDDDD), fontSize = 11.sp, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { clipboard.setText(AnnotatedString(value)) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, "Copy", tint = Color(0xFF666666), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun runScrape(
    scope: kotlinx.coroutines.CoroutineScope,
    scraper: WebScraper,
    url: String,
    mode: ScrapeMode,
    state: OverlayState,
    onLoading: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onStructured: (Map<String, List<String>>) -> Unit
) {
    if (url.isBlank()) return
    val target = if (url.startsWith("http")) url else "https://$url"
    scope.launch {
        onLoading(true)
        onError(null)
        when (mode) {
            ScrapeMode.CONTENT -> {
                scraper.scrape(target).fold(
                    onSuccess = { item ->
                        state.scrapedItems.add(0, item)
                        if (state.scrapedItems.size > 50) state.scrapedItems.removeLast()
                    },
                    onFailure = { onError(it.message) }
                )
            }
            ScrapeMode.OFFERS, ScrapeMode.DATA -> {
                scraper.extractStructured(target).fold(
                    onSuccess = { onStructured(it) },
                    onFailure = { onError(it.message) }
                )
            }
        }
        onLoading(false)
    }
}

enum class ScrapeMode(val label: String) {
    CONTENT("Content"),
    OFFERS("Offers/Prices"),
    DATA("Raw Data")
}

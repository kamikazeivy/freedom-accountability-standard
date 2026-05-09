package com.godmode.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.godmode.overlay.clipboard.ClipboardMonitor
import com.godmode.overlay.util.OverlayState

private val ACCENT = Color(0xFF00E5FF)
private val BG = Color(0xFF0A0A0A)
private val CARD = Color(0xFF141414)
private val GREEN = Color(0xFF00C853)

@Composable
fun TogglesTab(state: OverlayState, clipboardMonitor: ClipboardMonitor) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BG),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item { SectionLabel("BROWSER") }
        item {
            ToggleRow(
                icon = Icons.Filled.Code,
                title = "JavaScript",
                subtitle = "Enable JS in the browser WebView",
                checked = state.jsEnabled.value,
                onToggle = { state.jsEnabled.value = it }
            )
        }
        item {
            ToggleRow(
                icon = Icons.Filled.DarkMode,
                title = "Force Dark Mode",
                subtitle = "CSS invert on all pages",
                checked = state.darkWebViewEnabled.value,
                onToggle = { state.darkWebViewEnabled.value = it }
            )
        }
        item {
            ToggleRow(
                icon = Icons.Filled.Block,
                title = "Ad Blocker",
                subtitle = "Block ads, trackers, banners via CSS + host filtering",
                checked = state.adBlockEnabled.value,
                onToggle = { state.adBlockEnabled.value = it }
            )
        }
        item {
            ToggleRow(
                icon = Icons.Filled.Article,
                title = "Reader Mode",
                subtitle = "Strip pages to readable content only",
                checked = state.readerModeEnabled.value,
                onToggle = { state.readerModeEnabled.value = it }
            )
        }

        item {
            ToggleRow(
                icon = Icons.Filled.NetworkCheck,
                title = "Network Interceptor",
                subtitle = "Hook fetch/XHR in browser — log all API responses to Inspector tab",
                checked = state.networkInterceptEnabled.value,
                onToggle = { state.networkInterceptEnabled.value = it }
            )
        }

        item { SectionLabel("TOOLS") }
        item {
            ToggleRow(
                icon = Icons.Filled.DataObject,
                title = "Scraper",
                subtitle = "Auto-scrape when switching to Scraper tab",
                checked = state.scraperEnabled.value,
                onToggle = { state.scraperEnabled.value = it }
            )
        }
        item {
            ToggleRow(
                icon = Icons.Filled.ContentPaste,
                title = "Clipboard Monitor",
                subtitle = "Capture everything you copy system-wide",
                checked = state.clipboardEnabled.value,
                onToggle = {
                    state.clipboardEnabled.value = it
                    if (it) clipboardMonitor.start() else clipboardMonitor.stop()
                }
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text(
                "Overlay sits on top of any app. Toggling here takes effect immediately.",
                color = Color(0xFF444444),
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Color(0xFF555555),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CARD)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, title,
            tint = if (checked) ACCENT else Color(0xFF555555),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFF666666), fontSize = 10.sp, lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ACCENT,
                uncheckedThumbColor = Color(0xFF555555),
                uncheckedTrackColor = Color(0xFF2A2A2A)
            )
        )
    }
}

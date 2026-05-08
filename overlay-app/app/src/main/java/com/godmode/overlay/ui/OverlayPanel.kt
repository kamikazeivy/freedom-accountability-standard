package com.godmode.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.godmode.overlay.util.Tab

private val BG = Color(0xEE0A0A0A)
private val ACCENT = Color(0xFF00E5FF)
private val SURFACE = Color(0xFF1A1A1A)

@Composable
fun OverlayPanel(
    state: OverlayState,
    clipboardMonitor: ClipboardMonitor,
    onClose: () -> Unit,
    onStop: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(360.dp)
            .heightIn(min = 200.dp, max = 600.dp)
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .background(BG)
    ) {
        Column {
            // Header bar
            PanelHeader(onClose = onClose, onStop = onStop)

            // Tab row
            TabRow(state)

            // Tab content
            Box(modifier = Modifier.weight(1f)) {
                when (state.currentTab.value) {
                    Tab.BROWSER -> BrowserTab(state)
                    Tab.SCRAPER -> ScraperTab(state)
                    Tab.CLIPBOARD -> ClipboardTab(state)
                    Tab.TOGGLES -> TogglesTab(state, clipboardMonitor)
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(onClose: () -> Unit, onStop: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "GOD MODE",
            color = ACCENT,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.ChevronLeft, "Minimize", tint = Color.White)
        }
        IconButton(onClick = onStop, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.PowerSettingsNew, "Stop", tint = Color(0xFFFF4444))
        }
    }
}

@Composable
private fun TabRow(state: OverlayState) {
    data class TabDef(val tab: Tab, val icon: ImageVector, val label: String)
    val tabs = listOf(
        TabDef(Tab.BROWSER, Icons.Filled.Language, "Browser"),
        TabDef(Tab.SCRAPER, Icons.Filled.DataObject, "Scraper"),
        TabDef(Tab.CLIPBOARD, Icons.Filled.ContentPaste, "Clipboard"),
        TabDef(Tab.TOGGLES, Icons.Filled.Tune, "Toggles")
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SURFACE)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { def ->
            val selected = state.currentTab.value == def.tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickableIf(true) { state.currentTab.value = def.tab }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    def.icon,
                    def.label,
                    tint = if (selected) ACCENT else Color(0xFF888888),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    def.label,
                    fontSize = 9.sp,
                    color = if (selected) ACCENT else Color(0xFF666666)
                )
            }
        }
    }
}

// Extension to avoid androidx.compose.foundation.clickable import collision
fun Modifier.clickableIf(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) this.then(androidx.compose.foundation.clickable(onClick = onClick)) else this

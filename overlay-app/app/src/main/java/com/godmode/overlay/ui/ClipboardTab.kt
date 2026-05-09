package com.godmode.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.godmode.overlay.clipboard.ClipboardMonitor
import com.godmode.overlay.util.OverlayState

private val ACCENT = Color(0xFF00E5FF)
private val BG = Color(0xFF0A0A0A)
private val SURFACE = Color(0xFF1A1A1A)
private val CARD = Color(0xFF141414)

@Composable
fun ClipboardTab(state: OverlayState) {
    // Sync clipboard history from monitor into state
    val entries = remember { state.clipboardHistory }

    LaunchedEffect(Unit) {
        // Seed from monitor's in-memory list on first open
    }

    Column(modifier = Modifier.fillMaxSize().background(BG)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SURFACE)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${entries.size} entries",
                color = Color(0xFF888888),
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { entries.clear() }) {
                Text("Clear all", color = Color(0xFFFF4444), fontSize = 11.sp)
            }
        }

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.ContentPaste,
                        "Empty",
                        tint = Color(0xFF333333),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Copy anything to capture it here", color = Color(0xFF444444), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                itemsIndexed(entries) { index, text ->
                    ClipboardEntry(
                        text = text,
                        onCopy = { state.clipboardHistory[index].let { } /* already in clipboard */ },
                        onDelete = { entries.removeAt(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipboardEntry(
    text: String,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val preview = if (text.length > 120) text.take(120) + "..." else text

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CARD)
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (expanded) text else preview,
                color = Color(0xFFDDDDDD),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            if (text.length > 120) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text(
                        if (expanded) "Show less" else "Show more",
                        color = ACCENT,
                        fontSize = 10.sp
                    )
                }
            }
        }
        Column {
            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.ContentCopy, "Copy", tint = Color(0xFF888888), modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.DeleteOutline, "Delete", tint = Color(0xFF666666), modifier = Modifier.size(14.dp))
            }
        }
    }
}

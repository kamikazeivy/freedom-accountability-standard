package com.godmode.overlay.util

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

data class ScrapedItem(
    val url: String,
    val title: String,
    val text: String,
    val images: List<String>
)

class OverlayState {
    val isPanelOpen = mutableStateOf(false)
    val currentTab = mutableStateOf(Tab.BROWSER)
    val browserUrl = mutableStateOf("https://www.google.com")

    // Toggle states
    val scraperEnabled = mutableStateOf(true)
    val clipboardEnabled = mutableStateOf(true)
    val readerModeEnabled = mutableStateOf(false)
    val jsEnabled = mutableStateOf(true)
    val darkWebViewEnabled = mutableStateOf(true)
    val adBlockEnabled = mutableStateOf(true)

    val scrapedItems = mutableStateListOf<ScrapedItem>()
    val clipboardHistory = mutableStateListOf<String>()
}

enum class Tab { BROWSER, SCRAPER, CLIPBOARD, TOGGLES }

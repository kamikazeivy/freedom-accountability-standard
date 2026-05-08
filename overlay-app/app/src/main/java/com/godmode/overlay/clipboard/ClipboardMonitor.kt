package com.godmode.overlay.clipboard

import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ClipboardMonitor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val history = mutableListOf<String>()
    private val listeners = mutableListOf<(String) -> Unit>()

    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        scope.launch {
            val clip = clipboardManager.primaryClip ?: return@launch
            val text = clip.getItemAt(0)?.coerceToText(context)?.toString() ?: return@launch
            if (text.isBlank() || history.firstOrNull() == text) return@launch
            history.add(0, text)
            if (history.size > 200) history.removeLast()
            listeners.forEach { it(text) }
        }
    }

    fun start() {
        clipboardManager.addPrimaryClipChangedListener(listener)
    }

    fun stop() {
        clipboardManager.removePrimaryClipChangedListener(listener)
    }

    fun onNewEntry(callback: (String) -> Unit) {
        listeners.add(callback)
    }

    fun copyToClipboard(text: String) {
        val clip = android.content.ClipData.newPlainText("godmode", text)
        clipboardManager.setPrimaryClip(clip)
    }
}

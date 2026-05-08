package com.godmode.overlay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.godmode.overlay.clipboard.ClipboardMonitor
import com.godmode.overlay.util.OverlayState

@Composable
fun OverlayRoot(
    state: OverlayState,
    clipboardMonitor: ClipboardMonitor,
    onDrag: (Int, Int) -> Unit,
    onStop: () -> Unit
) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box {
            // Floating bubble (always visible when panel is closed)
            AnimatedVisibility(
                visible = !state.isPanelOpen.value,
                enter = slideInHorizontally { -it },
                exit = slideOutHorizontally { -it }
            ) {
                FloatingBubble(
                    onOpen = { state.isPanelOpen.value = true },
                    onDrag = onDrag
                )
            }

            // Slide-in panel
            AnimatedVisibility(
                visible = state.isPanelOpen.value,
                enter = slideInHorizontally { -it },
                exit = slideOutHorizontally { -it }
            ) {
                OverlayPanel(
                    state = state,
                    clipboardMonitor = clipboardMonitor,
                    onClose = { state.isPanelOpen.value = false },
                    onStop = onStop
                )
            }
        }
    }
}

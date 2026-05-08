package com.godmode.overlay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.godmode.overlay.service.OverlayService
import com.godmode.overlay.util.Prefs

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* result comes from Settings, just recheck */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                SetupScreen()
            }
        }
    }

    @Composable
    private fun SetupScreen() {
        val hasOverlayPermission = remember { mutableStateOf(Settings.canDrawOverlays(this)) }
        var autoStart by remember { mutableStateOf(Prefs.autoStartOnBoot(this)) }

        LaunchedEffect(Unit) {
            hasOverlayPermission.value = Settings.canDrawOverlays(this@MainActivity)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080808)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Logo / title
                Text(
                    "GOD MODE",
                    color = Color(0xFF00E5FF),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
                Text(
                    "Overlay · Browser · Scraper · Clipboard",
                    color = Color(0xFF555555),
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(8.dp))

                // Permission card
                PermissionCard(
                    icon = Icons.Filled.Layers,
                    title = "Draw over other apps",
                    description = "Required to show the overlay on top of any app",
                    granted = hasOverlayPermission.value,
                    onGrant = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        overlayPermissionLauncher.launch(intent)
                    }
                )

                // Auto-start toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141414))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.PowerSettingsNew,
                        "Auto-start",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Auto-start on boot", color = Color.White, fontSize = 13.sp)
                        Text("Launch overlay when phone starts", color = Color(0xFF555555), fontSize = 10.sp)
                    }
                    Switch(
                        checked = autoStart,
                        onCheckedChange = {
                            autoStart = it
                            Prefs.setAutoStartOnBoot(this@MainActivity, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color(0xFF00E5FF),
                            checkedThumbColor = Color.White
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Launch / stop buttons
                Button(
                    onClick = {
                        if (Settings.canDrawOverlays(this@MainActivity)) {
                            OverlayService.start(this@MainActivity)
                            moveTaskToBack(true) // minimize so overlay is visible
                        } else {
                            hasOverlayPermission.value = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Icon(Icons.Filled.RemoveRedEye, "Launch", tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("LAUNCH OVERLAY", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { OverlayService.stop(this@MainActivity) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4444))
                ) {
                    Icon(Icons.Filled.Stop, "Stop")
                    Spacer(Modifier.width(8.dp))
                    Text("STOP OVERLAY")
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Grant 'Draw over other apps' once, then launch. The overlay runs as a minimal foreground service.",
                    color = Color(0xFF333333),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recheck permission state when returning from Settings
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141414))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, title,
            tint = if (granted) Color(0xFF00C853) else Color(0xFF888888),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.sp)
            Text(description, color = Color(0xFF555555), fontSize = 10.sp)
        }
        if (granted) {
            Icon(Icons.Filled.CheckCircle, "Granted", tint = Color(0xFF00C853), modifier = Modifier.size(20.dp))
        } else {
            TextButton(onClick = onGrant) {
                Text("Grant", color = Color(0xFF00E5FF), fontSize = 12.sp)
            }
        }
    }
}

fun darkColorScheme() = darkColorScheme(
    background = Color(0xFF080808),
    surface = Color(0xFF141414),
    primary = Color(0xFF00E5FF),
    onBackground = Color.White,
    onSurface = Color.White
)

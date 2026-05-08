# God Mode Overlay — Build & Install

## Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34 (API 34)
- A Samsung Galaxy S24 running Android 13 or 14

## Build steps

1. Open `overlay-app/` in Android Studio as the project root
2. Let Gradle sync (first sync downloads ~200 MB of dependencies)
3. Connect Galaxy S24 via USB, enable USB Debugging in Developer Options
4. Run → "app" on your device

## First-time setup on the phone

1. Open **God Mode** from the launcher
2. Tap **Grant** next to "Draw over other apps" → Android opens Settings → flip the toggle
3. Return to the app → tap **LAUNCH OVERLAY**
4. The app minimizes — a small eye icon now floats on screen
5. Tap the eye to open the panel; drag it to reposition

## Permissions explained

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the floating overlay |
| `FOREGROUND_SERVICE` | Keep overlay alive while using other apps |
| `INTERNET` | Browser + scraper |
| `READ_CLIPBOARD` | Clipboard history tab |
| `RECEIVE_BOOT_COMPLETED` | Optional auto-start on boot |

## Features

### Browser tab
- Full WebView browser that runs **over any app**
- Desktop user-agent (gets full sites, not mobile-stripped versions)
- Per-page toggles: JS, dark mode CSS invert, ad/tracker blocker, reader mode

### Scraper tab
Three modes:
- **Content** — strips a page to title + readable text, captures images
- **Offers/Prices** — extracts price elements, promo/deal/badge tags, discount labels
- **Raw Data** — dumps tables + all `data-*` attributes (often reveals internal values, hidden offers, A/B variants)

Tap the link icon to auto-fill the URL currently open in the Browser tab.

### Clipboard tab
- Monitors every copy event system-wide, stores up to 200 entries
- Expand, copy, or delete individual entries

### Toggles tab
Live switches for every feature — changes take effect immediately, no restart needed.

## Notes on "undetectable"

The overlay is a standard Android `TYPE_APPLICATION_OVERLAY` window. Other apps have no API to
programmatically detect its presence. The notification in the status bar is required by Android OS
for any foreground service — it is set to minimum priority (silent, collapsed, no badge).

What this overlay does NOT do and will NOT do:
- Bypass `FLAG_SECURE` on banking or authentication apps (that's a security attack surface)
- Capture passwords or inject into other apps
- Anything that requires root

## Sideload APK (no USB)

```bash
# From overlay-app/ directory
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

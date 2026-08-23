# InvoiceGen

**Professional offline invoice generator for Android.** Create, preview, and export beautiful PDF & image invoices — no internet required.

---

## Features

- **100% Offline** — Zero network calls. Your data never leaves your device.
- **4 Built-in Templates** — Premium v2, Minimal Mono, Bold Slate, Classic Ledger.
- **PDF & Image Export** — Generate publication-ready PDF documents and PNG snapshots, saved directly to your Downloads folder.
- **Business Profile** — Set your logo, company name, contact info, bank details, and preferred currency once — they auto-fill into every invoice.
- **Client Management** — Save and reuse client details across invoices.
- **Custom Branding** — Upload your own logo; it appears on the splash screen, app icon, and every exported invoice.
- **Revenue Dashboard** — Track total invoices at a glance from the home screen.
- **Dark Mode Support** — Follows system theme or set manually (Light / Dark / System).
- **Tiny APK** — Under 25 MB, no bloat.

## Screenshots

| Home | Create Invoice | PDF Export |
|------|----------------|------------|
| Dashboard with invoice count | Line items, tax, discount | Professional A4 output |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Storage | SharedPreferences (JSON serialization) |
| PDF Engine | Offscreen WebView → `PrintDocumentAdapter` |
| Image Engine | Offscreen WebView → `Canvas.draw()` bitmap snapshot |
| Build | Gradle 8.14 + AGP + Kotlin 2.0 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |

## Project Structure

```
app/src/main/java/com/invoicegen/android/
├── AppState.kt          # Data models, ViewModel, JSON persistence
├── InvoiceExport.kt     # HTML template engine, PDF/Image export
└── MainActivity.kt      # All Compose UI screens & navigation

app/src/main/res/
├── drawable/logo.png    # App logo (used in splash, header, invoices)
├── mipmap-*/            # Adaptive launcher icons
└── values/              # Colors, styles, themes
```

## Getting Started

### Prerequisites

- **Java 17+** (JDK)
- **Android SDK** (Platform 35, Build Tools 35.0.0)
- Linux / macOS / WSL environment

### Debug Build

```bash
./build.sh
```

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

```bash
./release.sh
```

On first run, a signing keystore is auto-generated. The signed APK will be at:
`app/build/outputs/apk/release/app-release.apk`

## Templates

Select your default template from **Settings → Invoice Template**:

| Template | Style |
|----------|-------|
| **Premium v2** | Elegant serif with gold accents and dark header |
| **Minimal Mono** | Clean black & white with monospace typography |
| **Bold Slate** | Dark navy header with coral accent highlights |
| **Classic Ledger** | Traditional double-border bookkeeping aesthetic |

All templates include your business logo, contact details, itemized line items, tax/discount calculations, payment info, and notes.

## Export

Invoices are exported to your device's **Downloads** folder:
- **PDF** — `Invoice_INV-0001.pdf`
- **Image** — `INV-0001.png`

## Permissions

| Permission | Purpose |
|-----------|---------|
| `READ_MEDIA_IMAGES` | Access gallery for logo upload |
| `WRITE_EXTERNAL_STORAGE` (≤ API 28) | Legacy file write for older devices |

No internet permission. No analytics. No tracking.

## License

Private / Proprietary.

---

Built with ❤️ by the InvoiceGen team.

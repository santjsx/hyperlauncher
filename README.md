# HyprLauncher 🚀

> **Linux Power-User Workflow + Android-Native Reliability + Extreme Performance**

[![Platform](https://img.shields.io/badge/Platform-Android_10+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Tests](https://img.shields.io/badge/Tests-181_Passing-success?style=for-the-badge&logo=checkmarx&logoColor=white)](#testing--verification)
[![Architecture](https://img.shields.io/badge/Architecture-Clean_%2F_MVI-orange?style=for-the-badge)](#architecture)

HyprLauncher is an industry-grade Android launcher inspired by modern Linux ricing culture—specifically **Hyprland**, **Waybar**, **Rofi**, and **Arch Linux**. 

Rather than superficially skinning Android with desktop mockups, HyprLauncher translates the minimalist, highly customizable, keyboard-and-gesture-centric interaction philosophy of modern Linux tiling window managers into a smooth, battery-friendly, and native Android experience.

---

## 🌟 Key Features

* **🖥️ Hyprland Home Experience & Waybar:**
  * Clean, minimal home screen with integrated live system Waybar (clock, battery, Wi-Fi status, workspaces, active rice indicators).
  * Smooth wallpaper surface with AMOLED pure black mode and adjustable background dimming.
  * Modular dock with customizable pin apps, corner radii, and translucency.

* **🔍 Rofi-Inspired App Drawer & Search Engine:**
  * 7-tier fuzzy matching algorithm: Exact, Prefix, Word Prefix, Acronym (e.g. `yt` → YouTube), Subsequence, Levenshtein Fuzzy, and Package Name.
  * Deterministic scoring boosts based on launch frequency, recency, and favorites.
  * Category tabs (`ALL`, `FAVORITES`, `RECENTS`) with alphabetical indexing.

* **🗂️ Dynamic Multi-Workspaces (1–5):**
  * Seamless workspace switching with dedicated app assignments per workspace.
  * Isolated application layouts simulating multi-monitor / virtual desktop tiling workflows.

* **🎨 Theme Engine & Built-In Presets:**
  * Curated palettes inspired by popular Linux color schemes: **Arch Dark**, **Hyprland Neon**, **Catppuccin Macchiato**, **Tokyo Night**, and **Gruvbox Dark**.
  * Dynamic Monospace font scaling, adjustable window borders, and rounded corner tokens.

* **🧪 Rice Studio (Live Preview & Presets):**
  * In-app rice creation laboratory with real-time live preview.
  * Save, duplicate, activate, and export complete rices to JSON with automated schema migration.

* **🧩 Production Widget System:**
  * Full Android `AppWidgetHost` and `AppWidgetHostView` integration.
  * Interactive drag-to-place, cell clamping, span resizing, and untrusted UI crash isolation boundaries.

* **⚡ Terminal Telemetry & Diagnostics (`hyprctl`):**
  * Live HUD metrics tracking FPS, frame time distributions, jank counts, search latency, JVM heap, and native memory.
  * One-touch maintenance actions: cache clearing, index rebuilds, and diagnostic export (JSON or ASCII fastfetch report).

* **🛡️ Hardened & Resilient (Phase 12):**
  * `ComponentCallbacks2` memory trimming (purges in-memory LruCache during low-memory pressure).
  * Stress-tested against libraries of 2,500+ installed applications with sub-5ms search latency.
  * Process-death survival with persistent Room database & DataStore state restoration.

---

## 🏗️ Architecture

HyprLauncher follows strict **Unidirectional Data Flow (UDF)** and **Clean Architecture**:

```text
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│   (HomeScreen, DrawerScreen, RiceStudio, Diagnostics)  │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow / Events
┌───────────────────────────┴────────────────────────────┐
│                    ViewModel Layer                     │
│      (HomeViewModel, DrawerViewModel, DiagnosticsVM)   │
└───────────────────────────▲────────────────────────────┘
                            │ Coroutines / Domain Models
┌───────────────────────────┴────────────────────────────┐
│                   Domain / Use Cases                   │
│  (LaunchAppUseCase, AppSearchEngine, WidgetHostManager)│
└───────────────────────────▲────────────────────────────┘
                            │ Repository Contracts
┌───────────────────────────┴────────────────────────────┐
│                      Data Layer                        │
│   ┌─────────────────────┐       ┌──────────────────┐   │
│   │   Room Database     │       │  DataStore / PB  │   │
│   │(Apps, Workspaces,   │       │(LauncherPrefs,   │   │
│   │ Widgets, Rices)     │       │ Customization)   │   │
│   └─────────────────────┘       └──────────────────┘   │
└────────────────────────────────────────────────────────┘
```

---

## 🧪 Testing & Verification

HyprLauncher adheres to strict engineering verification standards with **100% unit test success**:

```bash
# Run unit tests across all 13 phases (181 tests)
./gradlew testDebugUnitTest

# Assemble production debug APK
./gradlew assembleDebug
```

* **Total Unit Tests:** **181 tests**
* **Success Rate:** **100% (0 failures, 0 flakiness)**
* **Execution Time:** ~13–24 seconds

---

## 🚀 Building & Running

### Prerequisites
* **Android Studio** Ladybug / Jellyfish or latest stable
* **JDK 17**
* **Android SDK:** Min SDK 29 (Android 10+), Target SDK 34 (Android 14)

### Clone & Compile
```bash
# Clone the repository
git clone https://github.com/santjsx/hyperlauncher.git
cd hyperlauncher

# Build debug APK
./gradlew assembleDebug
```
The compiled APK will be output to:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📜 License

Distributed under the Apache License 2.0. See `LICENSE` for details.

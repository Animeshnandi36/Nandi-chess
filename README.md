# Nandi Chess - Play, Learn, and Master Chess with AI

Nandi Chess is an advanced AI-powered Chess application designed with a premium, glowing glassmorphic interface. It is fully built and compiled across two dual layers: a high-fidelity **native Android application (Kotlin/Jetpack Compose/Room)** and an elegant **independent HTML/CSS/JavaScript web runtime**.

## ✨ Distinctive Features

*   **🎙️ Low-Latency Synth Feedback (SoundEffects Engine):** Handcrafts synthesized audio frequencies for piece moves, clicks, captures, and check alerts without relying on physical asset files.
*   **🧠 Minimax AI opponent with Alpha-Beta Pruning:** Offers customized gameplay with adaptive depths representing 500 Elo (Beginner), 1200 Elo (Intermediate), 1800 Elo (Advanced), and 2500+ Elo (Expert) levels.
*   **📂 Persistent Game Database:** Automatically saves positions, histories, and board drafts locally using a local SQLite standard (Room library on Android, and `localStorage` on the Web).
*   **📊 Dynamic Evaluation Balance Bar:** Provides a live-updating numerical value evaluating positional advantage.
*   **📝 PGN Imports/Exports:** Fully supports importing external standard FEN chess moves and copying PGN strings.
*   **📸 Visual Canvas Snapshot Exports:** Renders current board setups into dynamic Canvas images and downloads them immediately as PNGs.

---

## 🛠️ Native Android Architecture

For native packaging, the codebase adheres to Material Design 3 and MVVM architecture:

1.  **`ChessEngine.kt`:** Standard official board state structures, en passant trackers, castling permissions, promotion checks, and legal moves filtering.
2.  **`ChessAI.kt`:** MiniMax algorithm pruning alpha/beta and material-development evaluation.
3.  **`SoundEffects.kt`:** Core Android PCM synthesized audio sine-wave form players.
4.  **`ChessGameDatabase.kt`:** Room Database entities, DAO interfaces, and coroutine-safe game persistence.
5.  **`ChessViewModel.kt`:** The unidirectional state hub managing game modes, clocks, settings, undos, and database records.
6.  **`ChessApp.kt`:** The main Jetpack Compose UI utilizing advanced frosted-glass modifiers, dynamic layout structures, responsive sizing, and interactive indicators.

---

## 🌐 Web App Standalone

The web frontend works cleanly in browser environments using the exact same logic:
*   `index.html` — The main structural view.
*   `style.css` — Modern glassmorphism layouts, colors, and keyframe animations.
*   `script.js` — Core minimax logic, Web Audio synthesizer, and clock countdown loops.

---

## 🚀 Running and Compiling the Applet

This project uses an incremental Gradle compile stack on the cloud Android build system.

*   To check code correctness and build the Android APK:
    ```bash
    gradle assembleDebug
    ```
*   To test the Robolectric specs and screen graphics:
    ```bash
    gradle :app:testDebugUnitTest
    ```

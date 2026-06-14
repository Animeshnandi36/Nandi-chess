♟️ Nandi Chess - Play, Learn, and Master Chess with AI

Nandi Chess is an advanced AI-powered chess application featuring intelligent opponents, move analysis, opening databases, game history, and a modern user interface. The project includes both an Android application and a browser-based web version.

🌐 Live Demo

Website: https://nandichess.netlify.app

✨ Features

🤖 AI Chess Engine

- Multiple AI difficulty levels
- Beginner (~500 Elo)
- Intermediate (~1200 Elo)
- Advanced (~1800 Elo)
- Expert (~2500+ Elo)

♟️ Complete Chess Rules

- Legal move validation
- Check and checkmate detection
- Stalemate detection
- Castling
- En passant
- Pawn promotion
- Draw detection

📊 Analysis Tools

- Move history tracking
- Position evaluation
- Best move suggestions
- Opening recognition
- PGN import/export support

🎨 Modern Interface

- Responsive design
- Dark mode support
- Glassmorphism UI
- Smooth animations
- Mobile-friendly layout

💾 Data Management

- Automatic game saving
- Local storage support
- Persistent settings
- Match history

🏗️ Project Structure

Nandi-chess/
├── app/
├── assets/
├── gradle/
├── index.html
├── style.css
├── script.js
├── README.md
├── LICENSE
└── metadata.json

🛠️ Technologies Used

Android

- Kotlin
- Jetpack Compose
- MVVM Architecture
- Room Database
- Coroutines

Web

- HTML5
- CSS3
- JavaScript
- Local Storage API

🚀 Installation

Web Version

1. Clone the repository

git clone https://github.com/Animeshnandi36/Nandi-chess.git

2. Open "index.html" in your browser

Android Version

./gradlew assembleDebug

📸 Screenshots

Add screenshots of:

- Main Menu
- Chess Board
- AI Match
- Analysis Screen

🤝 Contributing

Contributions, issues, and feature requests are welcome.

📄 License

This project is licensed under the MIT License.

👨‍💻 Developer

Animesh Nandi

GitHub: https://github.com/Animeshnandi36

---

© 2026 Nandi Chess · Developed by Animesh Nandi*   **📊 Dynamic Evaluation Balance Bar:** Provides a live-updating numerical value evaluating positional advantage.
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

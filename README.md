# ♟️ Nandi Chess - Play, Learn, and Master Chess with AI

Nandi Chess is an advanced AI-powered chess application featuring intelligent opponents, move analysis, opening databases, game history, and a modern user interface. The project includes both Android and Web versions with comprehensive chess functionality.

## 🌐 Live Demo

**[Play Nandi Chess Now](https://nandichess.netlify.app)** - https://nandichess.netlify.app

## ✨ Features

### 🤖 AI Chess Engine
- Multiple AI difficulty levels
  - **Beginner** (~500 Elo)
  - **Intermediate** (~1200 Elo)
  - **Advanced** (~1800 Elo)
  - **Expert** (~2500+ Elo)

### ♟️ Complete Chess Rules
- Legal move validation
- Check and checkmate detection
- Stalemate detection
- Castling, En passant, and Pawn promotion
- Draw detection

### 📊 Analysis Tools
- Move history tracking
- Position evaluation
- Best move suggestions
- Opening recognition
- PGN import/export support
- **Dynamic Evaluation Balance Bar** - Live-updating numerical value evaluating positional advantage
- **Visual Canvas Snapshot Exports** - Renders current board setups and downloads as PNGs

### 🎨 Modern Interface
- Responsive design optimized for desktop and mobile
- Dark mode support
- Glassmorphism UI with smooth animations
- Mobile-friendly layout

### 💾 Data Management
- Automatic game saving
- Local storage support
- Persistent settings
- Match history tracking

## 🏗️ Project Structure

```
Nandi-chess/
├── app/                      # Android app source code
├── assets/                   # Resources and assets
├── gradle/                   # Gradle build configuration
├── index.html                # Web app entry point
├── style.css                 # Web app styling
├── script.js                 # Web app chess logic
├── README.md                 # This file
├── LICENSE                   # MIT License
└── metadata.json             # Project metadata
```

## 🛠️ Technologies Used

### Android
- **Kotlin** - Primary language
- **Jetpack Compose** - Modern UI framework
- **MVVM Architecture** - Clean code structure
- **Room Database** - Local data persistence
- **Coroutines** - Async operations

### Web
- **HTML5** - Semantic markup
- **CSS3** - Responsive styling with glassmorphism
- **JavaScript** - Chess engine and interactivity
- **Local Storage API** - Game persistence

### Core Components
1. **ChessEngine.kt** - Board state, move validation, legal move filtering
2. **ChessAI.kt** - MiniMax algorithm with alpha-beta pruning
3. **SoundEffects.kt** - PCM synthesized audio effects
4. **ChessGameDatabase.kt** - Room Database entities and DAOs
5. **ChessViewModel.kt** - State management and game logic
6. **ChessApp.kt** - Jetpack Compose UI components

## 🚀 Installation & Setup

### Web Version
1. Clone the repository:
   ```bash
   git clone https://github.com/Animeshnandi36/Nandi-chess.git
   cd Nandi-chess
   ```
2. Open `index.html` in your web browser
3. Or visit the live demo: https://nandichess.netlify.app

### Android Version
1. Ensure you have Android Studio and the Android SDK installed
2. Clone the repository:
   ```bash
   git clone https://github.com/Animeshnandi36/Nandi-chess.git
   cd Nandi-chess
   ```
3. Build the APK:
   ```bash
   ./gradlew assembleDebug
   ```
4. Run unit tests:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

## 📸 Screenshots

The application features:
- **Main Menu** - Game mode selection and settings
- **Chess Board** - Interactive piece movement with drag-and-drop
- **AI Match** - Real-time play against multiple difficulty levels
- **Analysis Screen** - Move suggestions and position evaluation

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Please feel free to:
- Open issues for bugs or feature suggestions
- Submit pull requests with improvements
- Share feedback and ideas

## 📄 License

This project is licensed under the **MIT License** - see the LICENSE file for details.

## 👨‍💻 Developer

**Animesh Nandi**
- GitHub: [@Animeshnandi36](https://github.com/Animeshnandi36)
- Live Demo: [https://nandichess.netlify.app](https://nandichess.netlify.app)

---

**© 2026 Nandi Chess · Developed by Animesh Nandi**

Made with ♟️ and ❤️

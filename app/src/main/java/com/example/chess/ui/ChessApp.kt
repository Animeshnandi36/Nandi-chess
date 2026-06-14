package com.example.chess.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.chess.utils.SoundEffects
import com.example.chess.engine.ChessColor
import com.example.chess.engine.ChessEngine
import com.example.chess.engine.ChessMove
import com.example.chess.engine.Difficulty
import com.example.chess.engine.PieceType
import com.example.chess.engine.Square
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Beautiful glowing digital glass colors
val DarkGlassBackground = Color(0xD80D111D)
val GlassBorderColor = Color(0x33B1F5FF)
val GlowNeonTeal = Color(0xFF00E5FF)
val GlowNeonVibe = Color(0xFFFF007F)
val DarkSteelGrey = Color(0xFF1E2530)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChessApp(viewModel: ChessViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableStateOf(0) } // 0: Controls, 1: History & PGN, 2: Saved Games
    var isBoardFlipped by remember { mutableStateOf(false) }
    var showPgnImportDialog by remember { mutableStateOf(false) }
    var pgnImportText by remember { mutableStateOf("") }
    var showExportResultDialog by remember { mutableStateOf(false) }
    var pgnExportText by remember { mutableStateOf("") }

    // Root Container with stunning interstellar ambient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090A10),
                        Color(0xFF16192E),
                        Color(0xFF0E1220)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Floating glassmorphism radial blobs in background
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = 100.dp)
                .background(Brush.radialGradient(listOf(Color(0x2213D5FF), Color.Transparent)))
                .blur(50.dp)
        )
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 50.dp)
                .background(Brush.radialGradient(listOf(Color(0x1CFF00E4), Color.Transparent)))
                .blur(60.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Text(
                    text = "NANDI CHESS",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White,
                    letterSpacing = 4.sp,
                    style = MaterialTheme.typography.displayMedium.copy(
                        shadow = Shadow(
                            color = GlowNeonTeal,
                            offset = Offset(0f, 0f),
                            blurRadius = 15f
                        )
                    )
                )
                Text(
                    text = "Play, Learn, and Master Chess with AI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFA6BACF),
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Evaluation Indicator bar & Chess Clocks Wrapper
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Frosted Clock - Black Player
                ClockCard(
                    playerName = if (state.gameMode == GameMode.PVAI && state.userColor == ChessColor.WHITE) "Nandi AI (${state.difficulty.elo})" else "Player Black",
                    timeMs = state.blackTimeLeftMs,
                    isActive = !state.isCompleted && state.boardState.activeColor == ChessColor.BLACK,
                    badgeColor = GlowNeonVibe,
                    isClockEnabled = state.timeControl != TimeControl.UNLIMITED,
                    modifier = Modifier.weight(1f)
                )

                // Frosted Clock - White Player
                ClockCard(
                    playerName = if (state.gameMode == GameMode.PVAI && state.userColor == ChessColor.BLACK) "Nandi AI (${state.difficulty.elo})" else "Player White",
                    timeMs = state.whiteTimeLeftMs,
                    isActive = !state.isCompleted && state.boardState.activeColor == ChessColor.WHITE,
                    badgeColor = GlowNeonTeal,
                    isClockEnabled = state.timeControl != TimeControl.UNLIMITED,
                    modifier = Modifier.weight(1f)
                )
            }

            // Position Evaluation Slider Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "White Advantage: " + if (state.evaluationCentipawns >= 0) "+${(state.evaluationCentipawns / 100.0)}" else "${(state.evaluationCentipawns / 100.0)}",
                        fontSize = 11.sp,
                        color = Color(0xFFC0D1E5),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (state.openingName != null) {
                        Text(
                            text = state.openingName!!,
                            fontSize = 11.sp,
                            color = GlowNeonTeal,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.animateContentSize()
                        )
                    }
                }
                EvaluationScoreBar(scorePct = state.evaluationScore)
            }

            // Interactive Adaptive Chess Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .widthIn(max = 600.dp)
                    .testTag("chess_board")
                    .shadow(24.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, GlassBorderColor, RoundedCornerShape(12.dp))
            ) {
                ChessBoardGrid(
                    state = state,
                    isFlipped = isBoardFlipped,
                    onSquareClick = { viewModel.handleSquareClick(it) }
                )

                // AI Processing / Thinking Overlay Cover
                if (state.isAiThinking) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99080A12)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = GlowNeonTeal,
                                strokeWidth = 5.dp,
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "Nandi AI is analyzing...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    shadow = Shadow(color = GlowNeonTeal, blurRadius = 10f)
                                )
                            )
                        }
                    }
                }
            }

            // Operational shortcuts row: Undo, Redo, Hint, Flip Board, Analyze toggler
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.undoMove() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(DarkGlassBackground, CircleShape)
                        .border(1.dp, GlassBorderColor, CircleShape)
                        .testTag("undo_button")
                ) {
                    Icon(Icons.Filled.Undo, contentDescription = "Undo Move", tint = Color.White)
                }

                IconButton(
                    onClick = {
                        isBoardFlipped = !isBoardFlipped
                        SoundEffects.playMove()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(DarkGlassBackground, CircleShape)
                        .border(1.dp, GlassBorderColor, CircleShape)
                        .testTag("flip_board_button")
                ) {
                    Icon(Icons.Filled.Flip, contentDescription = "Flip Board Perspective", tint = Color.White)
                }

                IconButton(
                    onClick = { viewModel.requestHint() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (state.activeHintMove != null) Color(0xFF005060) else DarkGlassBackground,
                            CircleShape
                        )
                        .border(1.dp, GlowNeonTeal, CircleShape)
                        .testTag("hint_button")
                ) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = "Get Move Hint", tint = GlowNeonTeal)
                }

                IconButton(
                    onClick = { viewModel.toggleAnalyzeMode() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (state.isAnalyzeMode) Color(0x33FF007F) else DarkGlassBackground,
                            CircleShape
                        )
                        .border(1.dp, if (state.isAnalyzeMode) GlowNeonVibe else GlassBorderColor, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.CompassCalibration,
                        contentDescription = "Toggle Live Analysis",
                        tint = if (state.isAnalyzeMode) GlowNeonVibe else Color.White
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleMute() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(DarkGlassBackground, CircleShape)
                        .border(1.dp, GlassBorderColor, CircleShape)
                ) {
                    Icon(
                        imageVector = if (state.isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = "Mute audio toggle",
                        tint = if (state.isMuted) Color.Gray else GlowNeonTeal
                    )
                }
            }

            // Master Tabs system panel (Game Settings / History / Database list)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .background(DarkGlassBackground, RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorderColor, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = GlowNeonTeal,
                    indicator = { tabPositions ->
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(3.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(GlowNeonTeal, GlowNeonVibe)
                                    )
                                )
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Moves & PGN", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Saved Games", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }

                when (selectedTab) {
                    0 -> SettingsTabContent(viewModel, state)
                    1 -> NotationTabContent(
                        viewModel = viewModel,
                        state = state,
                        onImportClick = {
                            pgnImportText = ""
                            showPgnImportDialog = true
                        },
                        onExportClick = {
                            pgnExportText = viewModel.getPgnString()
                            showExportResultDialog = true
                        }
                    )
                    2 -> SavedGamesTabContent(viewModel, state)
                }
            }

            // Footer branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text(
                    text = "© 2026 Nandi Chess",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Developed by Animesh Nandi",
                    fontSize = 10.sp,
                    color = Color.DarkGray,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    // Checking Game completion result dialogues
    if (state.isCompleted) {
        Dialog(onDismissRequest = { viewModel.setupNewGame() }) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF0F1221), RoundedCornerShape(16.dp))
                    .border(2.dp, GlowNeonTeal, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Game over trophie",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "GAME OVER",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = state.resultMessage,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlowNeonTeal,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.setupNewGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = GlowNeonTeal),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Play Again", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Pawn Promotion Choice dialog popup
    state.promotionPending?.let { _ ->
        Dialog(onDismissRequest = { viewModel.cancelPromotion() }) {
            Box(
                modifier = Modifier
                    .background(DarkGlassBackground, RoundedCornerShape(16.dp))
                    .border(2.dp, GlassBorderColor, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Choose Promotion Piece",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val selectorPieces = listOf(
                            PieceType.QUEEN to "♛",
                            PieceType.ROOK to "♜",
                            PieceType.BISHOP to "♝",
                            PieceType.KNIGHT to "♞"
                        )
                        for ((type, symbol) in selectorPieces) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.selectPromotion(type) }
                                    .background(Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                    .border(1.dp, GlassBorderColor, RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = symbol,
                                    fontSize = 32.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = type.name.lowercase().capitalize(),
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                    TextButton(onClick = { viewModel.cancelPromotion() }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }

    // Import PGN dialog
    if (showPgnImportDialog) {
        Dialog(onDismissRequest = { showPgnImportDialog = false }) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF0F1221), RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorderColor, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Import PGN Game Moves", color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = pgnImportText,
                        onValueChange = { pgnImportText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("Paste PGN string here (e.g. 1. e4 e5 2. Nf3 Nc6...)", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlowNeonTeal,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showPgnImportDialog = false }) {
                            Text("Dismiss", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                showPgnImportDialog = false
                                viewModel.importPgn(pgnImportText)
                                Toast.makeText(context, "PGN Game Loaded!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlowNeonTeal)
                        ) {
                            Text("Load Game", color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    // Export Board / PGN Output dialog
    if (showExportResultDialog) {
        Dialog(onDismissRequest = { showExportResultDialog = false }) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF0F1221), RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorderColor, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("PGN Output", color = Color.White, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color(0x33000000), RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = pgnExportText,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(pgnExportText))
                            Toast.makeText(context, "Copied PGN to Clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlowNeonTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                        Spacer(Modifier.width(8.dp))
                        Text("Copy to Clipboard", color = Color.Black)
                    }
                    TextButton(onClick = { showExportResultDialog = false }) {
                        Text("Done", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun ClockCard(
    playerName: String,
    timeMs: Long,
    isActive: Boolean,
    badgeColor: Color,
    isClockEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    // Dynamic pulsing alpha animation for ticking clock
    val infiniteTransition = rememberInfiniteTransition()
    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val mins = (timeMs / 1000) / 60
    val secs = (timeMs / 1000) % 60
    val formattedTimeStr = String.format(Locale.US, "%02d:%02d", mins, secs)

    Box(
        modifier = modifier
            .background(
                if (isActive) Color(0x2213D5FF) else DarkGlassBackground,
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                if (isActive) badgeColor else GlassBorderColor,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Green pulsing dot for active ticking turn
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF00FF66), CircleShape)
                            .scale(pulsingAlpha)
                    )
                }
                Text(
                    text = playerName,
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isClockEnabled) formattedTimeStr else "∞",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isActive) Color.White else Color.Gray,
                style = MaterialTheme.typography.displaySmall.copy(
                    shadow = if (isActive) Shadow(color = badgeColor, blurRadius = 8f) else null
                )
            )
        }
    }
}

@Composable
fun EvaluationScoreBar(scorePct: Float) {
    // Score visual slider bar. 100% White advantage (Gold Glow), 0% Black advantage (Midnight black)
    val animatedScore by animateFloatAsState(
        targetValue = scorePct,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "score"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF262A40))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // White advantage portion (left side representation)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(animatedScore.coerceIn(0.01f, 0.99f))
                    .background(Color.White)
            )
            // Black advantage portion
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight((1f - animatedScore).coerceIn(0.01f, 0.99f))
                    .background(Color(0xFF0A0D14))
            )
        }
        // Equivalence midpoint vertical dash line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .align(Alignment.Center)
                .background(Color.Gray)
        )
    }
}

@Composable
fun ChessBoardGrid(
    state: ChessUiState,
    isFlipped: Boolean,
    onSquareClick: (Square) -> Unit
) {
    // Generate board coordinates mapped 0..7
    val rows = if (isFlipped) (0..7).toList() else (0..7).toList()
    val cols = if (isFlipped) (7 downTo 0).toList() else (0..7).toList()

    Column(modifier = Modifier.fillMaxSize()) {
        for (r in rows) {
            val actualRow = if (isFlipped) 7 - r else r
            Row(modifier = Modifier.weight(1f)) {
                for (c in cols) {
                    val actualCol = c
                    val square = Square(actualRow, actualCol)
                    val p = state.boardState.getPiece(square)

                    // Decide cell colors based on theme configurations
                    val isLight = (actualRow + actualCol) % 2 == 0
                    val baseColor = when (state.boardTheme) {
                        BoardTheme.CLASSIC -> if (isLight) Color(0xFFF0D9B5) else Color(0xFFB58863)
                        BoardTheme.COSMIC_GLASS -> if (isLight) Color(0xFF2E3E5C) else Color(0xFF141F36)
                        BoardTheme.FOREST -> if (isLight) Color(0xFFE0E0C0) else Color(0xFF70A070)
                        BoardTheme.CRIMSON_VELVET -> if (isLight) Color(0xFFE8D0C0) else Color(0xFF8B122B)
                    }

                    // Check overlay status highlighters
                    val isSelected = state.selectedSquare == square
                    val isLegalDest = state.legalMovesForSelected.any { m -> m.to == square }
                    val isCheckedKing = p?.type == PieceType.KING && p.color == state.boardState.activeColor && ChessEngine.isKingInCheck(state.boardState, p.color)
                    val isSuggestionHint = state.activeHintMove?.from == square || state.activeHintMove?.to == square

                    // Glow background brushes
                    val finalModifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .drawBehind {
                            drawRect(color = baseColor)

                            // Highlight suggested move overlays
                            if (isSuggestionHint) {
                                drawRect(color = Color(0x3A00FFFF))
                            }
                            // Highlight selection square aura
                            if (isSelected) {
                                drawRect(color = Color(0x5500FF66))
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current
                        ) {
                            onSquareClick(square)
                        }

                    Box(
                        modifier = finalModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        // Checking glow borders
                        if (isCheckedKing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x66FF003C))
                                    .border(2.dp, Color.Red)
                            )
                        }

                        // Piece text rendering
                        if (p != null) {
                            val pieceColor = if (p.color == ChessColor.WHITE) Color(0xFFFFFDF0) else Color(0xFF111215)
                            val pieceGlowColor = if (p.color == ChessColor.WHITE) Color(0x99FFFFFF) else Color(255, 60, 100, 150)

                            Text(
                                text = p.getSymbol(),
                                fontSize = 34.sp,
                                color = pieceColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    shadow = Shadow(
                                        color = pieceGlowColor,
                                        offset = Offset(0f, 0f),
                                        blurRadius = 12f
                                    )
                                )
                            )
                        }

                        // Legal Move Destination indicators Dot
                        if (isLegalDest) {
                            val isCapture = p != null
                            Box(
                                modifier = Modifier
                                    .size(if (isCapture) 28.dp else 12.dp)
                                    .border(
                                        width = if (isCapture) 3.dp else 0.dp,
                                        color = if (isCapture) Color(0xFFFF007F) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .background(
                                        if (isCapture) Color.Transparent else Color(0xFF00FF66),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTabContent(viewModel: ChessViewModel, state: ChessUiState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Mode picker
        Text("Game Mode Selection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modes = listOf(
                GameMode.PVAI to "vs Computer",
                GameMode.PVP to "Pass & Play",
                GameMode.AIVAI to "AI vs AI"
            )
            for ((mode, label) in modes) {
                Button(
                    onClick = { viewModel.selectGameMode(mode) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.gameMode == mode) GlowNeonTeal else DarkSteelGrey
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (state.gameMode == mode) Color.Black else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Active Player Color chooser for PvAI Mode
        if (state.gameMode == GameMode.PVAI) {
            Text("Your Starting Side", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.selectUserColor(ChessColor.WHITE) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.userColor == ChessColor.WHITE) GlowNeonTeal else DarkSteelGrey
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Play as White",
                        color = if (state.userColor == ChessColor.WHITE) Color.Black else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = { viewModel.selectUserColor(ChessColor.BLACK) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.userColor == ChessColor.BLACK) GlowNeonTeal else DarkSteelGrey
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Play as Black",
                        color = if (state.userColor == ChessColor.BLACK) Color.Black else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Clock / Time controls setup menu
        Text("Chess Clock Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            val choices = TimeControl.values()
            items(choices) { choice ->
                Button(
                    onClick = { viewModel.selectTimeControl(choice) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.timeControl == choice) GlowNeonTeal else DarkSteelGrey
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = choice.label,
                        color = if (state.timeControl == choice) Color.Black else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // AI opponent difficulty list selection
        if (state.gameMode == GameMode.PVAI || state.gameMode == GameMode.AIVAI) {
            Text("AI Intelligence (Elo)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val levels = Difficulty.values()
                for (lvl in levels) {
                    Button(
                        onClick = { viewModel.selectDifficulty(lvl) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.difficulty == lvl) GlowNeonTeal else DarkSteelGrey
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = lvl.nameStr.split(" ")[0] + "\n(${lvl.elo})",
                            color = if (state.difficulty == lvl) Color.Black else Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Divider(color = GlassBorderColor)

        // Custom Board Styling themes selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Applet Themes Picker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val themes = BoardTheme.values()
            for (th in themes) {
                Button(
                    onClick = { viewModel.selectBoardTheme(th) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.boardTheme == th) GlowNeonTeal else DarkSteelGrey
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = th.label.split(" ").last(),
                        color = if (state.boardTheme == th) Color.Black else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Divider(color = GlassBorderColor)

        // Resign / Restart primary actions controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.setupNewGame() },
                colors = ButtonDefaults.buttonColors(containerColor = GlowNeonVibe),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.RestartAlt, contentDescription = "Restart")
                Spacer(Modifier.width(6.dp))
                Text("Restart", color = Color.White, fontWeight = FontWeight.Bold)
            }
            if (!state.isCompleted) {
                Button(
                    onClick = { viewModel.resignGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.OutlinedFlag, contentDescription = "Resign Game")
                    Spacer(Modifier.width(6.dp))
                    Text("Resign", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Local SQLite Saving game action
        Button(
            onClick = { viewModel.saveGameLocally() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Save, contentDescription = "Save Game Local")
            Spacer(Modifier.width(8.dp))
            Text("Save Current Board to Storage", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NotationTabContent(
    viewModel: ChessViewModel,
    state: ChessUiState,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Match Logs History", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onImportClick, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Filled.FileUpload, contentDescription = "Import", tint = GlowNeonTeal, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Import PGN", color = GlowNeonTeal, fontSize = 12.sp)
                }
                TextButton(onClick = onExportClick, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Filled.FileDownload, contentDescription = "Export", tint = GlowNeonTeal, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export PGN", color = GlowNeonTeal, fontSize = 12.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0x33000000), RoundedCornerShape(8.dp))
                .border(1.dp, GlassBorderColor, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            if (state.moveHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No moves logged yet. Start a match!", color = Color.DarkGray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.moveHistory) { line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = line,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedGamesTabContent(viewModel: ChessViewModel, state: ChessUiState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Saved Positions Database", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

        if (state.savedGamesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved games found. Click the save button to record yours!",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.savedGamesList) { game ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x11FFFFFF), RoundedCornerShape(8.dp))
                            .border(1.dp, GlassBorderColor, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = game.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            val formattedDate = SimpleDateFormat("LLL dd, yyyy HH:mm", Locale.US).format(Date(game.timestamp))
                            Text(
                                text = formattedDate,
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Engine layout: ${game.result}",
                                color = GlowNeonTeal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.loadSavedGame(game) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Load Session", tint = GlowNeonTeal)
                            }
                            IconButton(
                                onClick = { viewModel.deleteSavedGame(game.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Game Entry", tint = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        }
    }
}

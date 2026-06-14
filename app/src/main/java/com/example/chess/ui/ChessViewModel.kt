package com.example.chess.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.chess.data.ChessDatabase
import com.example.chess.data.ChessGameRepository
import com.example.chess.data.SavedChessGame
import com.example.chess.engine.*
import com.example.chess.utils.SoundEffects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GameMode { PVP, PVAI, AIVAI }

enum class TimeControl(val label: String, val minutes: Int, val incrementSeconds: Int) {
    BULLET_1_0("1 min (Bullet)", 1, 0),
    BLITZ_3_2("3+2 (Blitz)", 3, 2),
    RAPID_10_0("10 min (Rapid)", 10, 0),
    CLASSICAL_30_0("30 min (Classical)", 30, 0),
    UNLIMITED("No Clock", 9999, 0)
}

enum class BoardTheme(val label: String) {
    CLASSIC("Classic Walnut"),
    COSMIC_GLASS("Cosmic Slate"),
    FOREST("Emerald Grove"),
    CRIMSON_VELVET("Crimson Velvet")
}

data class ChessUiState(
    val boardState: BoardState = BoardState.fromFen(BoardState.START_FEN),
    val selectedSquare: Square? = null,
    val legalMovesForSelected: List<ChessMove> = emptyList(),
    val moveHistory: List<String> = emptyList(),
    val moveDetailedHistory: List<ChessMove> = emptyList(),
    val gameMode: GameMode = GameMode.PVAI,
    val difficulty: Difficulty = Difficulty.INTERMEDIATE,
    val userColor: ChessColor = ChessColor.WHITE,
    val isCompleted: Boolean = false,
    val resultMessage: String = "",
    val openingName: String? = null,
    val isAiThinking: Boolean = false,
    val whiteTimeLeftMs: Long = 10 * 60 * 1000L,
    val blackTimeLeftMs: Long = 10 * 60 * 1000L,
    val timeControl: TimeControl = TimeControl.RAPID_10_0,
    val isClockRunning: Boolean = false,
    val boardTheme: BoardTheme = BoardTheme.COSMIC_GLASS,
    val isMuted: Boolean = false,
    val selectedPieceStyle: String = "Neon Outline",
    val evaluationScore: Float = 0.5f, // 0.0 (Black advantage) to 1.0 (White advantage)
    val evaluationCentipawns: Int = 0,
    val activeHintMove: ChessMove? = null,
    val promotionPending: Pair<Square, Square>? = null, // from, to
    val isAnalyzeMode: Boolean = false,
    val savedGamesList: List<SavedChessGame> = emptyList()
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        ChessDatabase::class.java, "chess_game_database"
    ).fallbackToDestructiveMigration().build()

    private val repository = ChessGameRepository(db.savedGameDao())

    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    // Undo/Redo stacks
    private val undoStack = ArrayList<BoardState>()
    private val redoStack = ArrayList<BoardState>()
    private val moveHistoryUndoStack = ArrayList<List<String>>()
    private val moveDetailedHistoryUndoStack = ArrayList<List<ChessMove>>()

    private var clockJob: Job? = null
    private var aiJob: Job? = null

    init {
        // Load Saved Games from SQLite Database on init
        viewModelScope.launch {
            repository.allGames.collectLatest { games ->
                _uiState.value = _uiState.value.copy(savedGamesList = games)
            }
        }
        setupNewGame()
    }

    fun selectPieceStyle(style: String) {
        _uiState.value = _uiState.value.copy(selectedPieceStyle = style)
    }

    fun selectBoardTheme(theme: BoardTheme) {
        _uiState.value = _uiState.value.copy(boardTheme = theme)
    }

    fun selectTimeControl(tc: TimeControl) {
        _uiState.value = _uiState.value.copy(
            timeControl = tc,
            whiteTimeLeftMs = tc.minutes * 60 * 1000L,
            blackTimeLeftMs = tc.minutes * 60 * 1000L,
            isClockRunning = false
        )
        if (tc != TimeControl.UNLIMITED) {
            startClock()
        } else {
            stopClockOnly()
        }
    }

    fun toggleMute() {
        val nextMuted = !_uiState.value.isMuted
        _uiState.value = _uiState.value.copy(isMuted = nextMuted)
        SoundEffects.setMuted(nextMuted)
    }

    fun toggleAnalyzeMode() {
        val nextVal = !_uiState.value.isAnalyzeMode
        _uiState.value = _uiState.value.copy(
            isAnalyzeMode = nextVal,
            activeHintMove = null
        )
        if (nextVal) {
            triggerAnalysisHint()
        }
    }

    fun requestHint() {
        triggerAnalysisHint()
    }

    private fun triggerAnalysisHint() {
        val state = _uiState.value.boardState
        val diff = _uiState.value.difficulty
        viewModelScope.launch(Dispatchers.Default) {
            val (move, _) = ChessAI.getBestMove(state, diff)
            move?.let {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(activeHintMove = it)
                }
            }
        }
    }

    fun selectGameMode(mode: GameMode) {
        _uiState.value = _uiState.value.copy(gameMode = mode)
        setupNewGame()
    }

    fun selectDifficulty(diff: Difficulty) {
        _uiState.value = _uiState.value.copy(difficulty = diff)
        if (_uiState.value.gameMode == GameMode.PVAI && _uiState.value.isClockRunning) {
            triggerAiMoveIfNeeded()
        }
    }

    fun selectUserColor(color: ChessColor) {
        _uiState.value = _uiState.value.copy(userColor = color)
        setupNewGame()
    }

    fun setupNewGame() {
        stopClockOnly()
        aiJob?.cancel()

        val tc = _uiState.value.timeControl
        val startState = BoardState.fromFen(BoardState.START_FEN)

        undoStack.clear()
        redoStack.clear()
        moveHistoryUndoStack.clear()
        moveDetailedHistoryUndoStack.clear()

        _uiState.value = _uiState.value.copy(
            boardState = startState,
            selectedSquare = null,
            legalMovesForSelected = emptyList(),
            moveHistory = emptyList(),
            moveDetailedHistory = emptyList(),
            isCompleted = false,
            resultMessage = "",
            openingName = null,
            isAiThinking = false,
            whiteTimeLeftMs = tc.minutes * 60 * 1000L,
            blackTimeLeftMs = tc.minutes * 60 * 1000L,
            isClockRunning = false,
            activeHintMove = null,
            promotionPending = null,
            evaluationScore = 0.5f,
            evaluationCentipawns = 0
        )

        val isClockActive = tc != TimeControl.UNLIMITED
        _uiState.value = _uiState.value.copy(isClockRunning = isClockActive)
        if (isClockActive) {
            startClock()
        }

        evalPosition()
        triggerAiMoveIfNeeded()
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(100)
                val curState = _uiState.value
                if (!curState.isClockRunning || curState.isCompleted || curState.timeControl == TimeControl.UNLIMITED) continue

                val whiteTime = curState.whiteTimeLeftMs
                val blackTime = curState.blackTimeLeftMs

                if (curState.boardState.activeColor == ChessColor.WHITE) {
                    val nextWhite = Math.max(0L, whiteTime - 100)
                    if (nextWhite <= 0L) {
                        launch(Dispatchers.Main) {
                            flagFall(ChessColor.WHITE)
                        }
                    }
                    _uiState.value = _uiState.value.copy(whiteTimeLeftMs = nextWhite)
                } else {
                    val nextBlack = Math.max(0L, blackTime - 100)
                    if (nextBlack <= 0L) {
                        launch(Dispatchers.Main) {
                            flagFall(ChessColor.BLACK)
                        }
                    }
                    _uiState.value = _uiState.value.copy(blackTimeLeftMs = nextBlack)
                }
            }
        }
    }

    private fun flagFall(losingColor: ChessColor) {
        stopClockOnly()
        val winningMsg = if (losingColor == ChessColor.WHITE) "Black wins on time!" else "White wins on time!"
        _uiState.value = _uiState.value.copy(
            isCompleted = true,
            resultMessage = winningMsg
        )
        SoundEffects.playGameOver()
    }

    private fun stopClockOnly() {
        clockJob?.cancel()
        _uiState.value = _uiState.value.copy(isClockRunning = false)
    }

    fun handleSquareClick(square: Square) {
        val state = _uiState.value
        if (state.isCompleted || state.isAiThinking) return

        // 1. Check if AI's turn
        if (isAiTurnNow()) return

        val board = state.boardState
        val clickedPiece = board.getPiece(square)

        val selected = state.selectedSquare
        if (selected == null) {
            // Pick piece
            if (clickedPiece != null && clickedPiece.color == board.activeColor) {
                val legalMoves = ChessEngine.generateLegalMoves(board).filter { m -> m.from == square }
                _uiState.value = _uiState.value.copy(
                    selectedSquare = square,
                    legalMovesForSelected = legalMoves
                )
            }
        } else {
            // Find if move is legal
            val requestedMove = state.legalMovesForSelected.firstOrNull { m -> m.to == square }
            if (requestedMove != null) {
                // Check Pawn promotion row
                val p = requestedMove.piece
                val isPromo = p.type == PieceType.PAWN && (square.row == 0 || square.row == 7)
                if (isPromo) {
                    // Trigger promotion choice flow
                    _uiState.value = _uiState.value.copy(promotionPending = selected to square)
                } else {
                    executeMove(requestedMove)
                }
            } else {
                // Clicked other piece of same color -> Select that one instead
                if (clickedPiece != null && clickedPiece.color == board.activeColor) {
                    val legalMoves = ChessEngine.generateLegalMoves(board).filter { m -> m.from == square }
                    _uiState.value = _uiState.value.copy(
                        selectedSquare = square,
                        legalMovesForSelected = legalMoves
                    )
                } else {
                    // Clicked empty/opponent -> Deselect
                    _uiState.value = _uiState.value.copy(
                        selectedSquare = null,
                        legalMovesForSelected = emptyList()
                    )
                }
            }
        }
    }

    fun selectPromotion(type: PieceType) {
        val pending = _uiState.value.promotionPending ?: return
        val from = pending.first
        val to = pending.second
        val activeMoves = _uiState.value.legalMovesForSelected
        val matchingMove = activeMoves.firstOrNull { m -> m.from == from && m.to == to && m.promotionType == type }
        if (matchingMove != null) {
            _uiState.value = _uiState.value.copy(promotionPending = null)
            executeMove(matchingMove)
        }
    }

    fun cancelPromotion() {
        _uiState.value = _uiState.value.copy(
            promotionPending = null,
            selectedSquare = null,
            legalMovesForSelected = emptyList()
        )
    }

    private fun executeMove(move: ChessMove) {
        // Push undo states
        undoStack.add(_uiState.value.boardState.copy())
        moveHistoryUndoStack.add(_uiState.value.moveHistory.toList())
        moveDetailedHistoryUndoStack.add(_uiState.value.moveDetailedHistory.toList())
        redoStack.clear()

        // 1. Play sound
        if (move.capturedPiece != null || move.isEnPassant) {
            SoundEffects.playCapture()
        } else {
            SoundEffects.playMove()
        }

        val previousState = _uiState.value.boardState
        val nextState = previousState.makeMove(move)

        // Increment or increment clock
        addTimeIncrement(previousState.activeColor)

        // Compile Move History List
        val algebraicList = _uiState.value.moveHistory.toMutableList()
        val detailedList = _uiState.value.moveDetailedHistory.toMutableList()

        val isCheck = ChessEngine.isKingInCheck(nextState, nextState.activeColor)
        val nextLegal = ChessEngine.generateLegalMoves(nextState)
        val isMated = isCheck && nextLegal.isEmpty()

        val pieceSanNotation = move.toSan(
            allLegalMoves = ChessEngine.generateLegalMoves(previousState),
            isCheck = isCheck,
            isMated = isMated
        )

        // Combine full PGN steps
        if (previousState.activeColor == ChessColor.WHITE) {
            algebraicList.add("${previousState.fullMoveNumber}. $pieceSanNotation")
        } else {
            if (algebraicList.isNotEmpty()) {
                val lastIdx = algebraicList.size - 1
                algebraicList[lastIdx] = algebraicList[lastIdx] + " " + pieceSanNotation
            } else {
                algebraicList.add("1... $pieceSanNotation")
            }
        }
        detailedList.add(move)

        _uiState.value = _uiState.value.copy(
            boardState = nextState,
            selectedSquare = null,
            legalMovesForSelected = emptyList(),
            moveHistory = algebraicList,
            moveDetailedHistory = detailedList,
            activeHintMove = null
        )

        // Check for Game Over conditions
        checkGameOver(nextState, nextLegal)

        evalPosition()
        detectOpening()

        if (!_uiState.value.isCompleted) {
            // Play check sound if active check
            if (isCheck) {
                SoundEffects.playCheck()
            }
            triggerAiMoveIfNeeded()
        }
    }

    private fun addTimeIncrement(color: ChessColor) {
        val inc = _uiState.value.timeControl.incrementSeconds * 1000L
        if (inc <= 0) return
        if (color == ChessColor.WHITE) {
            _uiState.value = _uiState.value.copy(whiteTimeLeftMs = _uiState.value.whiteTimeLeftMs + inc)
        } else {
            _uiState.value = _uiState.value.copy(blackTimeLeftMs = _uiState.value.blackTimeLeftMs + inc)
        }
    }

    private fun checkGameOver(state: BoardState, legalMoves: List<ChessMove>) {
        val inCheck = ChessEngine.isKingInCheck(state, state.activeColor)

        if (legalMoves.isEmpty()) {
            _uiState.value = _uiState.value.copy(isCompleted = true)
            stopClockOnly()
            if (inCheck) {
                // Checkmate
                val winner = if (state.activeColor == ChessColor.WHITE) "Black wins by Checkmate!" else "White wins by Checkmate!"
                _uiState.value = _uiState.value.copy(resultMessage = winner)
                SoundEffects.playCheckmate()
            } else {
                // Stalemate
                _uiState.value = _uiState.value.copy(resultMessage = "Draw by Stalemate")
                SoundEffects.playGameOver()
            }
            return
        }

        // Draw by Fifty-Move Rule (100 plies)
        if (state.halfMoveClock >= 100) {
            _uiState.value = _uiState.value.copy(
                isCompleted = true,
                resultMessage = "Draw by 50-move rule"
            )
            stopClockOnly()
            SoundEffects.playGameOver()
            return
        }

        // Draw by Insufficient Material
        if (ChessEngine.isInsufficientMaterial(state)) {
            _uiState.value = _uiState.value.copy(
                isCompleted = true,
                resultMessage = "Draw by Insufficient Material"
            )
            stopClockOnly()
            SoundEffects.playGameOver()
            return
        }
    }

    private fun triggerAiMoveIfNeeded() {
        val state = _uiState.value
        if (state.isCompleted || state.isAiThinking) return
        if (!isAiTurnNow()) {
            if (state.isAnalyzeMode) {
                triggerAnalysisHint()
            }
            return
        }

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiThinking = true)
            // Add visual breathing space so user sees AI "thinking"
            delay(600)

            val currentBoard = _uiState.value.boardState
            val diff = _uiState.value.difficulty

            val bestMoveResult = withContext(Dispatchers.Default) {
                ChessAI.getBestMove(currentBoard, diff)
            }

            bestMoveResult.first?.let { aiMove ->
                executeMove(aiMove)
            }

            _uiState.value = _uiState.value.copy(isAiThinking = false)

            if (_uiState.value.isAnalyzeMode) {
                triggerAnalysisHint()
            }
        }
    }

    private fun isAiTurnNow(): Boolean {
        val state = _uiState.value
        return when (state.gameMode) {
            GameMode.PVP -> false
            GameMode.PVAI -> {
                val isAiWhite = state.userColor == ChessColor.BLACK
                val active = state.boardState.activeColor
                (isAiWhite && active == ChessColor.WHITE) || (!isAiWhite && active == ChessColor.BLACK)
            }
            GameMode.AIVAI -> true
        }
    }

    private fun evalPosition() {
        val rawScore = ChessAI.evaluateBoard(_uiState.value.boardState)
        // Normalize centipawns to percentage: score values map roughly [-1000..1000] to [0.0..1.0]
        val clampedVal = Math.max(-1500f, Math.min(1500f, rawScore.toFloat()))
        val pct = (clampedVal + 1500f) / 3000f
        _uiState.value = _uiState.value.copy(
            evaluationScore = pct,
            evaluationCentipawns = rawScore
        )
    }

    private fun detectOpening() {
        val movesList = _uiState.value.moveHistory
        if (movesList.isEmpty()) return

        // Format moves list as a single continuous string
        val joined = movesList.flatMap { step ->
            step.split("\\s+".toRegex()).filter { !it.contains('.') }
        }.joinToString(" ")

        val openingStr = ChessAI.getOpeningName(joined)
        if (openingStr != null) {
            _uiState.value = _uiState.value.copy(openingName = openingStr)
        }
    }

    // --- ACTIONS: UNDO, REDO, RESIGN, EXPORTS ---
    fun undoMove() {
        if (undoStack.isEmpty() || _uiState.value.isAiThinking) return
        aiJob?.cancel()
        _uiState.value = _uiState.value.copy(isAiThinking = false)

        // Save for redo
        redoStack.add(_uiState.value.boardState.copy())

        val prevBoard = undoStack.removeAt(undoStack.size - 1)
        val prevMoves = moveHistoryUndoStack.removeAt(moveHistoryUndoStack.size - 1)
        val prevDetailed = moveDetailedHistoryUndoStack.removeAt(moveDetailedHistoryUndoStack.size - 1)

        _uiState.value = _uiState.value.copy(
            boardState = prevBoard,
            moveHistory = prevMoves,
            moveDetailedHistory = prevDetailed,
            selectedSquare = null,
            legalMovesForSelected = emptyList(),
            isCompleted = false,
            resultMessage = "",
            activeHintMove = null
        )

        // If playing AI, undoing 1 move makes it AI's turn again. We should undo 2 steps so player gets their turn back!
        if (_uiState.value.gameMode == GameMode.PVAI && undoStack.isNotEmpty()) {
            val prevBoard2 = undoStack.removeAt(undoStack.size - 1)
            val prevMoves2 = moveHistoryUndoStack.removeAt(moveHistoryUndoStack.size - 1)
            val prevDetailed2 = moveDetailedHistoryUndoStack.removeAt(moveDetailedHistoryUndoStack.size - 1)

            _uiState.value = _uiState.value.copy(
                boardState = prevBoard2,
                moveHistory = prevMoves2,
                moveDetailedHistory = prevDetailed2
            )
        }

        evalPosition()
        detectOpening()
        triggerAiMoveIfNeeded()
    }

    fun resignGame() {
        if (_uiState.value.isCompleted) return
        val active = _uiState.value.boardState.activeColor
        val resignMsg = if (active == ChessColor.WHITE) "White resigns. Black wins!" else "Black resigns. White wins!"
        _uiState.value = _uiState.value.copy(
            isCompleted = true,
            resultMessage = resignMsg
        )
        stopClockOnly()
        SoundEffects.playGameOver()
    }

    fun getPgnString(): String {
        val sb = StringBuilder()
        sb.append("[Event \"Nandi Chess Practice\"]\n")
        sb.append("[Site \"AI Studio Emulator\"]\n")
        val timestamp = java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.US).format(java.util.Date())
        sb.append("[Date \"$timestamp\"]\n")
        sb.append("[Round \"1\"]\n")
        val mode = _uiState.value.gameMode.name
        val diff = _uiState.value.difficulty.nameStr
        sb.append("[White \"${if (mode == "PVAI" && _uiState.value.userColor == ChessColor.BLACK) "Nandi AI ($diff)" else "Human Player"}\"]\n")
        sb.append("[Black \"${if (mode == "PVAI" && _uiState.value.userColor == ChessColor.WHITE) "Nandi AI ($diff)" else "Human Player"}\"]\n")

        val resultStr = if (_uiState.value.isCompleted) {
            val msg = _uiState.value.resultMessage
            when {
                msg.contains("White wins") -> "1-0"
                msg.contains("Black wins") -> "0-1"
                else -> "1/2-1/2"
            }
        } else {
            "*"
        }
        sb.append("[Result \"$resultStr\"]\n\n")

        // Append moves
        val moves = _uiState.value.moveHistory
        for (m in moves) {
            sb.append(m).append(" ")
        }
        sb.append(resultStr)
        return sb.toString()
    }

    fun importPgn(pgn: String) {
        // Simple PGN parse helper
        // Looks for numbers like "1. e4 e5" or bare tokens
        try {
            val cleaned = pgn.replace("\\[.*?]".toRegex(), "").trim()
            val tokens = cleaned.split("\\s+".toRegex()).filter { token ->
                token.isNotEmpty() && !token.contains('.') && token != "1-0" && token != "0-1" && token != "1/2-1/2" && token != "*"
            }

            setupNewGame()
            viewModelScope.launch(Dispatchers.Default) {
                for (token in tokens) {
                    val activeBoard = _uiState.value.boardState
                    val legal = ChessEngine.generateLegalMoves(activeBoard)
                    // Match token
                    val matchedMove = legal.firstOrNull { m ->
                        val promoCheck = m.promotionType != null
                        val san = m.toSan(
                            allLegalMoves = ChessEngine.generateLegalMoves(activeBoard),
                            isCheck = ChessEngine.isKingInCheck(activeBoard.makeMove(m), if (activeBoard.activeColor == ChessColor.WHITE) ChessColor.BLACK else ChessColor.WHITE),
                            isMated = false
                        )
                        // Normalize token
                        val clearSan = san.replace("[+#]".toRegex(), "")
                        val clearTok = token.replace("[+#]".toRegex(), "")
                        clearSan == clearTok || m.from.toAlgebraic() + m.to.toAlgebraic() == clearTok
                    }

                    if (matchedMove != null) {
                        withContext(Dispatchers.Main) {
                            executeMove(matchedMove)
                        }
                        delay(250) // visual play interval
                    } else {
                        break // invalid move in PGN string found
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- SQL DATABASE PERSISTENCE ACTIONS ---
    fun saveGameLocally() {
        val state = _uiState.value
        val titleStr = "Nandi Chess - " + when (state.gameMode) {
            GameMode.PVP -> sFormat("Pass & Play (vs Human)")
            GameMode.PVAI -> sFormat("Human vs AI (%s)", state.difficulty.nameStr)
            GameMode.AIVAI -> sFormat("AI vs AI (%s)", state.difficulty.nameStr)
        }

        val gameEntity = SavedChessGame(
            title = titleStr,
            fen = state.boardState.toFen(),
            pgn = getPgnString(),
            gameMode = state.gameMode.name,
            difficulty = state.difficulty.name,
            whiteName = if (state.gameMode == GameMode.PVAI && state.userColor == ChessColor.BLACK) "AI Opponent" else "Player 1",
            blackName = if (state.gameMode == GameMode.PVAI && state.userColor == ChessColor.WHITE) "AI Opponent" else "Player 2",
            result = if (state.isCompleted) state.resultMessage else "Remaining Clocks",
            whiteTimeLeftMs = state.whiteTimeLeftMs,
            blackTimeLeftMs = state.blackTimeLeftMs,
            isCompleted = state.isCompleted
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveGame(gameEntity)
        }
    }

    fun loadSavedGame(game: SavedChessGame) {
        stopClockOnly()
        aiJob?.cancel()

        val board = BoardState.fromFen(game.fen)
        val mode = GameMode.valueOf(game.gameMode)
        val diff = Difficulty.valueOf(game.difficulty)

        // Compile move history from PGN string
        val movesList = ArrayList<String>()
        val pgnLines = game.pgn.split("\n")
        val movesLine = pgnLines.lastOrNull { it.trim().isNotEmpty() && !it.startsWith("[") } ?: ""
        if (movesLine.isNotEmpty()) {
            val tokens = movesLine.split("\\s+".toRegex())
            var currentTurnString = ""
            for (t in tokens) {
                if (t.contains(".")) {
                    if (currentTurnString.isNotEmpty()) {
                        movesList.add(currentTurnString.trim())
                    }
                    currentTurnString = t + " "
                } else if (t != "1-0" && t != "0-1" && t != "1/2-1/2" && t != "*") {
                    currentTurnString += "$t "
                }
            }
            if (currentTurnString.isNotEmpty() && currentTurnString != "1-0" && currentTurnString != "0-1") {
                movesList.add(currentTurnString.trim())
            }
        }

        _uiState.value = _uiState.value.copy(
            boardState = board,
            gameMode = mode,
            difficulty = diff,
            whiteTimeLeftMs = game.whiteTimeLeftMs,
            blackTimeLeftMs = game.blackTimeLeftMs,
            moveHistory = movesList,
            selectedSquare = null,
            legalMovesForSelected = emptyList(),
            isCompleted = game.isCompleted,
            resultMessage = if (game.isCompleted) game.result else "",
            isClockRunning = !game.isCompleted && _uiState.value.timeControl != TimeControl.UNLIMITED,
            activeHintMove = null
        )

        if (_uiState.value.isClockRunning) {
            startClock()
        }

        evalPosition()
        detectOpening()
        triggerAiMoveIfNeeded()
    }

    fun deleteSavedGame(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGame(id)
        }
    }

    // String formatting tool to support older Android version bounds cleanly
    private fun sFormat(format: String, vararg args: Any): String {
        return java.lang.String.format(java.util.Locale.US, format, *args)
    }
}

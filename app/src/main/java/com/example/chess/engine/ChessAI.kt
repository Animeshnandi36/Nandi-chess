package com.example.chess.engine

import java.util.Random

enum class Difficulty(val nameStr: String, val elo: Int) {
    BEGINNER("Beginner (500 ELO)", 500),
    INTERMEDIATE("Intermediate (1200 ELO)", 1200),
    ADVANCED("Advanced (1800 ELO)", 1800),
    EXPERT("Expert (2500+ ELO)", 2500)
}

object ChessAI {
    private val random = Random()

    // --- POSITIONAL EVALUATION (PIECE SQUARE TABLES) ---
    // Higher is better for white, lower is better for black.
    // Index mapping: row 0 to 7 (top to bottom).
    private val pawnPst = arrayOf(
        intArrayOf(  0,  0,  0,  0,  0,  0,  0,  0),
        intArrayOf( 50, 50, 50, 50, 50, 50, 50, 50),
        intArrayOf( 10, 10, 20, 30, 30, 20, 10, 10),
        intArrayOf(  5,  5, 10, 25, 25, 10,  5,  5),
        intArrayOf(  0,  0,  0, 20, 20,  0,  0,  0),
        intArrayOf(  5, -5,-10,  0,  0,-10, -5,  5),
        intArrayOf(  5, 10, 10,-20,-20, 10, 10,  5),
        intArrayOf(  0,  0,  0,  0,  0,  0,  0,  0)
    )

    private val knightPst = arrayOf(
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50),
        intArrayOf(-40,-20,  0,  0,  0,  0,-20,-40),
        intArrayOf(-30,  0, 10, 15, 15, 10,  0,-30),
        intArrayOf(-30,  5, 15, 20, 20, 15,  5,-30),
        intArrayOf(-30,  0, 15, 20, 20, 15,  0,-30),
        intArrayOf(-30,  5, 10, 15, 15, 10,  5,-30),
        intArrayOf(-40,-20,  0,  5,  5,  0,-20,-40),
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50)
    )

    private val bishopPst = arrayOf(
        intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20),
        intArrayOf(-10,  0,  0,  0,  0,  0,  0,-10),
        intArrayOf(-10,  0,  5, 10, 10,  5,  0,-10),
        intArrayOf(-10,  5,  5, 10, 10,  5,  5,-10),
        intArrayOf(-10,  0, 10, 10, 10, 10,  0,-10),
        intArrayOf(-10, 10, 10, 10, 10, 10, 10,-10),
        intArrayOf(-10,  5,  0,  0,  0,  0,  5,-10),
        intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20)
    )

    private val rookPst = arrayOf(
        intArrayOf(  0,  0,  0,  0,  0,  0,  0,  0),
        intArrayOf(  5, 10, 10, 10, 10, 10, 10,  5),
        intArrayOf( -5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf( -5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf( -5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf( -5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf( -5,  0,  0,  0,  0,  0,  0, -5),
        intArrayOf(  0,  0,  0,  5,  5,  0,  0,  0)
    )

    private val queenPst = arrayOf(
        intArrayOf(-20,-10,-10, -5, -5,-10,-10,-20),
        intArrayOf(-10,  0,  0,  0,  0,  0,  0,-10),
        intArrayOf(-10,  0,  5,  5,  5,  5,  0,-10),
        intArrayOf( -5,  0,  5,  5,  5,  5,  0, -5),
        intArrayOf(  0,  0,  5,  5,  5,  5,  0,  0),
        intArrayOf(-10,  5,  5,  5,  5,  5,  5,-10),
        intArrayOf(-10,  0,  5,  0,  0,  5,  0,-10),
        intArrayOf(-20,-10,-10, -5, -5,-10,-10,-20)
    )

    private val kingMiddleGamePst = arrayOf(
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-20,-30,-30,-40,-40,-30,-30,-20),
        intArrayOf(-10,-20,-20,-20,-20,-20,-20,-10),
        intArrayOf( 20, 20,  0,  0,  0,  0, 20, 20),
        intArrayOf( 20, 30, 10,  0,  0, 10, 30, 20)
    )

    // --- OPENING BOOK IMPLEMENTATION ---
    private val openingBook = mapOf(
        // Initial setup
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" to listOf("e2e4", "d2d4", "Nf3", "c2c4"),
        // 1. e4 responses
        "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1" to listOf("c7c5", "e7e5", "e7e6", "c7c6"),
        // 1. d4 responses
        "rnbqkbnr/pppppppp/8/8/3P4/8/PPPP1PPP/RNBQKBNR b KQkq d3 0 1" to listOf("d7d5", "g8f6", "e7e6"),
        // 1. e4 c5 (Sicilian)
        "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2" to listOf("g1f3", "b1c3"),
        // 1. e4 e5 (Open Game)
        "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2" to listOf("g1f3", "f2f4", "b1c3"),
        // 1. e4 e5 2. Nf3
        "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2" to listOf("b8c6", "d7d6", "g8f6"),
        // 1. e4 c5 2. Nf3
        "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 1 2" to listOf("d2d6", "e7e6", "b8c6")
    )

    private val openingNames = mapOf(
        "e4" to "The Open Game",
        "d4" to "The Queen's Pawn Opening",
        "Nf3" to "Réti Opening",
        "c4" to "English Opening",
        "e4 c5" to "Sicilian Defense",
        "e4 e5 Nf3 Nc6" to "Open Game (King's Knight)",
        "e4 e5 Nf3 Nc6 Bb5" to "Ruy Lopez (Spanish Opening)",
        "e4 e5 Nf3 Nc6 Bc4" to "Italian Game",
        "e4 e6" to "French Defense",
        "e4 c6" to "Caro-Kann Defense",
        "d4 d5 c4" to "Queen's Gambit Accepted/Declined",
        "d4 Nf6 c4 g6" to "King's Indian Defense",
        "e4 d5" to "Scandinavian Defense",
        "d4 d5 c4 c6" to "Slav Defense"
    )

    fun getOpeningName(pgn: String): String? {
        val trimmed = pgn.trim().replace("\\s+".toRegex(), " ")
        // Look up partial matches
        for ((moves, name) in openingNames) {
            if (trimmed.startsWith(moves)) {
                return name
            }
        }
        return null
    }

    // Returns a pair of: Selected Move, Evaluation score (positive is White advantage)
    fun getBestMove(state: BoardState, difficulty: Difficulty): Pair<ChessMove?, Int> {
        val legalMoves = ChessEngine.generateLegalMoves(state)
        if (legalMoves.isEmpty()) return null to evaluateBoard(state)

        // 1. Query Opening Book (Expert Difficulty preference)
        if (difficulty == Difficulty.EXPERT) {
            val fen = state.toFen().trim()
            val bookMoveStrings = openingBook.entries.firstOrNull { fen.startsWith(it.key) }?.value
            if (bookMoveStrings != null && bookMoveStrings.isNotEmpty()) {
                val bookMoveStr = bookMoveStrings.random()
                // Find matching legal move
                val matchingMove = findLegalMoveByString(legalMoves, bookMoveStr)
                if (matchingMove != null) {
                    return matchingMove to evaluateBoard(state)
                }
            }
        }

        // 2. Play styles based on Elo
        return when (difficulty) {
            Difficulty.BEGINNER -> {
                // 30% chance for random move, otherwise searches depth 1
                if (random.nextFloat() < 0.3f) {
                    legalMoves.random() to evaluateBoard(state)
                } else {
                    minimaxSearch(state, depth = 1, alpha = Int.MIN_VALUE, beta = Int.MAX_VALUE)
                }
            }
            Difficulty.INTERMEDIATE -> {
                // 10% chance for random move, otherwise depth 2
                if (random.nextFloat() < 0.10f) {
                    legalMoves.random() to evaluateBoard(state)
                } else {
                    minimaxSearch(state, depth = 2, alpha = Int.MIN_VALUE, beta = Int.MAX_VALUE)
                }
            }
            Difficulty.ADVANCED -> {
                minimaxSearch(state, depth = 3, alpha = Int.MIN_VALUE, beta = Int.MAX_VALUE)
            }
            Difficulty.EXPERT -> {
                minimaxSearch(state, depth = 4, alpha = Int.MIN_VALUE, beta = Int.MAX_VALUE)
            }
        }
    }

    private fun findLegalMoveByString(legalMoves: List<ChessMove>, moveStr: String): ChessMove? {
        // Checks e2e4 styles or Nf3 algebraic notations
        for (m in legalMoves) {
            val fromTo = m.from.toAlgebraic() + m.to.toAlgebraic()
            if (fromTo == moveStr) return m

            // Simple check of pieces
            if (m.piece.type != PieceType.PAWN) {
                val pChar = m.piece.getChar().uppercaseChar().toString()
                if (moveStr.startsWith(pChar) && moveStr.endsWith(m.to.toAlgebraic())) {
                    return m
                }
            } else if (moveStr == "e4" && m.to.toAlgebraic() == "e4") {
                return m
            } else if (moveStr == "d4" && m.to.toAlgebraic() == "d4") {
                return m
            }
        }
        return null
    }

    // --- MINIMAX SEARCH WITH ALPHA-BETA PRUNING & MOVE ORDERING ---
    private fun minimaxSearch(state: BoardState, depth: Int, alpha: Int, beta: Int): Pair<ChessMove?, Int> {
        val color = state.activeColor
        val legalMoves = ChessEngine.generateLegalMoves(state)

        if (legalMoves.isEmpty()) {
            val inCheck = ChessEngine.isKingInCheck(state, color)
            return if (inCheck) {
                // Checkmate
                val score = if (color == ChessColor.WHITE) -100000 + (4 - depth) else 100000 - (4 - depth)
                null to score
            } else {
                // Stalemate
                null to 0
            }
        }

        if (depth == 0) {
            return null to evaluateBoard(state)
        }

        // ORDER MOVES: Increases cutting speed exponentially!
        val orderedMoves = orderMoves(legalMoves)

        var bestMove: ChessMove? = null
        var currentAlpha = alpha
        var currentBeta = beta

        if (color == ChessColor.WHITE) {
            var maxEval = Int.MIN_VALUE
            for (move in orderedMoves) {
                val simulatedState = state.makeMove(move)
                val (_, eval) = minimaxSearch(simulatedState, depth - 1, currentAlpha, currentBeta)
                if (eval > maxEval) {
                    maxEval = eval
                    bestMove = move
                }
                currentAlpha = Math.max(currentAlpha, eval)
                if (currentBeta <= currentAlpha) {
                    break // Beta cutoff
                }
            }
            return bestMove to maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in orderedMoves) {
                val simulatedState = state.makeMove(move)
                val (_, eval) = minimaxSearch(simulatedState, depth - 1, currentAlpha, currentBeta)
                if (eval < minEval) {
                    minEval = eval
                    bestMove = move
                }
                currentBeta = Math.min(currentBeta, eval)
                if (currentBeta <= currentAlpha) {
                    break // Alpha cutoff
                }
            }
            return bestMove to minEval
        }
    }

    private fun orderMoves(moves: List<ChessMove>): List<ChessMove> {
        // MVV-LVA (Most Valuable Victim - Least Valuable Aggressor) for captures
        return moves.sortedByDescending { move ->
            var score = 0
            if (move.capturedPiece != null) {
                score += 10 * getPieceValue(move.capturedPiece.type) - getPieceValue(move.piece.type) / 10
            }
            if (move.promotionType != null) {
                score += 8 * getPieceValue(move.promotionType)
            }
            if (move.isCastlingKingSide || move.isCastlingQueenSide) {
                score += 40
            }
            score
        }
    }

    fun evaluateBoard(state: BoardState): Int {
        var score = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.board[r][c] ?: continue
                val isWhite = p.color == ChessColor.WHITE
                val value = getPieceValue(p.type)

                // PST Positional Values
                val pstVal = when (p.type) {
                    PieceType.PAWN -> getPstValue(pawnPst, r, c, isWhite)
                    PieceType.KNIGHT -> getPstValue(knightPst, r, c, isWhite)
                    PieceType.BISHOP -> getPstValue(bishopPst, r, c, isWhite)
                    PieceType.ROOK -> getPstValue(rookPst, r, c, isWhite)
                    PieceType.QUEEN -> getPstValue(queenPst, r, c, isWhite)
                    PieceType.KING -> getPstValue(kingMiddleGamePst, r, c, isWhite)
                }

                val totalVal = value + pstVal
                if (isWhite) {
                    score += totalVal
                } else {
                    score -= totalVal
                }
            }
        }
        return score
    }

    private fun getPieceValue(type: PieceType): Int {
        return when (type) {
            PieceType.PAWN -> 100
            PieceType.KNIGHT -> 320
            PieceType.BISHOP -> 330
            PieceType.ROOK -> 500
            PieceType.QUEEN -> 900
            PieceType.KING -> 20000
        }
    }

    private fun getPstValue(table: Array<IntArray>, row: Int, col: Int, isWhite: Boolean): Int {
        return if (isWhite) {
            // White values are straightforward, matching standard index
            table[row][col]
        } else {
            // Black values are vertically mirrored
            table[7 - row][col]
        }
    }
}

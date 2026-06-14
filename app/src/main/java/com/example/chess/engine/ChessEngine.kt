package com.example.chess.engine

import java.util.Locale

enum class ChessColor { WHITE, BLACK }

enum class PieceType { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }

data class ChessPiece(val type: PieceType, val color: ChessColor) {
    fun getSymbol(): String {
        return when (type) {
            PieceType.PAWN -> if (color == ChessColor.WHITE) "♙" else "♟"
            PieceType.KNIGHT -> if (color == ChessColor.WHITE) "♘" else "♞"
            PieceType.BISHOP -> if (color == ChessColor.WHITE) "♗" else "♝"
            PieceType.ROOK -> if (color == ChessColor.WHITE) "♖" else "♜"
            PieceType.QUEEN -> if (color == ChessColor.WHITE) "♕" else "♛"
            PieceType.KING -> if (color == ChessColor.WHITE) "♔" else "♚"
        }
    }

    fun getChar(): Char {
        val ch = when (type) {
            PieceType.PAWN -> 'P'
            PieceType.KNIGHT -> 'N'
            PieceType.BISHOP -> 'B'
            PieceType.ROOK -> 'R'
            PieceType.QUEEN -> 'Q'
            PieceType.KING -> 'K'
        }
        return if (color == ChessColor.WHITE) ch else ch.lowercaseChar()
    }

    companion object {
        fun fromChar(char: Char): ChessPiece? {
            val color = if (char.isUpperCase()) ChessColor.WHITE else ChessColor.BLACK
            val type = when (char.uppercaseChar()) {
                'P' -> PieceType.PAWN
                'N' -> PieceType.KNIGHT
                'B' -> PieceType.BISHOP
                'R' -> PieceType.ROOK
                'Q' -> PieceType.QUEEN
                'K' -> PieceType.KING
                else -> return null
            }
            return ChessPiece(type, color)
        }
    }
}

data class Square(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..7 && col in 0..7

    fun toAlgebraic(): String {
        val file = ('a' + col)
        val rank = (8 - row)
        return "$file$rank"
    }

    companion object {
        fun fromAlgebraic(alg: String): Square? {
            if (alg.length != 2) return null
            val col = alg[0] - 'a'
            val row = 8 - (alg[1] - '0')
            val sq = Square(row, col)
            return if (sq.isValid()) sq else null
        }
    }
}

data class CastlingRights(
    val whiteKingSide: Boolean = true,
    val whiteQueenSide: Boolean = true,
    val blackKingSide: Boolean = true,
    val blackQueenSide: Boolean = true
)

data class ChessMove(
    val from: Square,
    val to: Square,
    val piece: ChessPiece,
    val capturedPiece: ChessPiece? = null,
    val isCastlingKingSide: Boolean = false,
    val isCastlingQueenSide: Boolean = false,
    val isEnPassant: Boolean = false,
    val promotionType: PieceType? = null
) {
    fun toSan(allLegalMoves: List<ChessMove>, isCheck: Boolean, isMated: Boolean): String {
        if (isCastlingKingSide) return "O-O"
        if (isCastlingQueenSide) return "O-O-O"

        val sb = StringBuilder()
        if (piece.type != PieceType.PAWN) {
            sb.append(piece.getChar().uppercaseChar())
            // Disambiguator
            val duplicates = allLegalMoves.filter {
                it.to == to && it.piece.type == piece.type && it.from != from
            }
            if (duplicates.isNotEmpty()) {
                val sameFile = duplicates.any { it.from.col == from.col }
                val sameRow = duplicates.any { it.from.row == from.row }
                if (!sameFile) {
                    sb.append(('a' + from.col))
                } else if (!sameRow) {
                    sb.append((8 - from.row))
                } else {
                    sb.append(('a' + from.col))
                    sb.append((8 - from.row))
                }
            }
        } else {
            if (capturedPiece != null || isEnPassant) {
                sb.append(('a' + from.col))
            }
        }

        if (capturedPiece != null || isEnPassant) {
            sb.append('x')
        }

        sb.append(to.toAlgebraic())

        if (promotionType != null) {
            val pChar = when (promotionType) {
                PieceType.KNIGHT -> 'N'
                PieceType.BISHOP -> 'B'
                PieceType.ROOK -> 'R'
                PieceType.QUEEN -> 'Q'
                else -> 'Q'
            }
            sb.append("=$pChar")
        }

        if (isMated) {
            sb.append("#")
        } else if (isCheck) {
            sb.append("+")
        }

        return sb.toString()
    }
}

class BoardState {
    var board: Array<Array<ChessPiece?>> = Array(8) { arrayOfNulls<ChessPiece>(8) }
    var activeColor: ChessColor = ChessColor.WHITE
    var castlingRights: CastlingRights = CastlingRights()
    var enPassantTarget: Square? = null
    var halfMoveClock: Int = 0
    var fullMoveNumber: Int = 1

    companion object {
        const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun fromFen(fen: String): BoardState {
            val state = BoardState()
            val parts = fen.trim().split("\\s+".toRegex())
            if (parts.isEmpty()) return state

            // 1. Board positions
            val rows = parts[0].split('/')
            for (r in 0 until 8) {
                if (r >= rows.size) break
                val rowStr = rows[r]
                var c = 0
                for (i in 0 until rowStr.length) {
                    if (c >= 8) break
                    val char = rowStr[i]
                    if (char.isDigit()) {
                        val emptySquares = char.toString().toInt()
                        for (k in 0 until emptySquares) {
                            if (c < 8) {
                                state.board[r][c] = null
                                c++
                            }
                        }
                    } else {
                        state.board[r][c] = ChessPiece.fromChar(char)
                        c++
                    }
                }
            }

            // 2. Active player
            if (parts.size > 1) {
                state.activeColor = if (parts[1].lowercase(Locale.ROOT) == "b") ChessColor.BLACK else ChessColor.WHITE
            }

            // 3. Castling rights
            if (parts.size > 2) {
                val castStr = parts[2]
                state.castlingRights = CastlingRights(
                    whiteKingSide = castStr.contains('K'),
                    whiteQueenSide = castStr.contains('Q'),
                    blackKingSide = castStr.contains('k'),
                    blackQueenSide = castStr.contains('q')
                )
            }

            // 4. En Passant target
            if (parts.size > 3) {
                val epStr = parts[3]
                state.enPassantTarget = if (epStr == "-") null else Square.fromAlgebraic(epStr)
            }

            // 5. Halfmove clock
            if (parts.size > 4) {
                state.halfMoveClock = parts[4].toIntOrNull() ?: 0
            }

            // 6. Fullmove number
            if (parts.size > 5) {
                state.fullMoveNumber = parts[5].toIntOrNull() ?: 1
            }

            return state
        }
    }

    fun toFen(): String {
        val sb = StringBuilder()
        // 1. Pieces
        for (r in 0 until 8) {
            var emptyCount = 0
            for (c in 0 until 8) {
                val p = board[r][c]
                if (p == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(p.getChar())
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            if (r < 7) {
                sb.append('/')
            }
        }

        // 2. Active color
        sb.append(" ")
        sb.append(if (activeColor == ChessColor.WHITE) "w" else "b")

        // 3. Castling rights
        sb.append(" ")
        val castStr = StringBuilder()
        if (castlingRights.whiteKingSide) castStr.append('K')
        if (castlingRights.whiteQueenSide) castStr.append('Q')
        if (castlingRights.blackKingSide) castStr.append('k')
        if (castlingRights.blackQueenSide) castStr.append('q')
        if (castStr.isEmpty()) {
            sb.append("-")
        } else {
            sb.append(castStr.toString())
        }

        // 4. En Passant target
        sb.append(" ")
        sb.append(enPassantTarget?.toAlgebraic() ?: "-")

        // 5. Halfmove
        sb.append(" ")
        sb.append(halfMoveClock)

        // 6. Fullmove
        sb.append(" ")
        sb.append(fullMoveNumber)

        return sb.toString()
    }

    fun getPiece(sq: Square): ChessPiece? = if (sq.isValid()) board[sq.row][sq.col] else null

    fun copy(): BoardState {
        val copy = BoardState()
        for (r in 0 until 8) {
            System.arraycopy(this.board[r], 0, copy.board[r], 0, 8)
        }
        copy.activeColor = this.activeColor
        copy.castlingRights = this.castlingRights.copy()
        copy.enPassantTarget = this.enPassantTarget
        copy.halfMoveClock = this.halfMoveClock
        copy.fullMoveNumber = this.fullMoveNumber
        return copy
    }

    fun makeMove(move: ChessMove): BoardState {
        val next = this.copy()
        val from = move.from
        val to = move.to
        val p = move.piece

        next.board[from.row][from.col] = null

        // En passant capture removal
        if (move.isEnPassant) {
            val captureRow = from.row
            val captureCol = to.col
            next.board[captureRow][captureCol] = null
            next.halfMoveClock = 0
        } else if (p.type == PieceType.PAWN || move.capturedPiece != null) {
            next.halfMoveClock = 0
        } else {
            next.halfMoveClock++
        }

        // Place Piece or Promote
        if (move.promotionType != null) {
            next.board[to.row][to.col] = ChessPiece(move.promotionType, p.color)
        } else {
            next.board[to.row][to.col] = p
        }

        // Castling move execution (moving rook)
        if (move.isCastlingKingSide) {
            val rRow = if (p.color == ChessColor.WHITE) 7 else 0
            val rook = next.board[rRow][7]
            next.board[rRow][7] = null
            next.board[rRow][5] = rook
        } else if (move.isCastlingQueenSide) {
            val rRow = if (p.color == ChessColor.WHITE) 7 else 0
            val rook = next.board[rRow][0]
            next.board[rRow][0] = null
            next.board[rRow][3] = rook
        }

        // New En Passant Target calculation
        if (p.type == PieceType.PAWN && Math.abs(to.row - from.row) == 2) {
            val midRow = (from.row + to.row) / 2
            next.enPassantTarget = Square(midRow, from.col)
        } else {
            next.enPassantTarget = null
        }

        // Update Castling Rights
        var wK = next.castlingRights.whiteKingSide
        var wQ = next.castlingRights.whiteQueenSide
        var bK = next.castlingRights.blackKingSide
        var bQ = next.castlingRights.blackQueenSide

        // King moves
        if (p.type == PieceType.KING) {
            if (p.color == ChessColor.WHITE) {
                wK = false
                wQ = false
            } else {
                bK = false
                bQ = false
            }
        }

        // Rook moves
        if (p.type == PieceType.ROOK) {
            if (p.color == ChessColor.WHITE) {
                if (from.row == 7 && from.col == 7) wK = false
                if (from.row == 7 && from.col == 0) wQ = false
            } else {
                if (from.row == 0 && from.col == 7) bK = false
                if (from.row == 0 && from.col == 0) bQ = false
            }
        }

        // Rook capture updates castling rights mapping
        if (to.row == 7 && to.col == 7) wK = false
        if (to.row == 7 && to.col == 0) wQ = false
        if (to.row == 0 && to.col == 7) bK = false
        if (to.row == 0 && to.col == 0) bQ = false

        next.castlingRights = CastlingRights(wK, wQ, bK, bQ)

        // Alternate dynamic turn
        if (next.activeColor == ChessColor.BLACK) {
            next.fullMoveNumber++
        }
        next.activeColor = if (next.activeColor == ChessColor.WHITE) ChessColor.BLACK else ChessColor.WHITE

        return next
    }
}

object ChessEngine {

    fun generateLegalMoves(state: BoardState): List<ChessMove> {
        val pseudo = generatePseudoMoves(state)
        return pseudo.filter { !leavesKingInCheck(state, it) }
    }

    private fun leavesKingInCheck(state: BoardState, move: ChessMove): Boolean {
        val nextState = state.makeMove(move)
        val originalColor = state.activeColor
        return isKingInCheck(nextState, originalColor)
    }

    fun isKingInCheck(state: BoardState, color: ChessColor): Boolean {
        // Find the king of the given color
        var kingSquare: Square? = null
        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.board[r][c]
                if (p != null && p.type == PieceType.KING && p.color == color) {
                    kingSquare = Square(r, c)
                    break
                }
            }
            if (kingSquare != null) break
        }
        if (kingSquare == null) return false // fallback
        val enemyColor = if (color == ChessColor.WHITE) ChessColor.BLACK else ChessColor.WHITE
        return isSquareAttacked(kingSquare, enemyColor, state.board)
    }

    fun isSquareAttacked(square: Square, attackerColor: ChessColor, board: Array<Array<ChessPiece?>>): Boolean {
        // 1. Knight attacks
        val knightOffsets = arrayOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        for (o in knightOffsets) {
            val r = square.row + o.first
            val c = square.col + o.second
            if (r in 0..7 && c in 0..7) {
                val p = board[r][c]
                if (p != null && p.color == attackerColor && p.type == PieceType.KNIGHT) {
                    return true
                }
            }
        }

        // 2. Pawn attacks
        // If attacker is White, pawns attack towards row-1 index
        // If attacker is Black, pawns attack towards row+1 index
        val pawnRowOffset = if (attackerColor == ChessColor.WHITE) 1 else -1
        val pawnCols = arrayOf(square.col - 1, square.col + 1)
        for (pc in pawnCols) {
            val pr = square.row + pawnRowOffset
            if (pr in 0..7 && pc in 0..7) {
                val p = board[pr][pc]
                if (p != null && p.color == attackerColor && p.type == PieceType.PAWN) {
                    return true
                }
            }
        }

        // 3. King adjacent attacks
        val directions = arrayOf(
            Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
            Pair(0, -1),               Pair(0, 1),
            Pair(1, -1),  Pair(1, 0),  Pair(1, 1)
        )
        for (d in directions) {
            val r = square.row + d.first
            val c = square.col + d.second
            if (r in 0..7 && c in 0..7) {
                val p = board[r][c]
                if (p != null && p.color == attackerColor && p.type == PieceType.KING) {
                    return true
                }
            }
        }

        // 4. Sliding: Cardinal/Orthogonal (Rook & Queen)
        val orthogonalDirs = arrayOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
        for (d in orthogonalDirs) {
            var step = 1
            while (true) {
                val r = square.row + d.first * step
                val c = square.col + d.second * step
                if (r !in 0..7 || c !in 0..7) break
                val p = board[r][c]
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break // blocked by any piece
                }
                step++
            }
        }

        // 5. Sliding: Diagonal (Bishop & Queen)
        val diagonalDirs = arrayOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
        for (d in diagonalDirs) {
            var step = 1
            while (true) {
                val r = square.row + d.first * step
                val c = square.col + d.second * step
                if (r !in 0..7 || c !in 0..7) break
                val p = board[r][c]
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break // blocked
                }
                step++
            }
        }

        return false
    }

    private fun generatePseudoMoves(state: BoardState): List<ChessMove> {
        val moves = ArrayList<ChessMove>()
        val color = state.activeColor
        val enemyColor = if (color == ChessColor.WHITE) ChessColor.BLACK else ChessColor.WHITE

        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.board[r][c] ?: continue
                if (p.color != color) continue

                val from = Square(r, c)
                when (p.type) {
                    PieceType.PAWN -> generatePawnPseudoMoves(state, from, p, moves, enemyColor)
                    PieceType.KNIGHT -> generateKnightPseudoMoves(state, from, p, moves)
                    PieceType.BISHOP -> generateBishopPseudoMoves(state, from, p, moves)
                    PieceType.ROOK -> generateRookPseudoMoves(state, from, p, moves)
                    PieceType.QUEEN -> generateQueenPseudoMoves(state, from, p, moves)
                    PieceType.KING -> generateKingPseudoMoves(state, from, p, moves, enemyColor)
                }
            }
        }
        return moves
    }

    private fun generatePawnPseudoMoves(
        state: BoardState,
        from: Square,
        p: ChessPiece,
        moves: ArrayList<ChessMove>,
        enemyColor: ChessColor
    ) {
        val dir = if (p.color == ChessColor.WHITE) -1 else 1
        val startRow = if (p.color == ChessColor.WHITE) 6 else 1
        val promoRow = if (p.color == ChessColor.WHITE) 0 else 7

        // 1-step forward
        val nextRow1 = from.row + dir
        if (nextRow1 in 0..7 && state.board[nextRow1][from.col] == null) {
            val to = Square(nextRow1, from.col)
            if (nextRow1 == promoRow) {
                addPromotions(from, to, p, null, moves)
            } else {
                moves.add(ChessMove(from, to, p))
            }

            // 2-step forward from starting rank
            val nextRow2 = from.row + 2 * dir
            if (from.row == startRow && state.board[nextRow2][from.col] == null) {
                moves.add(ChessMove(from, Square(nextRow2, from.col), p))
            }
        }

        // Standard Diagonal Captures
        val captureCols = arrayOf(from.col - 1, from.col + 1)
        for (cc in captureCols) {
            if (cc in 0..7 && nextRow1 in 0..7) {
                val capPiece = state.board[nextRow1][cc]
                if (capPiece != null && capPiece.color == enemyColor) {
                    val to = Square(nextRow1, cc)
                    if (nextRow1 == promoRow) {
                        addPromotions(from, to, p, capPiece, moves)
                    } else {
                        moves.add(ChessMove(from, to, p, capPiece))
                    }
                }
            }
        }

        // En passant capture
        state.enPassantTarget?.let { epSquare ->
            if (nextRow1 == epSquare.row && Math.abs(from.col - epSquare.col) == 1) {
                val capturedPawn = state.board[from.row][epSquare.col]
                moves.add(ChessMove(from, epSquare, p, capturedPawn, isEnPassant = true))
            }
        }
    }

    private fun addPromotions(
        from: Square,
        to: Square,
        p: ChessPiece,
        captured: ChessPiece?,
        moves: ArrayList<ChessMove>
    ) {
        val promoTypes = arrayOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
        for (pt in promoTypes) {
            moves.add(ChessMove(from, to, p, captured, promotionType = pt))
        }
    }

    private fun generateKnightPseudoMoves(state: BoardState, from: Square, p: ChessPiece, moves: ArrayList<ChessMove>) {
        val offsets = arrayOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        for (o in offsets) {
            val r = from.row + o.first
            val c = from.col + o.second
            if (r in 0..7 && c in 0..7) {
                val dest = state.board[r][c]
                if (dest == null) {
                    moves.add(ChessMove(from, Square(r, c), p))
                } else if (dest.color != p.color) {
                    moves.add(ChessMove(from, Square(r, c), p, dest))
                }
            }
        }
    }

    private fun generateBishopPseudoMoves(state: BoardState, from: Square, p: ChessPiece, moves: ArrayList<ChessMove>) {
        val diagonalDirs = arrayOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
        generateSlidingPseudoMoves(state, from, p, diagonalDirs, moves)
    }

    private fun generateRookPseudoMoves(state: BoardState, from: Square, p: ChessPiece, moves: ArrayList<ChessMove>) {
        val orthogonalDirs = arrayOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
        generateSlidingPseudoMoves(state, from, p, orthogonalDirs, moves)
    }

    private fun generateQueenPseudoMoves(state: BoardState, from: Square, p: ChessPiece, moves: ArrayList<ChessMove>) {
        val allDirs = arrayOf(
            Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
            Pair(0, -1),               Pair(0, 1),
            Pair(1, -1),  Pair(1, 0),  Pair(1, 1)
        )
        generateSlidingPseudoMoves(state, from, p, allDirs, moves)
    }

    private fun generateSlidingPseudoMoves(
        state: BoardState,
        from: Square,
        p: ChessPiece,
        directions: Array<Pair<Int, Int>>,
        moves: ArrayList<ChessMove>
    ) {
        for (d in directions) {
            var step = 1
            while (true) {
                val r = from.row + d.first * step
                val c = from.col + d.second * step
                if (r !in 0..7 || c !in 0..7) break

                val dest = state.board[r][c]
                if (dest == null) {
                    moves.add(ChessMove(from, Square(r, c), p))
                } else {
                    if (dest.color != p.color) {
                        moves.add(ChessMove(from, Square(r, c), p, dest))
                    }
                    break // Blocked
                }
                step++
            }
        }
    }

    private fun generateKingPseudoMoves(
        state: BoardState,
        from: Square,
        p: ChessPiece,
        moves: ArrayList<ChessMove>,
        enemyColor: ChessColor
    ) {
        val adjacentDirs = arrayOf(
            Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
            Pair(0, -1),               Pair(0, 1),
            Pair(1, -1),  Pair(1, 0),  Pair(1, 1)
        )
        for (d in adjacentDirs) {
            val r = from.row + d.first
            val c = from.col + d.second
            if (r in 0..7 && c in 0..7) {
                val dest = state.board[r][c]
                if (dest == null) {
                    moves.add(ChessMove(from, Square(r, c), p))
                } else if (dest.color != p.color) {
                    moves.add(ChessMove(from, Square(r, c), p, dest))
                }
            }
        }

        // CASTLING
        val isWhite = p.color == ChessColor.WHITE
        val kingRow = if (isWhite) 7 else 0

        // Make sure king matches standard square and hasn't moved
        if (from.row == kingRow && from.col == 4) {
            val canKingSide = if (isWhite) state.castlingRights.whiteKingSide else state.castlingRights.blackKingSide
            val canQueenSide = if (isWhite) state.castlingRights.whiteQueenSide else state.castlingRights.blackQueenSide

            // Verify if currently in check
            val inCheck = isSquareAttacked(from, enemyColor, state.board)
            if (!inCheck) {
                // KING SIDE CASTLING
                if (canKingSide) {
                    val f1 = state.board[kingRow][5]
                    val g1 = state.board[kingRow][6]
                    if (f1 == null && g1 == null) {
                        // Squares must not be attacked
                        val f1Attacked = isSquareAttacked(Square(kingRow, 5), enemyColor, state.board)
                        val g1Attacked = isSquareAttacked(Square(kingRow, 6), enemyColor, state.board)
                        if (!f1Attacked && !g1Attacked) {
                            moves.add(
                                ChessMove(
                                    from = from,
                                    to = Square(kingRow, 6),
                                    piece = p,
                                    isCastlingKingSide = true
                                )
                            )
                        }
                    }
                }

                // QUEEN SIDE CASTLING
                if (canQueenSide) {
                    val d1 = state.board[kingRow][3]
                    val c1 = state.board[kingRow][2]
                    val b1 = state.board[kingRow][1]
                    if (d1 == null && c1 == null && b1 == null) {
                        val d1Attacked = isSquareAttacked(Square(kingRow, 3), enemyColor, state.board)
                        val c1Attacked = isSquareAttacked(Square(kingRow, 2), enemyColor, state.board)
                        if (!d1Attacked && !c1Attacked) {
                            moves.add(
                                ChessMove(
                                    from = from,
                                    to = Square(kingRow, 2),
                                    piece = p,
                                    isCastlingQueenSide = true
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun isInsufficientMaterial(state: BoardState): Boolean {
        var pawns = 0
        var knights = 0
        var bishops = 0
        var rooks = 0
        var queens = 0
        val whiteBishops = ArrayList<Square>()
        val blackBishops = ArrayList<Square>()

        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.board[r][c] ?: continue
                when (p.type) {
                    PieceType.PAWN -> pawns++
                    PieceType.ROOK -> rooks++
                    PieceType.QUEEN -> queens++
                    PieceType.KNIGHT -> knights++
                    PieceType.BISHOP -> {
                        bishops++
                        if (p.color == ChessColor.WHITE) {
                            whiteBishops.add(Square(r, c))
                        } else {
                            blackBishops.add(Square(r, c))
                        }
                    }
                    else -> {}
                }
            }
        }

        if (pawns > 0 || rooks > 0 || queens > 0) return false

        // King vs King
        if (knights == 0 && bishops == 0) return true

        // King + Bishop vs King
        if (bishops == 1 && knights == 0) return true

        // King + Knight vs King
        if (knights == 1 && bishops == 0) return true

        // King + Bishop vs King + Bishop
        if (bishops == 2 && knights == 0 && whiteBishops.size == 1 && blackBishops.size == 1) {
            // Check if bishops are on the same square color
            val whiteBishopSq = whiteBishops[0]
            val blackBishopSq = blackBishops[0]
            val isWhiteOnLight = (whiteBishopSq.row + whiteBishopSq.col) % 2 == 0
            val isBlackOnLight = (blackBishopSq.row + blackBishopSq.col) % 2 == 0
            if (isWhiteOnLight == isBlackOnLight) return true
        }

        return false
    }
}

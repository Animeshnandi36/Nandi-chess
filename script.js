/**
 * NANDI CHESS
 * High-Performance Client-Side Chess Engine, Minimax AI (500 - 2500+ ELO),
 * Clocks, Database Saves, & Web Audio Synthesis.
 * Developed by Animesh Nandi, 2026.
 */

/* ==========================================================================
   CONSTANTS & BASIC MODELS
   ========================================================================== */
const COLOR_WHITE = 'WHITE';
const COLOR_BLACK = 'BLACK';

const TYPE_PAWN = 'PAWN';
const TYPE_KNIGHT = 'KNIGHT';
const TYPE_BISHOP = 'BISHOP';
const TYPE_ROOK = 'ROOK';
const TYPE_QUEEN = 'QUEEN';
const TYPE_KING = 'KING';

// Piece unicode visual mapping symbols
const PieceSymbols = {
    [COLOR_WHITE]: {
        [TYPE_PAWN]: '♙',
        [TYPE_KNIGHT]: '♘',
        [TYPE_BISHOP]: '♗',
        [TYPE_ROOK]: '♖',
        [TYPE_QUEEN]: '♕',
        [TYPE_KING]: '♔'
    },
    [COLOR_BLACK]: {
        [TYPE_PAWN]: '♟',
        [TYPE_KNIGHT]: '♞',
        [TYPE_BISHOP]: '♝',
        [TYPE_ROOK]: '♜',
        [TYPE_QUEEN]: '♛',
        [TYPE_KING]: '♚'
    }
};

const PieceChars = {
    [COLOR_WHITE]: { [TYPE_PAWN]: 'P', [TYPE_KNIGHT]: 'N', [TYPE_BISHOP]: 'B', [TYPE_ROOK]: 'R', [TYPE_QUEEN]: 'Q', [TYPE_KING]: 'K' },
    [COLOR_BLACK]: { [TYPE_PAWN]: 'p', [TYPE_KNIGHT]: 'n', [TYPE_BISHOP]: 'b', [TYPE_ROOK]: 'r', [TYPE_QUEEN]: 'q', [TYPE_KING]: 'k' }
};

class ChessPiece {
    constructor(type, color) {
        this.type = type;
        this.color = color;
    }
}

class Square {
    constructor(row, col) {
        this.row = row;
        this.col = col;
    }

    isValid() {
        return this.row >= 0 && this.row < 8 && this.col >= 0 && this.col < 8;
    }

    toAlgebraic() {
        const file = String.fromCharCode(97 + this.col);
        const rank = 8 - this.row;
        return `${file}${rank}`;
    }

    static fromAlgebraic(alg) {
        if (!alg || alg.length !== 2) return null;
        const col = alg.charCodeAt(0) - 97;
        const row = 8 - parseInt(alg[1]);
        const sq = new Square(row, col);
        return sq.isValid() ? sq : null;
    }
}

class ChessMove {
    constructor(from, to, piece, capturedPiece = null, isEp = false, isCk = false, isCq = false, promoType = null) {
        this.from = from;
        this.to = to;
        this.piece = piece;
        this.capturedPiece = capturedPiece;
        this.isEnPassant = isEp;
        this.isCastlingKingSide = isCk;
        this.isCastlingQueenSide = isCq;
        this.promotionType = promoType;
    }
}

/* ==========================================================================
   WEB AUDIO TONAL SOUND SYNTHESIZER
   ========================================================================== */
const SoundEffects = {
    audioCtx: null,

    init() {
        if (!this.audioCtx) {
            this.audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        }
    },

    playTone(frequency, durationMs, type = 'sine') {
        try {
            if (isMuted) return;
            this.init();
            
            // Resume suspended contexts
            if (this.audioCtx.state === 'suspended') {
                this.audioCtx.resume();
            }

            const osc = this.audioCtx.createOscillator();
            const gain = this.audioCtx.createGain();
            
            osc.type = type;
            osc.frequency.value = frequency;
            
            gain.gain.setValueAtTime(0.08, this.audioCtx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, this.audioCtx.currentTime + durationMs / 1000);
            
            osc.connect(gain);
            gain.connect(this.audioCtx.destination);
            
            osc.start();
            osc.stop(this.audioCtx.currentTime + durationMs / 1000);
        } catch (e) {
            console.warn("Audio Context blocked or not ready: ", e);
        }
    },

    playMove() {
        this.playTone(480, 80);
    },

    playCapture() {
        this.playTone(400, 60);
        setTimeout(() => this.playTone(600, 80), 65);
    },

    playCheck() {
        this.playTone(330, 120);
        setTimeout(() => this.playTone(330, 120), 130);
        setTimeout(() => this.playTone(480, 200), 260);
    },

    playCheckmate() {
        const ar = [523, 659, 784, 1046];
        ar.forEach((freq, idx) => {
            setTimeout(() => this.playTone(freq, 150), idx * 150);
        });
    },

    playGameOver() {
        const ar = [380, 320, 260];
        ar.forEach((freq, idx) => {
            setTimeout(() => this.playTone(freq, 180), idx * 180);
        });
    }
};

/* ==========================================================================
   CHESS GAME STATE ENGINE
   ========================================================================== */
class BoardState {
    constructor() {
        this.board = Array(8).fill(null).map(() => Array(8).fill(null));
        this.activeColor = COLOR_WHITE;
        this.castling = {
            whiteKingSide: true, whiteQueenSide: true,
            blackKingSide: true, blackQueenSide: true
        };
        this.enPassantTarget = null; // Square
        this.halfMoveClock = 0;
        this.fullMoveNumber = 1;
    }

    getPiece(sq) {
        return sq.isValid() ? this.board[sq.row][sq.col] : null;
    }

    copy() {
        const next = new BoardState();
        for (let r = 0; r < 8; r++) {
            for (let c = 0; c < 8; c++) {
                const p = this.board[r][c];
                next.board[r][c] = p ? new ChessPiece(p.type, p.color) : null;
            }
        }
        next.activeColor = this.activeColor;
        next.castling = { ...this.castling };
        next.enPassantTarget = this.enPassantTarget ? new Square(this.enPassantTarget.row, this.enPassantTarget.col) : null;
        next.halfMoveClock = this.halfMoveClock;
        next.fullMoveNumber = this.fullMoveNumber;
        return next;
    }

    toFen() {
        const rows = [];
        for (let r = 0; r < 8; r++) {
            let rowStr = '';
            let emptyCount = 0;
            for (let c = 0; c < 8; c++) {
                const p = this.board[r][c];
                if (!p) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        rowStr += emptyCount;
                        emptyCount = 0;
                    }
                    rowStr += PieceChars[p.color][p.type];
                }
            }
            if (emptyCount > 0) {
                rowStr += emptyCount;
            }
            rows.push(rowStr);
        }

        const active = this.activeColor === COLOR_WHITE ? 'w' : 'b';
        
        let cast = '';
        if (this.castling.whiteKingSide) cast += 'K';
        if (this.castling.whiteQueenSide) cast += 'Q';
        if (this.castling.blackKingSide) cast += 'k';
        if (this.castling.blackQueenSide) cast += 'q';
        if (!cast) cast = '-';

        const ep = this.enPassantTarget ? this.enPassantTarget.toAlgebraic() : '-';

        return `${rows.join('/')} ${active} ${cast} ${ep} ${this.halfMoveClock} ${this.fullMoveNumber}`;
    }

    static fromFen(fen) {
        const state = new BoardState();
        const parts = fen.trim().split(/\s+/);
        if (parts.length === 0) return state;

        // 1. Board state
        const rows = parts[0].split('/');
        for (let r = 0; r < 8; r++) {
            if (r >= rows.length) break;
            const rStr = rows[r];
            let c = 0;
            for (let i = 0; i < rStr.length; i++) {
                if (c >= 8) break;
                const char = rStr[i];
                if (!isNaN(char)) {
                    c += parseInt(char);
                } else {
                    const color = char === char.toUpperCase() ? COLOR_WHITE : COLOR_BLACK;
                    let type = TYPE_PAWN;
                    const uc = char.toUpperCase();
                    if (uc === 'N') type = TYPE_KNIGHT;
                    else if (uc === 'B') type = TYPE_BISHOP;
                    else if (uc === 'R') type = TYPE_ROOK;
                    else if (uc === 'Q') type = TYPE_QUEEN;
                    else if (uc === 'K') type = TYPE_KING;

                    state.board[r][c] = new ChessPiece(type, color);
                    c++;
                }
            }
        }

        // 2. Active player
        if (parts.length > 1) {
            state.activeColor = parts[1].toLowerCase() === 'b' ? COLOR_BLACK : COLOR_WHITE;
        }

        // 3. Castling rights
        if (parts.length > 2) {
            const castStr = parts[2];
            state.castling = {
                whiteKingSide: castStr.includes('K'),
                whiteQueenSide: castStr.includes('Q'),
                blackKingSide: castStr.includes('k'),
                blackQueenSide: castStr.includes('q')
            };
        }

        // 4. En Passant
        if (parts.length > 3) {
            const epStr = parts[3];
            state.enPassantTarget = epStr === '-' ? null : Square.fromAlgebraic(epStr);
        }

        // 5. Halfmove clock
        if (parts.length > 4) {
            state.halfMoveClock = parseInt(parts[4]) || 0;
        }

        // 6. Fullmove number
        if (parts.length > 5) {
            state.fullMoveNumber = parseInt(parts[5]) || 1;
        }

        return state;
    }

    makeMove(move) {
        const next = this.copy();
        const { from, to, piece } = move;

        next.board[from.row][from.col] = null;

        // En passant capture
        if (move.isEnPassant) {
            next.board[from.row][to.col] = null;
            next.halfMoveClock = 0;
        } else if (piece.type === TYPE_PAWN || move.capturedPiece) {
            next.halfMoveClock = 0;
        } else {
            next.halfMoveClock++;
        }

        // Piece placement or promotion
        if (move.promotionType) {
            next.board[to.row][to.col] = new ChessPiece(move.promotionType, piece.color);
        } else {
            next.board[to.row][to.col] = piece;
        }

        // Castling moves execution
        if (move.isCastlingKingSide) {
            const rRow = piece.color === COLOR_WHITE ? 7 : 0;
            const rook = next.board[rRow][7];
            next.board[rRow][7] = null;
            next.board[rRow][5] = rook;
        } else if (move.isCastlingQueenSide) {
            const rRow = piece.color === COLOR_WHITE ? 7 : 0;
            const rook = next.board[rRow][0];
            next.board[rRow][0] = null;
            next.board[rRow][3] = rook;
        }

        // En passant clock setup
        if (piece.type === TYPE_PAWN && Math.abs(to.row - from.row) === 2) {
            next.enPassantTarget = new Square((from.row + to.row) / 2, from.col);
        } else {
            next.enPassantTarget = null;
        }

        // Update castling rights
        if (piece.type === TYPE_KING) {
            if (piece.color === COLOR_WHITE) {
                next.castling.whiteKingSide = false;
                next.castling.whiteQueenSide = false;
            } else {
                next.castling.blackKingSide = false;
                next.castling.blackQueenSide = false;
            }
        }

        if (piece.type === TYPE_ROOK) {
            if (piece.color === COLOR_WHITE) {
                if (from.row === 7 && from.col === 7) next.castling.whiteKingSide = false;
                if (from.row === 7 && from.col === 0) next.castling.whiteQueenSide = false;
            } else {
                if (from.row === 0 && from.col === 7) next.castling.blackKingSide = false;
                if (from.row === 0 && from.col === 0) next.castling.blackQueenSide = false;
            }
        }

        // Capture of rook updates opponent castling rights
        if (to.row === 7 && to.col === 7) next.castling.whiteKingSide = false;
        if (to.row === 7 && to.col === 0) next.castling.whiteQueenSide = false;
        if (to.row === 0 && to.col === 7) next.castling.blackKingSide = false;
        if (to.row === 0 && to.col === 0) next.castling.blackQueenSide = false;

        // Turn alternate
        if (next.activeColor === COLOR_BLACK) {
            next.fullMoveNumber++;
        }
        next.activeColor = next.activeColor === COLOR_WHITE ? COLOR_BLACK : COLOR_WHITE;

        return next;
    }
}

/* ==========================================================================
   CHESS GAME MOVEMENTS ENGINE RULES
   ========================================================================== */
const ChessEngine = {
    generateLegalMoves(state) {
        const pseudo = this.generatePseudoMoves(state);
        return pseudo.filter(m => !this.leavesKingInCheck(state, m));
    },

    leavesKingInCheck(state, move) {
        const simulated = state.makeMove(move);
        const originalColor = state.activeColor;
        return this.isKingInCheck(simulated, originalColor);
    },

    isKingInCheck(state, color) {
        let kingSq = null;
        for (let r = 0; r < 8; r++) {
            for (let c = 0; c < 8; c++) {
                const p = state.board[r][c];
                if (p && p.type === TYPE_KING && p.color === color) {
                    kingSq = new Square(r, c);
                    break;
                }
            }
            if (kingSq) break;
        }

        if (!kingSq) return false;
        const enemyColor = color === COLOR_WHITE ? COLOR_BLACK : COLOR_WHITE;
        return this.isSquareAttacked(kingSq, enemyColor, state.board);
    },

    isSquareAttacked(square, attackerColor, board) {
        // 1. Knight jumps
        const knightOffsets = [
            [-2, -1], [-2, 1], [-1, -2], [-1, 2],
            [1, -2], [1, 2], [2, -1], [2, 1]
        ];
        for (const [dr, dc] of knightOffsets) {
            const r = square.row + dr;
            const c = square.col + dc;
            if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                const p = board[r][c];
                if (p && p.color === attackerColor && p.type === TYPE_KNIGHT) return true;
            }
        }

        // 2. Pawns
        const pawnRowOffset = attackerColor === COLOR_WHITE ? 1 : -1;
        const pawnCols = [square.col - 1, square.col + 1];
        for (const c of pawnCols) {
            const r = square.row + pawnRowOffset;
            if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                const p = board[r][c];
                if (p && p.color === attackerColor && p.type === TYPE_PAWN) return true;
            }
        }

        // 3. Adjacent Kings
        const directions = [
            [-1, -1], [-1, 0], [-1, 1],
            [0, -1],           [0, 1],
            [1, -1],  [1, 0],  [1, 1]
        ];
        for (const [dr, dc] of directions) {
            const r = square.row + dr;
            const c = square.col + dc;
            if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                const p = board[r][c];
                if (p && p.color === attackerColor && p.type === TYPE_KING) return true;
            }
        }

        // 4. Sliding Cardinal paths (Rook/Queen)
        const orthogonalDirs = [[-1, 0], [1, 0], [0, -1], [0, 1]];
        for (const [dr, dc] of orthogonalDirs) {
            let step = 1;
            while (true) {
                const r = square.row + dr * step;
                const c = square.col + dc * step;
                if (r < 0 || r >= 8 || c < 0 || c >= 8) break;
                const p = board[r][c];
                if (p) {
                    if (p.color === attackerColor && (p.type === TYPE_ROOK || p.type === TYPE_QUEEN)) return true;
                    break; // blocked
                }
                step++;
            }
        }

        // 5. Sliding Diagonal paths (Bishop/Queen)
        const diagonalDirs = [[-1, -1], [-1, 1], [1, -1], [1, 1]];
        for (const [dr, dc] of diagonalDirs) {
            let step = 1;
            while (true) {
                const r = square.row + dr * step;
                const c = square.col + dc * step;
                if (r < 0 || r >= 8 || c < 0 || c >= 8) break;
                const p = board[r][c];
                if (p) {
                    if (p.color === attackerColor && (p.type === TYPE_BISHOP || p.type === TYPE_QUEEN)) return true;
                    break; // blocked
                }
                step++;
            }
        }

        return false;
    },

    generatePseudoMoves(state) {
        const moves = [];
        const color = state.activeColor;
        const enemy = color === COLOR_WHITE ? COLOR_BLACK : COLOR_WHITE;

        for (let r = 0; r < 8; r++) {
            for (let c = 0; c < 8; c++) {
                const p = state.board[r][c];
                if (!p || p.color !== color) continue;

                const from = new Square(r, c);
                if (p.type === TYPE_PAWN) this.addPawnPseudo(state, from, p, enemy, moves);
                else if (p.type === TYPE_KNIGHT) this.addKnightPseudo(state, from, p, moves);
                else if (p.type === TYPE_BISHOP) this.addBishopPseudo(state, from, p, moves);
                else if (p.type === TYPE_ROOK) this.addRookPseudo(state, from, p, moves);
                else if (p.type === TYPE_QUEEN) this.addQueenPseudo(state, from, p, moves);
                else if (p.type === TYPE_KING) this.addKingPseudo(state, from, p, enemy, moves);
            }
        }
        return moves;
    },

    addPawnPseudo(state, from, p, enemy, moves) {
        const dir = p.color === COLOR_WHITE ? -1 : 1;
        const startRow = p.color === COLOR_WHITE ? 6 : 1;
        const promoRow = p.color === COLOR_WHITE ? 0 : 7;

        // 1 step push
        const r1 = from.row + dir;
        if (r1 >= 0 && r1 < 8 && !state.board[r1][from.col]) {
            const to = new Square(r1, from.col);
            if (r1 === promoRow) {
                this.addPromotions(from, to, p, null, moves);
            } else {
                moves.push(new ChessMove(from, to, p));
            }

            // 2 step push start rank
            const r2 = from.row + 2 * dir;
            if (from.row === startRow && !state.board[r2][from.col]) {
                moves.push(new ChessMove(from, new Square(r2, from.col), p));
            }
        }

        // Diagonals standard captures
        const captureCols = [from.col - 1, from.col + 1];
        for (const c of captureCols) {
            if (r1 >= 0 && r1 < 8 && c >= 0 && c < 8) {
                const cap = state.board[r1][c];
                if (cap && cap.color === enemy) {
                    const to = new Square(r1, c);
                    if (r1 === promoRow) {
                        this.addPromotions(from, to, p, cap, moves);
                    } else {
                        moves.push(new ChessMove(from, to, p, cap));
                    }
                }
            }
        }

        // En passant
        if (state.enPassantTarget && r1 === state.enPassantTarget.row && Math.abs(from.col - state.enPassantTarget.col) === 1) {
            const capPawn = state.board[from.row][state.enPassantTarget.col];
            moves.push(new ChessMove(from, state.enPassantTarget, p, capPawn, true));
        }
    },

    addPromotions(from, to, p, cap, moves) {
        const pts = [TYPE_QUEEN, TYPE_ROOK, TYPE_BISHOP, TYPE_KNIGHT];
        for (const t of pts) {
            moves.push(new ChessMove(from, to, p, cap, false, false, false, t));
        }
    },

    addKnightPseudo(state, from, p, moves) {
        const jumpList = [
            [-2, -1], [-2, 1], [-1, -2], [-1, 2],
            [1, -2], [1, 2], [2, -1], [2, 1]
        ];
        for (const [dr, dc] of jumpList) {
            const r = from.row + dr;
            const c = from.col + dc;
            if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                const dest = state.board[r][c];
                if (!dest) {
                    moves.push(new ChessMove(from, new Square(r, c), p));
                } else if (dest.color !== p.color) {
                    moves.push(new ChessMove(from, new Square(r, c), p, dest));
                }
            }
        }
    },

    addBishopPseudo(state, from, p, moves) {
        const dirs = [[-1, -1], [-1, 1], [1, -1], [1, 1]];
        this.addSlidingPseudo(state, from, p, dirs, moves);
    },

    addRookPseudo(state, from, p, moves) {
        const dirs = [[-1, 0], [1, 0], [0, -1], [0, 1]];
        this.addSlidingPseudo(state, from, p, dirs, moves);
    },

    addQueenPseudo(state, from, p, moves) {
        const dirs = [
            [-1, -1], [-1, 0], [-1, 1],
            [0, -1],           [0, 1],
            [1, -1],  [1, 0],  [1, 1]
        ];
        this.addSlidingPseudo(state, from, p, dirs, moves);
    },

    addSlidingPseudo(state, from, p, directions, moves) {
        for (const [dr, dc] of directions) {
            let step = 1;
            while (true) {
                const r = from.row + dr * step;
                const c = from.col + dc * step;
                if (r < 0 || r >= 8 || c < 0 || c >= 8) break;

                const dest = state.board[r][c];
                if (!dest) {
                    moves.push(new ChessMove(from, new Square(r, c), p));
                } else {
                    if (dest.color !== p.color) {
                        moves.push(new ChessMove(from, new Square(r, c), p, dest));
                    }
                    break; // Blocked
                }
                step++;
            }
        }
    },

    addKingPseudo(state, from, p, enemy, moves) {
        const adjacent = [
            [-1, -1], [-1, 0], [-1, 1],
            [0, -1],           [0, 1],
            [1, -1],  [1, 0],  [1, 1]
        ];
        for (const [dr, dc] of adjacent) {
            const r = from.row + dr;
            const c = from.col + dc;
            if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                const dest = state.board[r][c];
                if (!dest) {
                    moves.push(new ChessMove(from, new Square(r, c), p));
                } else if (dest.color !== p.color) {
                    moves.push(new ChessMove(from, new Square(r, c), p, dest));
                }
            }
        }

        // CASTLING
        const isWhite = p.color === COLOR_WHITE;
        const kingRow = isWhite ? 7 : 0;

        if (from.row === kingRow && from.col === 4) {
            const canK = isWhite ? state.castling.whiteKingSide : state.castling.blackKingSide;
            const canQ = isWhite ? state.castling.whiteQueenSide : state.castling.blackQueenSide;

            const inCheck = this.isSquareAttacked(from, enemy, state.board);
            if (!inCheck) {
                // KING SIDE
                if (canK && !state.board[kingRow][5] && !state.board[kingRow][6]) {
                    if (!this.isSquareAttacked(new Square(kingRow, 5), enemy, state.board) &&
                        !this.isSquareAttacked(new Square(kingRow, 6), enemy, state.board)) {
                        moves.push(new ChessMove(from, new Square(kingRow, 6), p, null, false, true, false));
                    }
                }
                // QUEEN SIDE
                if (canQ && !state.board[kingRow][3] && !state.board[kingRow][2] && !state.board[kingRow][1]) {
                    if (!this.isSquareAttacked(new Square(kingRow, 3), enemy, state.board) &&
                        !this.isSquareAttacked(new Square(kingRow, 2), enemy, state.board)) {
                        moves.push(new ChessMove(from, new Square(kingRow, 2), p, null, false, false, true));
                    }
                }
            }
        }
    },

    isInsufficientMaterial(state) {
        let pawns = 0, rooks = 0, queens = 0, knights = 0, bishops = 0;
        let whiteBishops = [], blackBishops = [];

        for (let r = 0; r < 8; r++) {
            for (let c = 0; c < 8; c++) {
                const p = state.board[r][c];
                if (!p) continue;
                if (p.type === TYPE_PAWN) pawns++;
                else if (p.type === TYPE_ROOK) rooks++;
                else if (p.type === TYPE_QUEEN) queens++;
                else if (p.type === TYPE_KNIGHT) knights++;
                else if (p.type === TYPE_BISHOP) {
                    bishops++;
                    if (p.color === COLOR_WHITE) whiteBishops.push(new Square(r,c));
                    else blackBishops.push(new Square(r,c));
                }
            }
        }

        if (pawns > 0 || rooks > 0 || queens > 0) return false;
        if (knights === 0 && bishops === 0) return true; // King vs King
        if (bishops === 1 && knights === 0) return true; // King vs King + Bishop
        if (knights === 1 && bishops === 0) return true; // King vs King + Knight

        if (bishops === 2 && knights === 0 && whiteBishops.length === 1 && blackBishops.length === 1) {
            const wb = whiteBishops[0];
            const bb = blackBishops[0];
            const wbLight = (wb.row + wb.col) % 2 === 0;
            const bbLight = (bb.row + bb.col) % 2 === 0;
            if (wbLight === bbLight) return true; // same diagonal colors draw
        }

        return false;
    }
};

/* ==========================================================================
   CHESS AI OPTIMIZER AND MINIMAX ALPHA-BETA
   ========================================================================== */
function evaluateBoard(state) {
    let score = 0;
    const values = {
        [TYPE_PAWN]: 100, [TYPE_KNIGHT]: 320, [TYPE_BISHOP]: 330,
        [TYPE_ROOK]: 500, [TYPE_QUEEN]: 900, [TYPE_KING]: 20000
    };

    // Position tables (mid game developmental weighting)
    const pawnPst = [
        [  0,  0,  0,  0,  0,  0,  0,  0],
        [ 50, 50, 50, 50, 50, 50, 50, 50],
        [ 10, 10, 20, 30, 30, 20, 10, 10],
        [  5,  5, 10, 25, 25, 10,  5,  5],
        [  0,  0,  0, 20, 20,  0,  0,  0],
        [  5, -5,-10,  0,  0,-10, -5,  5],
        [  5, 10, 10,-20,-20, 10, 10,  5],
        [  0,  0,  0,  0,  0,  0,  0,  0]
    ];

    const knightPst = [
        [-50,-40,-30,-30,-30,-30,-40,-50],
        [-40,-20,  0,  0,  0,  0,-20,-40],
        [-30,  0, 10, 15, 15, 10,  0,-30],
        [-30,  5, 15, 20, 20, 15,  5,-30],
        [-30,  0, 15, 20, 20, 15,  0,-30],
        [-30,  5, 10, 15, 15, 10,  5,-30],
        [-40,-20,  0,  5,  5,  0,-20,-40],
        [-50,-40,-30,-30,-30,-30,-40,-50]
    ];

    const bishopPst = [
        [-20,-10,-10,-10,-10,-10,-10,-20],
        [-10,  0,  0,  0,  0,  0,  0,-10],
        [-10,  0,  5, 10, 10,  5,  0,-10],
        [-10,  5,  5, 10, 10,  5,  5,-10],
        [-10,  0, 10, 10, 10, 10,  0,-10],
        [-10, 10, 10, 10, 10, 10, 10,-10],
        [-10,  5,  0,  0,  0,  0,  5,-10],
        [-20,-10,-10,-10,-10,-10,-10,-20]
    ];

    const kingPst = [
        [-30,-40,-40,-50,-50,-40,-40,-30],
        [-30,-40,-40,-50,-50,-40,-40,-30],
        [-30,-40,-40,-50,-50,-40,-40,-30],
        [-30,-40,-40,-50,-50,-40,-40,-30],
        [-20,-30,-30,-40,-40,-30,-30,-20],
        [-10,-20,-20,-20,-20,-20,-20,-10],
        [ 20, 20,  0,  0,  0,  0, 20, 20],
        [ 20, 30, 10,  0,  0, 10, 30, 20]
    ];

    for (let r = 0; r < 8; r++) {
        for (let c = 0; c < 8; c++) {
            const p = state.board[r][c];
            if (!p) continue;
            const val = values[p.type];
            let pst = 0;
            const isWhite = p.color === COLOR_WHITE;
            const actualRow = isWhite ? r : (7 - r);

            if (p.type === TYPE_PAWN) pst = pawnPst[actualRow][c];
            else if (p.type === TYPE_KNIGHT) pst = knightPst[actualRow][c];
            else if (p.type === TYPE_BISHOP) pst = bishopPst[actualRow][c];
            else if (p.type === TYPE_KING) pst = kingPst[actualRow][c];

            const cellVal = val + pst;
            if (isWhite) score += cellVal;
            else score -= cellVal;
        }
    }
    return score;
}

const ChessAI = {
    // Basic opening book moves
    openingBook: {
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1": ["e2e4", "d2d4"],
        "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1": ["c7c5", "e7e5"],
        "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2": ["g1f3", "b1c3"]
    },

    openingNames: {
        "e4": "The Open Game",
        "d4": "The Queen's Pawn Opening",
        "e4 c5": "Sicilian Defense",
        "e4 e5 Nf3 Nc6": "King's Knight Opening",
        "e4 e5 Nf3 Nc6 Bb5": "Ruy Lopez Spanish",
        "e4 e5 Nf3 Nc6 Bc4": "Italian Game",
        "e4 e6": "French Defense",
        "e4 c6": "Caro-Kann"
    },

    getOpeningName(pgn) {
        const join = pgn.trim().replace(/\s+/g, " ");
        for (const [moves, name] of Object.entries(this.openingNames)) {
            if (join.startsWith(moves)) return name;
        }
        return null;
    },

    getBestMove(state, difficulty) {
        const legal = ChessEngine.generateLegalMoves(state);
        if (legal.length === 0) return { move: null, score: evaluateBoard(state) };

        // 1. Try opening book on Expert Difficulty preference
        if (difficulty === 'EXPERT') {
            const fen = state.toFen().trim();
            for (const [key, candidates] of Object.entries(this.openingBook)) {
                if (fen.startsWith(key)) {
                    const picked = candidates[Math.floor(Math.random() * candidates.length)];
                    const matched = legal.find(m => {
                        const alg = m.from.toAlgebraic() + m.to.toAlgebraic();
                        return alg === picked;
                    });
                    if (matched) return { move: matched, score: evaluateBoard(state) };
                }
            }
        }

        // 2. Play level depth filters
        if (difficulty === 'BEGINNER') {
            if (Math.random() < 0.3) {
                return { move: legal[Math.floor(Math.random() * legal.length)], score: evaluateBoard(state) };
            }
            return this.minimax(state, 1, -1000000, 1000000);
        } else if (difficulty === 'INTERMEDIATE') {
            if (Math.random() < 0.1) {
                return { move: legal[Math.floor(Math.random() * legal.length)], score: evaluateBoard(state) };
            }
            return this.minimax(state, 2, -1000000, 1000000);
        } else if (difficulty === 'ADVANCED') {
            return this.minimax(state, 3, -1000000, 1000000);
        } else {
            // Expert Elo (2500+) depth 4
            return this.minimax(state, 4, -1000000, 1000000);
        }
    },

    minimax(state, depth, alpha, beta) {
        const color = state.activeColor;
        const legal = ChessEngine.generateLegalMoves(state);

        if (legal.length === 0) {
            const inCheck = ChessEngine.isKingInCheck(state, color);
            if (inCheck) {
                return {
                    move: null,
                    score: color === COLOR_WHITE ? -900000 + (4 - depth) : 900000 - (4 - depth)
                };
            }
            return { move: null, score: 0 }; // stalemate
        }

        if (depth === 0) {
            return { move: null, score: evaluateBoard(state) };
        }

        // MVV-LVA move ordering logic
        const ordered = legal.slice().sort((a,b) => {
            let scoreA = 0, scoreB = 0;
            const pVal = { [TYPE_PAWN]: 10, [TYPE_KNIGHT]: 30, [TYPE_BISHOP]: 30, [TYPE_ROOK]: 50, [TYPE_QUEEN]: 90, [TYPE_KING]: 2000 };
            if (a.capturedPiece) scoreA += 10 * pVal[a.capturedPiece.type] - pVal[a.piece.type] / 10;
            if (b.capturedPiece) scoreB += 10 * pVal[b.capturedPiece.type] - pVal[b.piece.type] / 10;
            if (a.promotionType) scoreA += 80;
            if (b.promotionType) scoreB += 80;
            return scoreB - scoreA;
        });

        let bestMove = null;
        let currentAlpha = alpha;
        let currentBeta = beta;

        if (color === COLOR_WHITE) {
            let maxEval = -Infinity;
            for (const m of ordered) {
                const sim = state.makeMove(m);
                const { score } = this.minimax(sim, depth - 1, currentAlpha, currentBeta);
                if (score > maxEval) {
                    maxEval = score;
                    bestMove = m;
                }
                currentAlpha = Math.max(currentAlpha, score);
                if (currentBeta <= currentAlpha) break;
            }
            return { move: bestMove, score: maxEval };
        } else {
            let minEval = Infinity;
            for (const m of ordered) {
                const sim = state.makeMove(m);
                const { score } = this.minimax(sim, depth - 1, currentAlpha, currentBeta);
                if (score < minEval) {
                    minEval = score;
                    bestMove = m;
                }
                currentBeta = Math.min(currentBeta, score);
                if (currentBeta <= currentAlpha) break;
            }
            return { move: bestMove, score: minEval };
        }
    }
};

/* ==========================================================================
   GLOBAL APP STATE REPLICATORS
   ========================================================================== */
let activeState = BoardState.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
let selectedSquare = null;
let legalMovesForSelected = [];
let moveHistory = []; // string algebraic
let moveDetailedHistory = []; // raw ChessMove array
let undoStack = [];

let gameMode = 'PVAI'; // 'PVAI', 'PVP', 'AIVAI'
let aiDifficulty = 'INTERMEDIATE';
let userColor = COLOR_WHITE;
let boardTheme = 'cosmic';
let isMuted = false;
let isAnalyzeMode = false;
let activeHint = null;

let whiteTimeLeft = 10 * 60 * 1000;
let blackTimeLeft = 10 * 60 * 1000;
let isClockRunning = false;
let timeLimitMinutes = 10;
let timeIncrementSeconds = 0;
let isCompleted = false;

let clockInterval = null;
let aiThinkingInterval = null;
let promotionSelectionPayload = null; // { from, to }

/* ==========================================================================
   UI RENDERING AND INTERACTIVE CONTROLS BOUNDS
   ========================================================================== */
function querySelector(selector) {
    return document.querySelector(selector);
}

function initUI() {
    // 1. TABS SYSTEM INTERACTION
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
            
            btn.classList.add('active');
            const targetPaneId = `tab-${btn.dataset.tab}`;
            querySelector(`#${targetPaneId}`).classList.add('active');
        });
    });

    // 2. TOGGLE ACTION ROWS (Game mode, Starting color side, AI level, Themes picker)
    querySelector('#mode-pvai').addEventListener('click', () => setGameMode('PVAI'));
    querySelector('#mode-pvp').addEventListener('click', () => setGameMode('PVP'));
    querySelector('#mode-aivai').addEventListener('click', () => setGameMode('AIVAI'));

    querySelector('#side-white').addEventListener('click', () => setUserStartingSide(COLOR_WHITE));
    querySelector('#side-black').addEventListener('click', () => setUserStartingSide(COLOR_BLACK));

    document.querySelectorAll('[data-level]').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('[data-level]').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            aiDifficulty = btn.dataset.level;
            triggerAiIfNeeded();
        });
    });

    document.querySelectorAll('[data-theme]').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('[data-theme]').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            boardTheme = btn.dataset.theme;
            renderBoard();
        });
    });

    // Time presets scrolling row buttons
    document.querySelectorAll('.mode-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.mode-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            timeLimitMinutes = parseInt(btn.dataset.minutes);
            timeIncrementSeconds = parseInt(btn.dataset.increment);
            resetGame();
        });
    });

    // Operational Shortcuts
    querySelector('#btn-undo').addEventListener('click', undoMove);
    querySelector('#btn-flip').addEventListener('click', flipBoardToggle);
    querySelector('#btn-hint').addEventListener('click', requestHint);
    querySelector('#btn-analyze').addEventListener('click', toggleAnalyzer);
    querySelector('#btn-mute').addEventListener('click', toggleMutedVoice);

    querySelector('#btn-restart').addEventListener('click', resetGame);
    querySelector('#btn-resign').addEventListener('click', resignGameAction);
    querySelector('#btn-save-game').addEventListener('click', saveGameToLocalDb);

    // Notation operations
    querySelector('#btn-import-pgn').addEventListener('click', showPgnImportModal);
    querySelector('#btn-export-pgn').addEventListener('click', showPgnExportModal);
    querySelector('#btn-export-image').addEventListener('click', downloadBoardCanvasScreenshot);

    // Modal close panels clicks
    querySelector('#promo-cancel').addEventListener('click', hidePromoModal);
    querySelector('#pgn-dialog-close').addEventListener('click', hidePgnModal);

    // Promotion items chooser clicks
    document.querySelectorAll('.promo-choice').forEach(btn => {
        btn.addEventListener('click', () => {
            const chosenType = btn.dataset.piece;
            executePromoMovement(chosenType);
        });
    });

    resetGame();
    loadGamesFromDbHistory();
}

function renderBoard() {
    const boardDiv = querySelector('#chess-board');
    boardDiv.className = `chess-board ${boardTheme}-theme`;
    boardDiv.innerHTML = '';

    // If screen is flipped perspective
    const rows = isFlipped ? Array.from({length: 8}, (_, k) => k) : Array.from({length: 8}, (_, k) => k);
    const cols = isFlipped ? Array.from({length: 8}, (_, k) => 7 - k) : Array.from({length: 8}, (_, k) => k);

    for (let r = 0; r < 8; r++) {
        const actualRow = isFlipped ? 7 - r : r;
        for (let c = 0; c < 8; c++) {
            const actualCol = cols[c];
            const sq = new Square(actualRow, actualCol);
            const p = activeState.getPiece(sq);

            const isLight = (actualRow + actualCol) % 2 === 0;
            const sqDiv = document.createElement('div');
            sqDiv.className = `chess-square ${isLight ? 'square-light' : 'square-dark'}`;
            sqDiv.dataset.row = actualRow;
            sqDiv.dataset.col = actualCol;

            // Highlight status overlays
            if (selectedSquare && selectedSquare.row === actualRow && selectedSquare.col === actualCol) {
                sqDiv.classList.add('selected');
            }
            if (activeHint && (activeHint.from.row === actualRow && activeHint.from.col === actualCol || activeHint.to.row === actualRow && activeHint.to.col === actualCol)) {
                sqDiv.classList.add('hint');
            }

            // Checks King glowing
            if (p && p.type === TYPE_KING && p.color === activeState.activeColor && ChessEngine.isKingInCheck(activeState, p.color)) {
                sqDiv.classList.add('in-check');
            }

            // Move Legal dot highlighter
            const matchedLegal = legalMovesForSelected.find(m => m.to.row === actualRow && m.to.col === actualCol);
            if (matchedLegal) {
                sqDiv.classList.add('legal-dest');
                if (p) sqDiv.classList.add('is-capture');
            }

            // Draw pieces representation text character symbole
            if (p) {
                const pSpan = document.createElement('span');
                pSpan.className = `chess-piece ${p.color === COLOR_WHITE ? 'white-piece' : 'black-piece'}`;
                pSpan.innerHTML = PieceSymbols[p.color][p.type];
                sqDiv.appendChild(pSpan);
            }

            sqDiv.addEventListener('click', () => handleSquareClick(sq));
            boardDiv.appendChild(sqDiv);
        }
    }
}

function handleSquareClick(square) {
    if (isCompleted) return;
    if (isAiTurnNow()) return;

    const clickedPiece = activeState.getPiece(square);

    if (selectedSquare === null) {
        // Select
        if (clickedPiece && clickedPiece.color === activeState.activeColor) {
            selectedSquare = square;
            legalMovesForSelected = ChessEngine.generateLegalMoves(activeState).filter(m => m.from.row === square.row && m.from.col === square.col);
            renderBoard();
        }
    } else {
        // Execute move request
        const match = legalMovesForSelected.find(m => m.to.row === square.row && m.to.col === square.col);
        if (match) {
            const isPromo = match.piece.type === TYPE_PAWN && (square.row === 0 || square.row === 7);
            if (isPromo) {
                promotionSelectionPayload = { from: selectedSquare, to: square };
                showPromoModal();
            } else {
                executeBoardMove(match);
            }
        } else {
            if (clickedPiece && clickedPiece.color === activeState.activeColor) {
                selectedSquare = square;
                legalMovesForSelected = ChessEngine.generateLegalMoves(activeState).filter(m => m.from.row === square.row && m.from.col === square.col);
                renderBoard();
            } else {
                selectedSquare = null;
                legalMovesForSelected = [];
                renderBoard();
            }
        }
    }
}

function executeBoardMove(move) {
    undoStack.push(activeState.copy());

    if (move.capturedPiece) {
        SoundEffects.playCapture();
    } else {
        SoundEffects.playMove();
    }

    const previousColor = activeState.activeColor;
    const nextState = activeState.makeMove(move);

    // Apply clock tick increment
    addClockIncrement(previousColor);

    // SAN Algebraic logging computation
    const legalPrev = ChessEngine.generateLegalMoves(activeState);
    const isCheck = ChessEngine.isKingInCheck(nextState, nextState.activeColor);
    const legalNext = ChessEngine.generateLegalMoves(nextState);
    const isMated = isCheck && legalNext.length === 0;

    const san = generateSanNotation(move, legalPrev, isCheck, isMated);

    compileHistoryNotation(previousColor, san);

    activeState = nextState;
    selectedSquare = null;
    legalMovesForSelected = [];
    activeHint = null;

    renderBoard();
    updateLiveEvaluation();
    detectOpeningNames();

    const checkOver = checkGameOverCondition(activeState, legalNext, isCheck);
    if (!checkOver) {
        if (isCheck) SoundEffects.playCheck();
        triggerAiIfNeeded();
    }
}

function addClockIncrement(color) {
    if (timeLimitMinutes === 9999) return;
    const incMs = timeIncrementSeconds * 1000;
    if (color === COLOR_WHITE) {
        whiteTimeLeft += incMs;
    } else {
        blackTimeLeft += incMs;
    }
    updateTimerUI();
}

function compileHistoryNotation(color, san) {
    if (color === COLOR_WHITE) {
        moveHistory.push(`${activeState.fullMoveNumber}. ${san}`);
    } else {
        if (moveHistory.length > 0) {
            moveHistory[moveHistory.length - 1] += ` ${san}`;
        } else {
            moveHistory.push(`1... ${san}`);
        }
    }
    renderHistoryNotationPanel();
}

function renderHistoryNotationPanel() {
    const listLog = querySelector('#moves-log');
    listLog.innerHTML = '';
    if (moveHistory.length === 0) {
        listLog.innerHTML = '<p class="empty-log-msg">No moves logged yet. Start a match!</p>';
        return;
    }

    moveHistory.forEach(line => {
        const div = document.createElement('div');
        div.className = 'move-row';
        div.innerText = line;
        listLog.appendChild(div);
    });
    listLog.scrollTop = listLog.scrollHeight;
}

function generateSanNotation(move, allLegal, isCheck, isMated) {
    if (move.isCastlingKingSide) return "O-O";
    if (move.isCastlingQueenSide) return "O-O-O";

    let n = '';
    if (move.piece.type !== TYPE_PAWN) {
        n += PieceChars[COLOR_WHITE][move.piece.type];
        // Disambiguation simple
        const duplicates = allLegal.filter(m => m.to.row === move.to.row && m.to.col === move.to.col && m.piece.type === move.piece.type && (m.from.row !== move.from.row || m.from.col !== move.from.col));
        if (duplicates.length > 0) {
            n += String.fromCharCode(97 + move.from.col);
        }
    } else {
        if (move.capturedPiece || move.isEnPassant) {
            n += String.fromCharCode(97 + move.from.col);
        }
    }

    if (move.capturedPiece || move.isEnPassant) n += 'x';
    n += move.to.toAlgebraic();

    if (move.promotionType) {
        n += `=${PieceChars[COLOR_WHITE][move.promotionType]}`;
    }

    if (isMated) n += '#';
    else if (isCheck) n += '+';

    return n;
}

function checkGameOverCondition(state, legal, inCheck) {
    if (legal.length === 0) {
        isCompleted = true;
        stopClock();
        if (inCheck) {
            const winner = state.activeColor === COLOR_WHITE ? 'Black wins by Checkmate!' : 'White wins by Checkmate!';
            triggerMatchOverDialog(winner);
            SoundEffects.playCheckmate();
        } else {
            triggerMatchOverDialog('Draw by Stalemate');
            SoundEffects.playGameOver();
        }
        return true;
    }

    if (state.halfMoveClock >= 100) {
        isCompleted = true;
        stopClock();
        triggerMatchOverDialog('Draw by 50-move rule');
        SoundEffects.playGameOver();
        return true;
    }

    if (ChessEngine.isInsufficientMaterial(state)) {
        isCompleted = true;
        stopClock();
        triggerMatchOverDialog('Draw by Insufficient Material');
        SoundEffects.playGameOver();
        return true;
    }

    return false;
}

function triggerMatchOverDialog(message) {
    alert(`GAME OVER — ${message}`);
}

/* ==========================================================================
   AI SCHEDULER & DECISION LOOPS
   ========================================================================== */
function isAiTurnNow() {
    if (gameMode === 'PVP') return false;
    if (gameMode === 'AIVAI') return true;
    // PVAI match turn check
    const isAiWhite = userColor === COLOR_BLACK;
    const active = activeState.activeColor;
    return (isAiWhite && active === COLOR_WHITE) || (!isAiWhite && active === COLOR_BLACK);
}

function triggerAiIfNeeded() {
    if (isCompleted || !isAiTurnNow()) {
        if (isAnalyzeMode) performLiveAnalyzingHint();
        return;
    }

    querySelector('#ai-loading').classList.remove('hidden');

    setTimeout(() => {
        const { move } = ChessAI.getBestMove(activeState, aiDifficulty);
        querySelector('#ai-loading').classList.add('hidden');
        if (move) {
            executeBoardMove(move);
        }
    }, 600); // realistic think delays
}

function performLiveAnalyzingHint() {
    setTimeout(() => {
        const { move } = ChessAI.getBestMove(activeState, aiDifficulty);
        if (move) {
            activeHint = move;
            renderBoard();
        }
    }, 150);
}

function updateLiveEvaluation() {
    const rawVal = evaluateBoard(activeState);
    const clamped = Math.max(-1500, Math.min(1500, rawVal));
    const normalizedPercent = ((clamped + 1500) / 3000) * 100;
    
    querySelector('#eval-fill').style.width = `${normalizedPercent}%`;
    querySelector('#eval-text').innerText = `Advantage: ${rawVal >= 0 ? '+' : ''}${(rawVal/100).toFixed(2)}`;
}

function detectOpeningNames() {
    if (moveHistory.length === 0) return;
    const clearTokens = moveHistory.flatMap(line => line.split(/\s+/).filter(t => !t.includes('.'))).join(" ");
    const opening = ChessAI.getOpeningName(clearTokens);
    if (opening) {
        querySelector('#opening-text').style.display = 'inline-block';
        querySelector('#opening-text').innerText = opening;
    } else {
        querySelector('#opening-text').style.display = 'none';
    }
}

/* ==========================================================================
   CHESS TIMERS & CLOCKS IMPLEMENTATION
   ========================================================================== */
function startClock() {
    if (timeLimitMinutes === 9999) return;
    stopClock();
    isClockRunning = true;
    updateClockHighlights();

    clockInterval = setInterval(() => {
        if (!isClockRunning || isCompleted) return;

        if (activeState.activeColor === COLOR_WHITE) {
            whiteTimeLeft = Math.max(0, whiteTimeLeft - 100);
            if (whiteTimeLeft <= 0) flagFallClock(COLOR_WHITE);
        } else {
            blackTimeLeft = Math.max(0, blackTimeLeft - 100);
            if (blackTimeLeft <= 0) flagFallClock(COLOR_BLACK);
        }
        updateTimerUI();
    }, 100);
}

function flagFallClock(color) {
    stopClock();
    isCompleted = true;
    const msg = color === COLOR_WHITE ? 'Black wins on time!' : 'White wins on time!';
    triggerMatchOverDialog(msg);
    SoundEffects.playGameOver();
}

function stopClock() {
    clearInterval(clockInterval);
    isClockRunning = false;
    updateClockHighlights();
}

function updateTimerUI() {
    const formatTime = (ms) => {
        const mins = Math.floor((ms / 1000) / 60);
        const secs = Math.floor((ms / 1000) % 60);
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    };

    if (timeLimitMinutes === 9999) {
        querySelector('#time-white').innerText = '∞';
        querySelector('#time-black').innerText = '∞';
    } else {
        querySelector('#time-white').innerText = formatTime(whiteTimeLeft);
        querySelector('#time-black').innerText = formatTime(blackTimeLeft);
    }
}

function updateClockHighlights() {
    const whiteClk = querySelector('#clock-white');
    const blackClk = querySelector('#clock-black');

    whiteClk.className = 'clock-card';
    blackClk.className = 'clock-card';

    if (isClockRunning && !isCompleted) {
        if (activeState.activeColor === COLOR_WHITE) whiteClk.classList.add('active');
        else blackClk.classList.add('active', 'active-black');
    }
}

/* ==========================================================================
   GAME SETTING & RESTORES HANDLERS
   ========================================================================== */
let isFlipped = false;

function setGameMode(mode) {
    document.querySelectorAll('[id^="mode-"]').forEach(b => b.classList.remove('active'));
    querySelector(`#mode-${mode.toLowerCase()}`).classList.add('active');
    gameMode = mode;
    resetGame();
}

function setUserStartingSide(side) {
    document.querySelectorAll('[id^="side-"]').forEach(b => b.classList.remove('active'));
    querySelector(`#side-${side.toLowerCase()}`).classList.add('active');
    userColor = side;
    resetGame();
}

function resetGame() {
    stopClock();
    activeState = BoardState.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    selectedSquare = null;
    legalMovesForSelected = [];
    moveHistory = [];
    undoStack = [];
    isCompleted = false;
    activeHint = null;

    whiteTimeLeft = timeLimitMinutes * 60 * 1000;
    blackTimeLeft = timeLimitMinutes * 60 * 1000;

    querySelector('#opening-text').style.display = 'none';

    updateTimerUI();
    if (timeLimitMinutes !== 9999) {
        startClock();
    }

    renderBoard();
    updateLiveEvaluation();
    renderHistoryNotationPanel();
    triggerAiIfNeeded();
}

function undoMove() {
    if (undoStack.length === 0) return;
    activeState = undoStack.pop();
    
    // For PvAI, popping 1 move places it in AI's turn. Undo 2 turns to give human their grid back!
    if (gameMode === 'PVAI' && undoStack.length > 0) {
        activeState = undoStack.pop();
    }

    selectedSquare = null;
    legalMovesForSelected = [];
    isCompleted = false;
    activeHint = null;

    if (moveHistory.length > 0) {
        moveHistory.pop();
    }

    updateClockHighlights();
    renderBoard();
    updateLiveEvaluation();
    detectOpeningNames();
    renderHistoryNotationPanel();
}

function flipBoardToggle() {
    isFlipped = !isFlipped;
    SoundEffects.playMove();
    renderBoard();
}

function requestHint() {
    const { move } = ChessAI.getBestMove(activeState, aiDifficulty);
    if (move) {
        activeHint = move;
        renderBoard();
        SoundEffects.playMove();
    }
}

function toggleAnalyzer() {
    isAnalyzeMode = !isAnalyzeMode;
    const btn = querySelector('#btn-analyze');
    if (isAnalyzeMode) {
        btn.classList.add('active');
        performLiveAnalyzingHint();
    } else {
        btn.classList.remove('active');
        activeHint = null;
        renderBoard();
    }
}

function toggleMutedVoice() {
    isMuted = !isMuted;
    const icon = querySelector('#btn-mute i');
    if (isMuted) {
        icon.className = 'fa-solid fa-volume-xmark';
    } else {
        icon.className = 'fa-solid fa-volume-high';
    }
    SoundEffects.playTone(400, 50); // tester beep
}

function resignGameAction() {
    if (isCompleted) return;
    isCompleted = true;
    stopClock();
    const txt = activeState.activeColor === COLOR_WHITE ? 'White resigns. Black wins!' : 'Black resigns. White wins!';
    triggerMatchOverDialog(txt);
    SoundEffects.playGameOver();
}

/* ==========================================================================
   PROMOTION AND MODAL ACTIONS
   ========================================================================== */
function showPromoModal() {
    querySelector('#promo-dialog').classList.remove('hidden');
}

function hidePromoModal() {
    querySelector('#promo-dialog').classList.add('hidden');
    promotionSelectionPayload = null;
}

function executePromoMovement(chosenType) {
    if (!promotionSelectionPayload) return;
    const { from, to } = promotionSelectionPayload;
    hidePromoModal();

    const activeMoves = ChessEngine.generateLegalMoves(activeState);
    const match = activeMoves.find(m => m.from.row === from.row && m.from.col === from.col && m.to.row === to.row && m.to.col === to.col && m.promotionType === chosenType);
    if (match) {
        executeBoardMove(match);
    }
}

/* ==========================================================================
   PGN IMPORTS AND EXPORTS MANAGER
   ========================================================================== */
function showPgnModal(title, textValue, readOnly = false, hasAction = true) {
    const modal = querySelector('#pgn-dialog');
    querySelector('#pgn-dialog-title').innerText = title;
    
    const area = querySelector('#pgn-dialog-textarea');
    area.value = textValue;
    area.readOnly = readOnly;

    const actionBtn = querySelector('#pgn-dialog-action');
    if (hasAction) {
        actionBtn.style.display = 'inline-block';
        actionBtn.innerText = title.includes('Import') ? 'Import' : 'Copy';
        
        // Re-bind action button click handler
        actionBtn.onclick = () => {
            if (title.includes('Import')) {
                executePgnImport(area.value);
                hidePgnModal();
            } else {
                navigator.clipboard.writeText(area.value);
                alert('Copied PGN to clipboard successfully!');
                hidePgnModal();
            }
        };
    } else {
        actionBtn.style.display = 'none';
    }
    
    modal.classList.remove('hidden');
}

function hidePgnModal() {
    querySelector('#pgn-dialog').classList.add('hidden');
}

function showPgnImportModal() {
    showPgnModal('Import PGN Game Moves', '', false, true);
}

function showPgnExportModal() {
    const pgnText = buildFullPgnString();
    showPgnModal('Export PGN Game State', pgnText, true, true);
}

function buildFullPgnString() {
    const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, ".");
    let pgn = `[Event "Nandi Chess Practice Match"]\n`;
    pgn += `[Site "Local Browser LocalStorage"]\n`;
    pgn += `[Date "${dateStr}"]\n`;
    pgn += `[White "${gameMode === 'PVAI' && userColor === COLOR_BLACK ? 'Nandi AI (' + aiDifficulty + ')' : 'Human Player'}"]\n`;
    pgn += `[Black "${gameMode === 'PVAI' && userColor === COLOR_WHITE ? 'Nandi AI (' + aiDifficulty + ')' : 'Human Player'}"]\n`;
    
    let result = '*';
    if (isCompleted) {
        const lastMove = moveHistory[moveHistory.length - 1] || '';
        if (lastMove.includes('#')) {
            result = activeState.activeColor === COLOR_WHITE ? '0-1' : '1-0';
        } else {
            result = '1/2-1/2';
        }
    }
    pgn += `[Result "${result}"]\n\n`;

    pgn += moveHistory.join(" ") + " " + result;
    return pgn;
}

function executePgnImport(pgnText) {
    try {
        const clearMoves = pgnText.replace(/\[.*?\]/g, '').trim();
        const tokens = clearMoves.split(/\s+/).filter(t => t && !t.includes('.') && !['1-0', '0-1', '1/2-1/2', '*'].includes(t));
        
        resetGame();
        let idx = 0;

        function loadStepLoop() {
            if (idx >= tokens.length) {
                alert('PGN game successfully loaded!');
                return;
            }
            const token = tokens[idx].replace(/[+#]/g, '');
            const legals = ChessEngine.generateLegalMoves(activeState);

            const matched = legals.find(m => {
                const san = generateSanNotation(m, ChessEngine.generateLegalMoves(activeState), false, false).replace(/[+#]/g, '');
                const fromTo = m.from.toAlgebraic() + m.to.toAlgebraic();
                return san === token || fromTo === token;
            });

            if (matched) {
                executeBoardMove(matched);
                idx++;
                setTimeout(loadStepLoop, 250);
            } else {
                console.warn('PGN Parse broke at token moves: ', token);
                alert(`Error loading PGN. Failed at move index ${idx + 1}: ${token}`);
            }
        }
        loadStepLoop();
    } catch (e) {
        alert('Invalid PGN string format.');
    }
}

/* ==========================================================================
   CANVAS SCREENSHOT BOARD EXPORTS
   ========================================================================== */
function downloadBoardCanvasScreenshot() {
    const canvas = document.createElement('canvas');
    canvas.width = 600;
    canvas.height = 600;
    const ctx = canvas.getContext('2d');

    const themes = {
        cosmic: { light: '#2E3E5C', dark: '#141F36', text: '#FFFDF0', bgPiece: '#111215' },
        classic: { light: '#F0D9B5', dark: '#B58863', text: '#FFFDF0', bgPiece: '#111215' },
        forest: { light: '#E0E0C0', dark: '#70A070', text: '#FFFDF0', bgPiece: '#111215' },
        velvet: { light: '#E8D0C0', dark: '#8B122B', text: '#FFFDF0', bgPiece: '#111215' }
    };

    const scheme = themes[boardTheme] || themes.cosmic;
    const size = 600 / 8;

    for (let r = 0; r < 8; r++) {
        for (let c = 0; c < 8; c++) {
            const sq = new Square(r, c);
            const isLight = (r + c) % 2 === 0;
            ctx.fillStyle = isLight ? scheme.light : scheme.dark;
            ctx.fillRect(c * size, r * size, size, size);

            const p = activeState.getPiece(sq);
            if (p) {
                ctx.fillStyle = p.color === COLOR_WHITE ? scheme.text : scheme.bgPiece;
                ctx.font = `${size * 0.72}px sans-serif`;
                ctx.textAlign = 'center';
                ctx.textBaseline = 'middle';
                ctx.fillText(PieceSymbols[p.color][p.type], c * size + size/2, r * size + size/2);
            }
        }
    }

    // Trigger downloads
    const link = document.createElement('a');
    link.download = `nandi_chess_position_${Date.now()}.png`;
    link.href = canvas.toDataURL();
    link.click();
}

/* ==========================================================================
   PERSISTENCE LOCAL DATABASE ACQUISITIONS
   ========================================================================== */
function saveGameToLocalDb() {
    try {
        const list = JSON.parse(localStorage.getItem('nandi_chess_games') || '[]');
        const label = `Nandi Match vs AI (${aiDifficulty})`;
        const game = {
            id: Date.now(),
            title: label,
            timestamp: Date.now(),
            fen: activeState.toFen(),
            pgn: buildFullPgnString(),
            result: isCompleted ? 'Completed Match' : 'Saved Draft'
        };
        list.push(game);
        localStorage.setItem('nandi_chess_games', JSON.stringify(list));
        alert('Active board saved successfully to positions database!');
        loadGamesFromDbHistory();
    } catch (e) {
        alert('Database write failed.');
    }
}

function loadGamesFromDbHistory() {
    const listContainer = querySelector('#saved-games-container');
    listContainer.innerHTML = '';
    const games = JSON.parse(localStorage.getItem('nandi_chess_games') || '[]');
    
    if (games.length === 0) {
        listContainer.innerHTML = '<p class="empty-log-msg">No saved games found. Record your moves!</p>';
        return;
    }

    games.forEach(g => {
        const dateStr = new Date(g.timestamp).toLocaleString(undefined, {month: 'short', day: 'numeric', hour: '2-digit', minute:'2-digit'});
        const card = document.createElement('div');
        card.className = 'saved-game-entry';
        card.innerHTML = `
            <div class="saved-game-info">
                <h4>${g.title}</h4>
                <p>${dateStr}</p>
                <span>${g.result}</span>
            </div>
            <div class="saved-game-actions">
                <button class="game-icon-btn load" title="Resume Match"><i class="fa-solid fa-play"></i></button>
                <button class="game-icon-btn delete" title="Delete Draft"><i class="fa-solid fa-trash-can"></i></button>
            </div>
        `;
        
        card.querySelector('.load').onclick = () => resumeLocalSavedGameSession(g);
        card.querySelector('.delete').onclick = () => deleteLocalSavedGameSession(g.id);
        
        listContainer.appendChild(card);
    });
}

function resumeLocalSavedGameSession(game) {
    try {
        activeState = BoardState.fromFen(game.fen);
        
        // Re-compile move notation
        moveHistory = [];
        const lines = game.pgn.split('\n');
        const notationLine = lines[lines.length - 1];
        if (notationLine && !notationLine.startsWith('[')) {
            const steps = notationLine.split(/\s+/);
            let activeLineStr = '';
            steps.forEach(t => {
                if (t.includes('.')) {
                    if (activeLineStr) moveHistory.push(activeLineStr.trim());
                    activeLineStr = t + " ";
                } else if (!['1-0','0-1','1/2-1/2','*'].includes(t)) {
                    activeLineStr += t + " ";
                }
            });
            if (activeLineStr) moveHistory.push(activeLineStr.trim());
        }

        selectedSquare = null;
        legalMovesForSelected = [];
        isCompleted = false;
        activeHint = null;

        renderBoard();
        updateLiveEvaluation();
        detectOpeningNames();
        renderHistoryNotationPanel();

        // Switch Tab views
        document.querySelector('[data-tab="settings"]').click();
        alert('Board position loaded!');
    } catch (e) {
        alert('Error loading saved draft.');
    }
}

function deleteLocalSavedGameSession(id) {
    if (!confirm('Are you sure you want to delete this position?')) return;
    let list = JSON.parse(localStorage.getItem('nandi_chess_games') || '[]');
    list = list.filter(g => g.id !== id);
    localStorage.setItem('nandi_chess_games', JSON.stringify(list));
    loadGamesFromDbHistory();
}

/* ==========================================================================
   DOM INITIALIZATION LISTENER
   ========================================================================== */
window.addEventListener('DOMContentLoaded', initUI);

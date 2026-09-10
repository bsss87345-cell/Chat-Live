package com.example.ui.screens.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMatchMode
import com.example.ui.theme.MujtamaGold
import com.example.ui.theme.MujtamaOnlineGreen
import com.example.ui.theme.MujtamaPrimary
import com.example.ui.theme.MujtamaTeal
import kotlin.random.Random

enum class ChessPieceType(val symbolWhite: String, val symbolBlack: String, val nameAr: String) {
    PAWN("♙", "♟", "بيدق"),
    ROOK("♖", "♜", "قلعة"),
    KNIGHT("♘", "♞", "حصان"),
    BISHOP("♗", "♝", "فيل"),
    QUEEN("♕", "♛", "وزير"),
    KING("♔", "♚", "ملك")
}

data class ChessPiece(
    val type: ChessPieceType,
    val isWhite: Boolean // true = Player (White), false = Opponent (Black)
)

data class ChessPos(val row: Int, val col: Int)

@Composable
fun ChessGameView(
    mode: GameMatchMode,
    opponentName: String,
    onBack: () -> Unit,
    onWinReward: (Int) -> Unit
) {
    fun initialBoard(): Array<Array<ChessPiece?>> {
        val b = Array(8) { Array<ChessPiece?>(8) { null } }
        // Black pieces (Row 0 & 1)
        b[0][0] = ChessPiece(ChessPieceType.ROOK, false)
        b[0][1] = ChessPiece(ChessPieceType.KNIGHT, false)
        b[0][2] = ChessPiece(ChessPieceType.BISHOP, false)
        b[0][3] = ChessPiece(ChessPieceType.QUEEN, false)
        b[0][4] = ChessPiece(ChessPieceType.KING, false)
        b[0][5] = ChessPiece(ChessPieceType.BISHOP, false)
        b[0][6] = ChessPiece(ChessPieceType.KNIGHT, false)
        b[0][7] = ChessPiece(ChessPieceType.ROOK, false)
        for (c in 0..7) b[1][c] = ChessPiece(ChessPieceType.PAWN, false)

        // White pieces (Row 6 & 7)
        for (c in 0..7) b[6][c] = ChessPiece(ChessPieceType.PAWN, true)
        b[7][0] = ChessPiece(ChessPieceType.ROOK, true)
        b[7][1] = ChessPiece(ChessPieceType.KNIGHT, true)
        b[7][2] = ChessPiece(ChessPieceType.BISHOP, true)
        b[7][3] = ChessPiece(ChessPieceType.QUEEN, true)
        b[7][4] = ChessPiece(ChessPieceType.KING, true)
        b[7][5] = ChessPiece(ChessPieceType.BISHOP, true)
        b[7][6] = ChessPiece(ChessPieceType.KNIGHT, true)
        b[7][7] = ChessPiece(ChessPieceType.ROOK, true)
        return b
    }

    var board by remember { mutableStateOf(initialBoard()) }
    var selectedPos by remember { mutableStateOf<ChessPos?>(null) }
    var isWhiteTurn by remember { mutableStateOf(true) } // White = Player, Black = Opponent
    var gameMessage by remember { mutableStateOf("دورك بالقطع البيضاء! حدد قطعة لرؤية النقلات المتاحة ♟️") }
    var winner by remember { mutableStateOf<String?>(null) }
    var rewardGranted by remember { mutableStateOf(false) }

    fun getValidMoves(pos: ChessPos, isWhite: Boolean): List<ChessPos> {
        val piece = board[pos.row][pos.col] ?: return emptyList()
        if (piece.isWhite != isWhite) return emptyList()

        val moves = mutableListOf<ChessPos>()
        val dir = if (isWhite) -1 else 1

        fun inBounds(r: Int, c: Int) = r in 0..7 && c in 0..7

        when (piece.type) {
            ChessPieceType.PAWN -> {
                // 1 step forward
                val f1 = pos.row + dir
                if (inBounds(f1, pos.col) && board[f1][pos.col] == null) {
                    moves.add(ChessPos(f1, pos.col))
                    // 2 steps from initial
                    val f2 = pos.row + 2 * dir
                    val initialRow = if (isWhite) 6 else 1
                    if (pos.row == initialRow && inBounds(f2, pos.col) && board[f2][pos.col] == null) {
                        moves.add(ChessPos(f2, pos.col))
                    }
                }
                // Diagonal captures
                for (dc in listOf(-1, 1)) {
                    val cr = pos.row + dir
                    val cc = pos.col + dc
                    if (inBounds(cr, cc) && board[cr][cc] != null && board[cr][cc]!!.isWhite != isWhite) {
                        moves.add(ChessPos(cr, cc))
                    }
                }
            }
            ChessPieceType.KNIGHT -> {
                val knightOffsets = listOf(
                    -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                    1 to -2, 1 to 2, 2 to -1, 2 to 1
                )
                for ((dr, dc) in knightOffsets) {
                    val nr = pos.row + dr
                    val nc = pos.col + dc
                    if (inBounds(nr, nc)) {
                        val target = board[nr][nc]
                        if (target == null || target.isWhite != isWhite) {
                            moves.add(ChessPos(nr, nc))
                        }
                    }
                }
            }
            ChessPieceType.BISHOP -> {
                val diagDirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
                for ((dr, dc) in diagDirs) {
                    var r = pos.row + dr
                    var c = pos.col + dc
                    while (inBounds(r, c)) {
                        val target = board[r][c]
                        if (target == null) {
                            moves.add(ChessPos(r, c))
                        } else {
                            if (target.isWhite != isWhite) moves.add(ChessPos(r, c))
                            break
                        }
                        r += dr
                        c += dc
                    }
                }
            }
            ChessPieceType.ROOK -> {
                val straightDirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                for ((dr, dc) in straightDirs) {
                    var r = pos.row + dr
                    var c = pos.col + dc
                    while (inBounds(r, c)) {
                        val target = board[r][c]
                        if (target == null) {
                            moves.add(ChessPos(r, c))
                        } else {
                            if (target.isWhite != isWhite) moves.add(ChessPos(r, c))
                            break
                        }
                        r += dr
                        c += dc
                    }
                }
            }
            ChessPieceType.QUEEN -> {
                val allDirs = listOf(
                    -1 to 0, 1 to 0, 0 to -1, 0 to 1,
                    -1 to -1, -1 to 1, 1 to -1, 1 to 1
                )
                for ((dr, dc) in allDirs) {
                    var r = pos.row + dr
                    var c = pos.col + dc
                    while (inBounds(r, c)) {
                        val target = board[r][c]
                        if (target == null) {
                            moves.add(ChessPos(r, c))
                        } else {
                            if (target.isWhite != isWhite) moves.add(ChessPos(r, c))
                            break
                        }
                        r += dr
                        c += dc
                    }
                }
            }
            ChessPieceType.KING -> {
                val allDirs = listOf(
                    -1 to 0, 1 to 0, 0 to -1, 0 to 1,
                    -1 to -1, -1 to 1, 1 to -1, 1 to 1
                )
                for ((dr, dc) in allDirs) {
                    val nr = pos.row + dr
                    val nc = pos.col + dc
                    if (inBounds(nr, nc)) {
                        val target = board[nr][nc]
                        if (target == null || target.isWhite != isWhite) {
                            moves.add(ChessPos(nr, nc))
                        }
                    }
                }
            }
        }
        return moves
    }

    val validMoves = remember(selectedPos, isWhiteTurn) {
        selectedPos?.let { getValidMoves(it, isWhiteTurn) } ?: emptyList()
    }

    // AI Turn simulation
    LaunchedEffect(isWhiteTurn, winner) {
        if (!isWhiteTurn && winner == null) {
            kotlinx.coroutines.delay(1200)

            // Find all black pieces that have valid moves
            val allBlackMoves = mutableListOf<Pair<ChessPos, ChessPos>>()
            for (r in 0..7) {
                for (c in 0..7) {
                    val piece = board[r][c]
                    if (piece != null && !piece.isWhite) {
                        val moves = getValidMoves(ChessPos(r, c), false)
                        for (m in moves) {
                            allBlackMoves.add(ChessPos(r, c) to m)
                        }
                    }
                }
            }

            if (allBlackMoves.isNotEmpty()) {
                // Prefer capture if available
                val captureMove = allBlackMoves.find { (_, dest) -> board[dest.row][dest.col] != null }
                val chosen = captureMove ?: allBlackMoves.random()

                val from = chosen.first
                val to = chosen.second
                val movingPiece = board[from.row][from.col]
                val targetPiece = board[to.row][to.col]

                // Clone board
                val newBoard = Array(8) { r -> Array(8) { c -> board[r][c] } }
                newBoard[to.row][to.col] = movingPiece
                newBoard[from.row][from.col] = null
                board = newBoard

                if (targetPiece?.type == ChessPieceType.KING) {
                    winner = "$opponentName الفائز! 🤝"
                    gameMessage = "تمت محاصرة الملك من قِبل $opponentName! كش ملك."
                } else if (targetPiece != null) {
                    gameMessage = "قام $opponentName بأكل ${targetPiece.type.nameAr} الخاص بك!"
                } else {
                    gameMessage = "$opponentName حرك ${movingPiece?.type?.nameAr} إلى المربع."
                }
            }

            kotlinx.coroutines.delay(600)
            isWhiteTurn = true
            selectedPos = null
            if (winner == null) {
                gameMessage = "دورك بالقطع البيضاء! حدد قطعتك ♟️"
            }
        }
    }

    fun handleSquareClick(row: Int, col: Int) {
        if (!isWhiteTurn || winner != null) return

        val clickedPiece = board[row][col]
        val currentSel = selectedPos

        if (currentSel != null && validMoves.contains(ChessPos(row, col))) {
            // Execute move
            val movingPiece = board[currentSel.row][currentSel.col]
            val capturedPiece = board[row][col]

            val newBoard = Array(8) { r -> Array(8) { c -> board[r][c] } }
            newBoard[row][col] = movingPiece
            newBoard[currentSel.row][currentSel.col] = null
            board = newBoard
            selectedPos = null

            if (capturedPiece?.type == ChessPieceType.KING) {
                winner = "أنت الفائز! 🏆"
                gameMessage = "كش ملك! أسقطت ملك الخصم وفزت بالمباراة وحصلت على +100 نقطة نظام 🌟"
                if (!rewardGranted) {
                    rewardGranted = true
                    onWinReward(100)
                }
            } else if (capturedPiece != null) {
                gameMessage = "رائع! التهمت ${capturedPiece.type.nameAr} الخاص بـ $opponentName 🎯"
                isWhiteTurn = false
            } else {
                gameMessage = "نقلة ممتازة! ينتقل الدور لـ $opponentName..."
                isWhiteTurn = false
            }
        } else if (clickedPiece != null && clickedPiece.isWhite) {
            // Select white piece
            selectedPos = ChessPos(row, col)
            gameMessage = "حددت ${clickedPiece.type.nameAr}. اختر أحد المربعات المضاءة."
        } else {
            selectedPos = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("chess_game_screen")
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("chess_back_button")) {
                Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("الشطرنج الكلاسيكي ♟️", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(
                    text = "${mode.titleAr} • ضد: $opponentName (القطع السوداء)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(shape = RoundedCornerShape(10.dp), color = MujtamaGold.copy(alpha = 0.2f)) {
                Text(
                    text = "+100 نقطة",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MujtamaGold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Status banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (winner != null) MujtamaGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = gameMessage,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(10.dp),
                color = if (winner != null) MujtamaGold else MaterialTheme.colorScheme.onSurface
            )
        }

        // 8x8 Chessboard
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in 0..7) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (col in 0..7) {
                            val isLightSquare = (row + col) % 2 == 0
                            val isSelected = selectedPos?.row == row && selectedPos?.col == col
                            val isMoveTarget = validMoves.contains(ChessPos(row, col))
                            val piece = board[row][col]

                            val squareColor = when {
                                isSelected -> MujtamaGold.copy(alpha = 0.6f)
                                isMoveTarget -> MujtamaOnlineGreen.copy(alpha = 0.45f)
                                isLightSquare -> Color(0xFFF0D9B5)
                                else -> Color(0xFFB58863)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(squareColor)
                                    .clickable(enabled = winner == null) { handleSquareClick(row, col) }
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MujtamaGold)
                                        else if (isMoveTarget) Modifier.border(1.5.dp, MujtamaOnlineGreen)
                                        else Modifier
                                    )
                                    .testTag("chess_cell_${row}_$col"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (piece != null) {
                                    Text(
                                        text = if (piece.isWhite) piece.type.symbolWhite else piece.type.symbolBlack,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (piece.isWhite) Color(0xFF1E293B) else Color(0xFF0F172A)
                                    )
                                } else if (isMoveTarget) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MujtamaOnlineGreen)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isWhiteTurn) Color.White else Color.Black)
                            .border(1.dp, Color.Gray, CircleShape)
                    )
                    Text(
                        text = if (isWhiteTurn) "دورك (الأبيض)" else "دور $opponentName (الأسود)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = {
                        board = initialBoard()
                        selectedPos = null
                        isWhiteTurn = true
                        winner = null
                        rewardGranted = false
                        gameMessage = "بدأت رقعة شطرنج جديدة! انقر على قطعتك لنقلها ♟️"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MujtamaPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إعادة الرقعة 🔄", fontSize = 11.sp)
                }
            }
        }
    }
}

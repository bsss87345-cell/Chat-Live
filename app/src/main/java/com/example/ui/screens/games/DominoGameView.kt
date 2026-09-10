package com.example.ui.screens.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

data class DominoTile(
    val id: String,
    val left: Int,
    val right: Int
) {
    val pipSum: Int get() = left + right
    fun reversed(): DominoTile = DominoTile(id, right, left)
}

@Composable
fun DominoGameView(
    mode: GameMatchMode,
    opponentName: String,
    onBack: () -> Unit,
    onWinReward: (Int) -> Unit
) {
    // Standard double-six domino pool
    val allTiles = remember {
        val list = mutableListOf<DominoTile>()
        var idCounter = 1
        for (i in 0..6) {
            for (j in i..6) {
                list.add(DominoTile("t_${idCounter++}", i, j))
            }
        }
        list.shuffled()
    }

    var boneyard by remember { mutableStateOf(allTiles.drop(11)) }
    var playerHand by remember { mutableStateOf(allTiles.take(5)) }
    var opponentHandCount by remember { mutableStateOf(5) }
    var opponentTiles by remember { mutableStateOf(allTiles.drop(5).take(5)) }

    // Start with one initial tile on board
    val initialBoardTile = remember { allTiles[10] }
    var boardChain by remember { mutableStateOf(listOf(initialBoardTile)) }

    var isPlayerTurn by remember { mutableStateOf(true) }
    var gameMessage by remember { mutableStateOf("طابق أطراف قطع الدومينو وتخلص من أحجارك أولاً 🀄") }
    var winner by remember { mutableStateOf<String?>(null) }
    var rewardGranted by remember { mutableStateOf(false) }

    val leftEnd = boardChain.first().left
    val rightEnd = boardChain.last().right

    fun checkWinCondition() {
        if (winner != null) return
        if (playerHand.isEmpty()) {
            winner = "أنت الفائز! 🎉"
            gameMessage = "مبروك! تخلصت من جميع قطعك بنجاح وحصلت على +100 نقطة نظام 🌟"
            if (!rewardGranted) {
                rewardGranted = true
                onWinReward(100)
            }
        } else if (opponentTiles.isEmpty() || opponentHandCount == 0) {
            winner = "$opponentName الفائز! 🤝"
            gameMessage = "أنهى $opponentName جميع قطعه أولاً وفاز بالجولة!"
        }
    }

    LaunchedEffect(playerHand.size, opponentHandCount) {
        checkWinCondition()
    }

    fun endTurn() {
        isPlayerTurn = !isPlayerTurn
        if (!isPlayerTurn && winner == null) {
            gameMessage = "دور $opponentName... يفحص قطعه ويلعب 🤖"
        } else if (isPlayerTurn && winner == null) {
            gameMessage = "دورك الآن! الأطراف المفتوحة: [$leftEnd] و [$rightEnd]"
        }
    }

    // AI Turn simulation
    LaunchedEffect(isPlayerTurn, winner) {
        if (!isPlayerTurn && winner == null) {
            kotlinx.coroutines.delay(1200)
            val currentL = boardChain.first().left
            val currentR = boardChain.last().right

            // Find matching tile in opponent's hand
            val matchLeft = opponentTiles.find { it.left == currentL || it.right == currentL }
            val matchRight = opponentTiles.find { it.left == currentR || it.right == currentR }

            if (matchLeft != null) {
                val tileToPlay = if (matchLeft.right == currentL) matchLeft else matchLeft.reversed()
                boardChain = listOf(tileToPlay) + boardChain
                opponentTiles = opponentTiles.filter { it.id != matchLeft.id }
                opponentHandCount = opponentTiles.size
                gameMessage = "$opponentName لعب [${tileToPlay.left}|${tileToPlay.right}] على الطرف الأيسر."
            } else if (matchRight != null) {
                val tileToPlay = if (matchRight.left == currentR) matchRight else matchRight.reversed()
                boardChain = boardChain + tileToPlay
                opponentTiles = opponentTiles.filter { it.id != matchRight.id }
                opponentHandCount = opponentTiles.size
                gameMessage = "$opponentName لعب [${tileToPlay.left}|${tileToPlay.right}] على الطرف الأيمن."
            } else if (boneyard.isNotEmpty()) {
                // Opponent draws
                val drawn = boneyard.first()
                boneyard = boneyard.drop(1)
                opponentTiles = opponentTiles + drawn
                opponentHandCount = opponentTiles.size
                gameMessage = "$opponentName سحب قطعة من البنك لعدم وجود تطابق."
            } else {
                gameMessage = "$opponentName مرر دوره لعدم توفر قطع مطابقة."
            }

            kotlinx.coroutines.delay(800)
            endTurn()
        }
    }

    fun playTile(tile: DominoTile) {
        if (!isPlayerTurn || winner != null) return

        val currentL = boardChain.first().left
        val currentR = boardChain.last().right

        // Check if matches left end
        if (tile.right == currentL) {
            boardChain = listOf(tile) + boardChain
            playerHand = playerHand.filter { it.id != tile.id }
            gameMessage = "لعبت [${tile.left}|${tile.right}] على الطرف الأيسر بنجاح!"
            endTurn()
        } else if (tile.left == currentL) {
            val rev = tile.reversed()
            boardChain = listOf(rev) + boardChain
            playerHand = playerHand.filter { it.id != tile.id }
            gameMessage = "لعبت [${rev.left}|${rev.right}] على الطرف الأيسر بنجاح!"
            endTurn()
        } else if (tile.left == currentR) {
            boardChain = boardChain + tile
            playerHand = playerHand.filter { it.id != tile.id }
            gameMessage = "لعبت [${tile.left}|${tile.right}] على الطرف الأيمن بنجاح!"
            endTurn()
        } else if (tile.right == currentR) {
            val rev = tile.reversed()
            boardChain = boardChain + rev
            playerHand = playerHand.filter { it.id != tile.id }
            gameMessage = "لعبت [${rev.left}|${rev.right}] على الطرف الأيمن بنجاح!"
            endTurn()
        } else {
            gameMessage = "هذه القطعة لا تطابق أي من الطرفين [$currentL] أو [$currentR]!"
        }
    }

    fun drawTile() {
        if (!isPlayerTurn || winner != null) return
        if (boneyard.isEmpty()) {
            gameMessage = "بنك السحب فارغ! تمرير الدور للخصم."
            endTurn()
            return
        }
        val drawn = boneyard.first()
        boneyard = boneyard.drop(1)
        playerHand = playerHand + drawn
        gameMessage = "سحبت [${drawn.left}|${drawn.right}] من البنك 🀄"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("domino_game_screen")
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("domino_back_button")) {
                Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("لعبة الدومينو الكلاسيكية 🀄", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(
                    text = "${mode.titleAr} • ضد: $opponentName",
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

        // Dominoes Board Surface
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Opponent Hand Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MujtamaTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 16.sp)
                        }
                        Column {
                            Text(opponentName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("متبقي لديه $opponentHandCount قطع", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Opponent back tiles visual
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(opponentHandCount.coerceAtMost(6)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF37474F),
                                modifier = Modifier.size(width = 18.dp, height = 28.dp)
                            ) {}
                        }
                    }
                }

                // Domino Table Center with Chain
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1B2E28))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MujtamaGold.copy(alpha = 0.2f)) {
                                Text("طرف أيسر: $leftEnd", color = MujtamaGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            Text("سلسلة الطاولة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Surface(shape = RoundedCornerShape(8.dp), color = MujtamaGold.copy(alpha = 0.2f)) {
                                Text("طرف أيمن: $rightEnd", color = MujtamaGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }

                        // Horizontal Scrollable Domino Chain
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            items(boardChain) { tile ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFFDE7),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBDBDBD)),
                                    modifier = Modifier.size(width = 44.dp, height = 68.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceEvenly,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("${tile.left}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                                        Divider(color = Color.Black.copy(alpha = 0.3f), thickness = 1.dp)
                                        Text("${tile.right}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }

                // Bank & Draw Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بنك السحب: ${boneyard.size} قطع متبقية",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { drawTile() },
                        enabled = isPlayerTurn && winner == null && boneyard.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MujtamaTeal),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("domino_draw_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("سحب من البنك", fontSize = 11.sp)
                    }
                }
            }
        }

        // Player's Hand Tiles
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "قطعك (اضغط على قطعة مطابقة للعبها):",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(playerHand) { tile ->
                    val isPlayable = isPlayerTurn && winner == null && (
                            tile.left == leftEnd || tile.right == leftEnd ||
                                    tile.left == rightEnd || tile.right == rightEnd
                            )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPlayable) Color(0xFFFFF9C4) else Color(0xFFEEEEEE),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isPlayable) 2.dp else 1.dp,
                            color = if (isPlayable) MujtamaGold else Color.Gray.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .size(width = 46.dp, height = 72.dp)
                            .clickable(enabled = isPlayable) { playTile(tile) }
                            .testTag("domino_player_tile_${tile.id}")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${tile.left}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                            Divider(color = Color.Black.copy(alpha = 0.3f), thickness = 1.dp)
                            Text("${tile.right}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                        }
                    }
                }
            }
        }

        if (winner != null) {
            Button(
                onClick = {
                    val shuffled = allTiles.shuffled()
                    boneyard = shuffled.drop(11)
                    playerHand = shuffled.take(5)
                    opponentTiles = shuffled.drop(5).take(5)
                    opponentHandCount = 5
                    boardChain = listOf(shuffled[10])
                    isPlayerTurn = true
                    winner = null
                    gameMessage = "بدأت جولة دومينو جديدة! طابق الأطراف 🀄"
                },
                colors = ButtonDefaults.buttonColors(containerColor = MujtamaPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("جولة جديدة 🔄", fontWeight = FontWeight.Bold)
            }
        }
    }
}

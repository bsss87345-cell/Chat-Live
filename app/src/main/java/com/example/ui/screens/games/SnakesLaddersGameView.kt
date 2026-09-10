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
import androidx.compose.ui.graphics.Brush
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

@Composable
fun SnakesLaddersGameView(
    mode: GameMatchMode,
    opponentName: String,
    onBack: () -> Unit,
    onWinReward: (Int) -> Unit
) {
    // 36-Square Board (6x6) with Ladders & Snakes
    val ladders = mapOf(
        3 to 14,
        8 to 22,
        17 to 29,
        21 to 33
    )
    val snakes = mapOf(
        16 to 6,
        26 to 11,
        31 to 18,
        35 to 15
    )

    var playerPos by remember { mutableStateOf(1) }
    var opponentPos by remember { mutableStateOf(1) }

    var isPlayerTurn by remember { mutableStateOf(true) }
    var currentDice by remember { mutableStateOf<Int?>(null) }
    var isRolling by remember { mutableStateOf(false) }
    var gameMessage by remember { mutableStateOf("دورك للعب! ارمِ النرد لتتسلق السلالم نحو المربع 36 🪜") }
    var winner by remember { mutableStateOf<String?>(null) }
    var rewardGranted by remember { mutableStateOf(false) }

    // Win evaluation
    LaunchedEffect(playerPos, opponentPos) {
        if (playerPos >= 36 && winner == null) {
            winner = "أنت الفائز! 🏆"
            gameMessage = "ألف مبروك! وصلت للمربع 36 وحققت الفوز في لعبة السلم (+100 نقطة) 🌟"
            if (!rewardGranted) {
                rewardGranted = true
                onWinReward(100)
            }
        } else if (opponentPos >= 36 && winner == null) {
            winner = "$opponentName الفائز! 🤝"
            gameMessage = "سبقك $opponentName إلى المربع النهائي وفاز بالجولة!"
        }
    }

    fun endTurn() {
        isRolling = false
        currentDice = null
        isPlayerTurn = !isPlayerTurn
        if (!isPlayerTurn && winner == null) {
            gameMessage = "دور $opponentName... يرمي النرد 🤖"
        } else if (isPlayerTurn && winner == null) {
            gameMessage = "دورك الآن! اضغط على النرد لرميه 🎲"
        }
    }

    // AI Turn
    LaunchedEffect(isPlayerTurn, winner) {
        if (!isPlayerTurn && winner == null) {
            kotlinx.coroutines.delay(1100)
            val dice = Random.nextInt(1, 7)
            currentDice = dice

            kotlinx.coroutines.delay(800)
            var nextPos = opponentPos + dice
            if (nextPos > 36) nextPos = 36

            // Check ladder or snake
            if (ladders.containsKey(nextPos)) {
                val ladderEnd = ladders[nextPos]!!
                gameMessage = "$opponentName صعد السلم من $nextPos إلى $ladderEnd! 🪜"
                nextPos = ladderEnd
            } else if (snakes.containsKey(nextPos)) {
                val snakeEnd = snakes[nextPos]!!
                gameMessage = "$opponentName لدغه ثعبان وهبط من $nextPos إلى $snakeEnd! 🐍"
                nextPos = snakeEnd
            } else {
                gameMessage = "$opponentName رمى $dice وتقدم إلى المربع $nextPos."
            }

            opponentPos = nextPos
            kotlinx.coroutines.delay(1000)
            endTurn()
        }
    }

    fun rollDice() {
        if (!isPlayerTurn || isRolling || winner != null) return
        isRolling = true
        val dice = Random.nextInt(1, 7)
        currentDice = dice

        var nextPos = playerPos + dice
        if (nextPos > 36) nextPos = 36

        if (ladders.containsKey(nextPos)) {
            val ladderEnd = ladders[nextPos]!!
            gameMessage = "تسلقت السلم بنجاح! من $nextPos إلى $ladderEnd 🪜✨"
            nextPos = ladderEnd
        } else if (snakes.containsKey(nextPos)) {
            val snakeEnd = snakes[nextPos]!!
            gameMessage = "أوه لا! لدغك الثعبان وهبطت من $nextPos إلى $snakeEnd 🐍"
            nextPos = snakeEnd
        } else {
            gameMessage = "رميت $dice وتقدمت إلى المربع $nextPos 🎯"
        }

        playerPos = nextPos
        endTurn()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("snakes_game_screen")
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("snakes_back_button")) {
                Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("لعبة السلم والثعبان 🪜", fontWeight = FontWeight.Black, fontSize = 16.sp)
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

        // 6x6 Board
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
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Board Grid: Rows 6 down to 1
                for (row in 5 downTo 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Alternate row direction for authentic board flow
                        val cols = if (row % 2 == 1) (5 downTo 0) else (0..5)
                        for (col in cols) {
                            val cellNum = row * 6 + col + 1
                            val isPlayerHere = playerPos == cellNum
                            val isOpponentHere = opponentPos == cellNum
                            val isLadderStart = ladders.containsKey(cellNum)
                            val isSnakeStart = snakes.containsKey(cellNum)

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    cellNum == 36 -> MujtamaGold.copy(alpha = 0.4f)
                                    isLadderStart -> Color(0xFF81C784).copy(alpha = 0.35f)
                                    isSnakeStart -> Color(0xFFE57373).copy(alpha = 0.35f)
                                    cellNum % 2 == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                },
                                border = if (cellNum == 36) androidx.compose.foundation.BorderStroke(1.5.dp, MujtamaGold) else null
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$cellNum",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (isLadderStart) {
                                            Text("🪜+${ladders[cellNum]!! - cellNum}", fontSize = 8.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                        } else if (isSnakeStart) {
                                            Text("🐍-${cellNum - snakes[cellNum]!!}", fontSize = 8.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                        }

                                        // Player or opponent tokens
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            if (isPlayerHere) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(MujtamaOnlineGreen),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("أنت", fontSize = 6.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            if (isOpponentHere) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFE53935)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("خصم", fontSize = 6.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Players Positions Summary & Dice Control
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
                // Positions
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MujtamaOnlineGreen))
                        Text("موقعك: المربع $playerPos / 36", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFE53935)))
                        Text("موقع $opponentName: المربع $opponentPos / 36", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Dice Button
                if (winner != null) {
                    Button(
                        onClick = {
                            playerPos = 1
                            opponentPos = 1
                            winner = null
                            isPlayerTurn = true
                            currentDice = null
                            gameMessage = "بدأت جولة جديدة من المربع 1! ارمِ النرد 🎲"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MujtamaPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("جولة جديدة 🔄", fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = { rollDice() },
                        enabled = isPlayerTurn && !isRolling,
                        colors = ButtonDefaults.buttonColors(containerColor = MujtamaGold),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("snakes_dice_button")
                    ) {
                        Text(
                            text = if (currentDice != null) "رميت $currentDice 🎲" else "ارمِ النرد 🎲",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

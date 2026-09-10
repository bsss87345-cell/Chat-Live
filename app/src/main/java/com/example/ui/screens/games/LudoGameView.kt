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

data class LudoPawn(
    val id: Int,
    val isPlayer: Boolean, // true = Player 1 (Green), false = Opponent (Red)
    val position: Int // -1 = In Base, 0..27 = On Track, 28..31 = Home Path, 32 = Goal
)

@Composable
fun LudoGameView(
    mode: GameMatchMode,
    opponentName: String,
    onBack: () -> Unit,
    onWinReward: (Int) -> Unit
) {
    // 2 pawns per side for quick mobile gameplay
    var playerPawns by remember {
        mutableStateOf(listOf(LudoPawn(1, true, -1), LudoPawn(2, true, -1)))
    }
    var opponentPawns by remember {
        mutableStateOf(listOf(LudoPawn(3, false, -1), LudoPawn(4, false, -1)))
    }

    var isPlayerTurn by remember { mutableStateOf(true) }
    var currentDice by remember { mutableStateOf<Int?>(null) }
    var hasRolled by remember { mutableStateOf(false) }
    var gameMessage by remember { mutableStateOf("دورك الآن! اضغط على النرد لرميه 🎲") }
    var winner by remember { mutableStateOf<String?>(null) }
    var rewardGranted by remember { mutableStateOf(false) }

    // Win check
    val playerWon = playerPawns.all { it.position == 32 }
    val opponentWon = opponentPawns.all { it.position == 32 }

    LaunchedEffect(playerWon, opponentWon) {
        if (playerWon && winner == null) {
            winner = "أنت الفائز! 🎉"
            gameMessage = "تهانينا! فزت في جولة لودو وحصلت على +100 نقطة نظام 🌟"
            if (!rewardGranted) {
                rewardGranted = true
                onWinReward(100)
            }
        } else if (opponentWon && winner == null) {
            winner = "$opponentName الفائز! 🤝"
            gameMessage = "فاز $opponentName في هذه الجولة. حظاً أوفر في المرة القادمة!"
        }
    }

    fun endTurn() {
        hasRolled = false
        currentDice = null
        isPlayerTurn = !isPlayerTurn
        if (!isPlayerTurn && winner == null) {
            gameMessage = "دور $opponentName... يفكر في نقلته 🤖"
        } else if (isPlayerTurn && winner == null) {
            gameMessage = "دورك الآن! اضغط لرمي النرد 🎲"
        }
    }

    // Opponent Auto Move
    LaunchedEffect(isPlayerTurn, winner) {
        if (!isPlayerTurn && winner == null) {
            kotlinx.coroutines.delay(900)
            val dice = Random.nextInt(1, 7)
            currentDice = dice
            hasRolled = true

            kotlinx.coroutines.delay(800)
            // AI decision: try to move a pawn
            val basePawn = opponentPawns.find { it.position == -1 }
            val trackPawn = opponentPawns.find { it.position in 0..31 }

            if (dice == 6 && basePawn != null && Random.nextBoolean()) {
                opponentPawns = opponentPawns.map {
                    if (it.id == basePawn.id) it.copy(position = 14) else it // Opponent starts at track 14
                }
                gameMessage = "$opponentName أخرج حجراً من القاعدة برقم 6!"
            } else if (trackPawn != null) {
                val nextPos = (trackPawn.position + dice).coerceAtMost(32)
                opponentPawns = opponentPawns.map {
                    if (it.id == trackPawn.id) it.copy(position = nextPos) else it
                }
                // Check capture
                playerPawns = playerPawns.map { p ->
                    if (p.position == nextPos && nextPos < 28) {
                        gameMessage = "قام $opponentName بإسقاط حجرك وإرجاعه للقاعدة!"
                        p.copy(position = -1)
                    } else p
                }
            } else {
                gameMessage = "$opponentName لم يستطع التحرك بالنرد $dice."
            }

            kotlinx.coroutines.delay(1000)
            endTurn()
        }
    }

    fun rollDice() {
        if (hasRolled || !isPlayerTurn || winner != null) return
        val dice = Random.nextInt(1, 7)
        currentDice = dice
        hasRolled = true

        val canExitBase = dice == 6 && playerPawns.any { it.position == -1 }
        val canMoveTrack = playerPawns.any { it.position in 0..31 && it.position + dice <= 32 }

        if (!canExitBase && !canMoveTrack) {
            gameMessage = "رميت $dice! لا توجد نقلات متاحة، ينتقل الدور للخصم."
            // Delay and switch
            hasRolled = false
            currentDice = null
            isPlayerTurn = false
            gameMessage = "دور $opponentName... يفكر في نقلته 🤖"
        } else {
            gameMessage = "رميت $dice! اضغط على أحد أحجارك الخضراء لتحريكه 🟢"
        }
    }

    fun movePlayerPawn(pawn: LudoPawn) {
        val dice = currentDice ?: return
        if (!hasRolled || !isPlayerTurn || winner != null) return

        if (pawn.position == -1) {
            if (dice == 6) {
                playerPawns = playerPawns.map {
                    if (it.id == pawn.id) it.copy(position = 0) else it
                }
                gameMessage = "أحسنت! أخرجت حجراً إلى نقطة البداية 🚀"
                // Capturing opponent
                opponentPawns = opponentPawns.map { opp ->
                    if (opp.position == 0) {
                        gameMessage = "رائع! قمت بإسقاط حجر $opponentName وإعادته لقاعدته! 💥"
                        opp.copy(position = -1)
                    } else opp
                }
                endTurn()
            }
        } else if (pawn.position in 0..31) {
            val newPos = pawn.position + dice
            if (newPos <= 32) {
                playerPawns = playerPawns.map {
                    if (it.id == pawn.id) it.copy(position = newPos) else it
                }
                // Check capture
                opponentPawns = opponentPawns.map { opp ->
                    if (opp.position == newPos && newPos < 28) {
                        gameMessage = "ضربة ممتازة! أسقطت حجر $opponentName وأرجعته للقاعدة! 🎯"
                        opp.copy(position = -1)
                    } else opp
                }
                if (newPos == 32) {
                    gameMessage = "وصل أحد أحجارك إلى الهدف النهائي بنجاح! 🏆"
                }
                endTurn()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ludo_game_screen")
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Game Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("ludo_back_button")) {
                Icon(Icons.Default.ArrowForward, contentDescription = "رجوع للألعاب")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("لعبة لودو الكلاسيكية 🎲", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(
                    text = "${mode.titleAr} • ضد: $opponentName",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MujtamaGold.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "+100 نقطة",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MujtamaGold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Status / Feedback Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (winner != null) MujtamaGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = gameMessage,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(10.dp),
                color = if (winner != null) MujtamaGold else MaterialTheme.colorScheme.onSurface
            )
        }

        // Ludo Board representation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Opponent Base Row (Red)
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
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔴", fontSize = 16.sp)
                        }
                        Column {
                            Text(opponentName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                if (!isPlayerTurn) "يفكر الآن..." else "في الانتظار",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Opponent Bases
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        opponentPawns.forEach { pawn ->
                            Surface(
                                shape = CircleShape,
                                color = if (pawn.position == -1) Color(0xFFE53935).copy(alpha = 0.3f)
                                else if (pawn.position == 32) MujtamaGold
                                else Color(0xFFE53935),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (pawn.position == -1) "بيت" else if (pawn.position == 32) "فوز" else "${pawn.position}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Middle Board Track (Visual Track Grid)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF1F1836),
                                    Color(0xFF2A2048)
                                )
                            )
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "مسار المنافسة والهدف 🎯",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        // Goal Center
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MujtamaGold.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Stars, contentDescription = null, tint = MujtamaGold, modifier = Modifier.size(16.dp))
                                Text("نقطة الوصول الآمنة (المربع 32)", fontSize = 11.sp, color = MujtamaGold, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Track visual summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("أحجارك الخضراء", fontSize = 10.sp, color = MujtamaOnlineGreen)
                                playerPawns.forEach {
                                    Text(
                                        text = when (it.position) {
                                            -1 -> "في القاعدة (يحتاج 6 للخروج)"
                                            32 -> "وصل الهدف بنجاح 🏆"
                                            else -> "المربع ${it.position}/32"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("أحجار الخصم", fontSize = 10.sp, color = Color(0xFFEF5350))
                                opponentPawns.forEach {
                                    Text(
                                        text = when (it.position) {
                                            -1 -> "في القاعدة"
                                            32 -> "وصل الهدف 🏆"
                                            else -> "المربع ${it.position}/32"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Player Base Row (Green)
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
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MujtamaOnlineGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🟢", fontSize = 16.sp)
                        }
                        Column {
                            Text("أنت (اللاعب الأخضر)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                if (isPlayerTurn) "دورك للعب 🎯" else "انتظر الخصم",
                                fontSize = 10.sp,
                                color = if (isPlayerTurn) MujtamaOnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Player Pawns (Clickable when dice rolled)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        playerPawns.forEach { pawn ->
                            val canMove = hasRolled && isPlayerTurn && (
                                    (pawn.position == -1 && currentDice == 6) ||
                                            (pawn.position in 0..31 && pawn.position + (currentDice ?: 0) <= 32)
                                    )
                            Surface(
                                shape = CircleShape,
                                color = if (pawn.position == 32) MujtamaGold
                                else if (canMove) MujtamaOnlineGreen
                                else MujtamaOnlineGreen.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable(enabled = canMove) { movePlayerPawn(pawn) }
                                    .then(if (canMove) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                                    .testTag("ludo_pawn_${pawn.id}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (pawn.position == -1) "قاعدة" else if (pawn.position == 32) "فوز" else "${pawn.position}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dice & Controls Bottom Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dice Visual
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isPlayerTurn) MujtamaGold else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = isPlayerTurn && !hasRolled && winner == null) { rollDice() }
                        .testTag("ludo_dice_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (currentDice) {
                            1 -> "⚀"
                            2 -> "⚁"
                            3 -> "⚂"
                            4 -> "⚃"
                            5 -> "⚄"
                            6 -> "⚅"
                            else -> "🎲"
                        },
                        fontSize = 32.sp,
                        color = if (isPlayerTurn) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        text = if (currentDice != null) "قيمة النرد: $currentDice" else "انقر لرمي النرد",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (hasRolled) "اختر الحجر الأخضر لتحريكه" else "رقم 6 يخرج حجراً جديداً من القاعدة",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (winner != null) {
                    Button(
                        onClick = {
                            playerPawns = listOf(LudoPawn(1, true, -1), LudoPawn(2, true, -1))
                            opponentPawns = listOf(LudoPawn(3, false, -1), LudoPawn(4, false, -1))
                            winner = null
                            hasRolled = false
                            currentDice = null
                            isPlayerTurn = true
                            gameMessage = "بدأت جولة جديدة! ارمِ النرد 🎲"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MujtamaPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("جولة جديدة 🔄", fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = { rollDice() },
                        enabled = isPlayerTurn && !hasRolled,
                        colors = ButtonDefaults.buttonColors(containerColor = MujtamaGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ارمِ النرد", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

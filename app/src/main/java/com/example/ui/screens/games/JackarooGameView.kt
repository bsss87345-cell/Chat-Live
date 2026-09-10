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

data class JackarooCard(
    val id: String,
    val value: Int, // 1=قص (A), 4=أربعة عكسية, 7=سبعة, 11=ولد (J), 12=بنت (Q), 13=شايب (K), or 2..10
    val labelAr: String,
    val description: String,
    val suit: String = "♠"
)

data class JackarooMarble(
    val id: Int,
    val isPlayer: Boolean,
    val position: Int // -1 = In Base, 0..23 = Track, 24 = Home / Safe Winner Zone
)

@Composable
fun JackarooGameView(
    mode: GameMatchMode,
    opponentName: String,
    onBack: () -> Unit,
    onWinReward: (Int) -> Unit
) {
    val deckPool = listOf(
        JackarooCard("c1", 1, "قص (A)", "يخرج حجراً من القاعدة أو يتقدم خطوة واحدة", "♦"),
        JackarooCard("c2", 4, "4 عكسية", "يرجع الحجر 4 خطوات للخلف استراتيجياً!", "♣"),
        JackarooCard("c3", 7, "7", "يتقدم 7 خطوات إلى الأمام", "♥"),
        JackarooCard("c4", 11, "ولد (J)", "تبديل مكان حجرك مع حجر الخصم على المسار!", "♠"),
        JackarooCard("c5", 13, "شايب (K)", "يخرج حجراً من القاعدة أو يتقدم 13 خطوة!", "♥"),
        JackarooCard("c6", 5, "5", "يتقدم 5 خطوات", "♦"),
        JackarooCard("c7", 10, "10", "يتقدم 10 خطوات", "♣"),
        JackarooCard("c8", 12, "بنت (Q)", "يتقدم 12 خطوة سريعة", "♠")
    )

    fun dealCards(count: Int): List<JackarooCard> {
        return (1..count).map { deckPool.random().copy(id = "card_${System.currentTimeMillis()}_${Random.nextInt(1000)}") }
    }

    var playerMarbles by remember {
        mutableStateOf(listOf(JackarooMarble(1, true, -1), JackarooMarble(2, true, -1)))
    }
    var opponentMarbles by remember {
        mutableStateOf(listOf(JackarooMarble(3, false, -1), JackarooMarble(4, false, -1)))
    }

    var playerHand by remember { mutableStateOf(dealCards(3)) }
    var selectedCard by remember { mutableStateOf<JackarooCard?>(null) }
    var isPlayerTurn by remember { mutableStateOf(true) }
    var gameMessage by remember { mutableStateOf("اختر ورقة من يدك، ثم انقر على حجر لتحريكه 🃏") }
    var winner by remember { mutableStateOf<String?>(null) }
    var rewardGranted by remember { mutableStateOf(false) }

    // Win evaluation
    val playerWon = playerMarbles.all { it.position == 24 }
    val opponentWon = opponentMarbles.all { it.position == 24 }

    LaunchedEffect(playerWon, opponentWon) {
        if (playerWon && winner == null) {
            winner = "أنت الفائز في جاكارو! 👑"
            gameMessage = "كفو! أوصلت أحجارك لمنطقة الأمان وحصلت على +100 نقطة نظام 🌟"
            if (!rewardGranted) {
                rewardGranted = true
                onWinReward(100)
            }
        } else if (opponentWon && winner == null) {
            winner = "$opponentName الفائز! 🤝"
            gameMessage = "فاز $opponentName بالجولة، جاكارو تحتاج لرد الهجوم!"
        }
    }

    fun endTurn() {
        selectedCard = null
        isPlayerTurn = !isPlayerTurn
        if (playerHand.isEmpty()) {
            playerHand = dealCards(3)
        }
        if (!isPlayerTurn && winner == null) {
            gameMessage = "دور $opponentName... يلعب ورقته الاستراتيجية 🤖"
        } else if (isPlayerTurn && winner == null) {
            gameMessage = "دورك الآن! اختر بطاقة من يدك 🃏"
        }
    }

    // AI Turn Simulation
    LaunchedEffect(isPlayerTurn, winner) {
        if (!isPlayerTurn && winner == null) {
            kotlinx.coroutines.delay(1200)
            val aiCard = deckPool.random()
            val baseMarble = opponentMarbles.find { it.position == -1 }
            val trackMarble = opponentMarbles.find { it.position in 0..23 }

            if ((aiCard.value == 13 || aiCard.value == 1) && baseMarble != null) {
                opponentMarbles = opponentMarbles.map {
                    if (it.id == baseMarble.id) it.copy(position = 12) else it
                }
                gameMessage = "$opponentName لعب '${aiCard.labelAr}' وأخرج حجراً إلى المسار!"
            } else if (trackMarble != null) {
                val nextPos = when (aiCard.value) {
                    4 -> (trackMarble.position - 4).coerceAtLeast(0)
                    11 -> {
                        // Swap with player marble if exists
                        val targetPlayer = playerMarbles.find { it.position in 0..23 }
                        if (targetPlayer != null) {
                            val temp = trackMarble.position
                            playerMarbles = playerMarbles.map { if (it.id == targetPlayer.id) it.copy(position = temp) else it }
                            targetPlayer.position
                        } else (trackMarble.position + 3).coerceAtMost(24)
                    }
                    else -> (trackMarble.position + aiCard.value).coerceAtMost(24)
                }
                opponentMarbles = opponentMarbles.map {
                    if (it.id == trackMarble.id) it.copy(position = nextPos) else it
                }
                gameMessage = "$opponentName لعب '${aiCard.labelAr}' وحرك حجره إلى المربع $nextPos!"
            } else {
                gameMessage = "$opponentName تخلص من ورقة لعدم وجود حركة مناسبة."
            }

            kotlinx.coroutines.delay(1000)
            endTurn()
        }
    }

    fun playCardOnMarble(marble: JackarooMarble) {
        val card = selectedCard ?: return
        if (!isPlayerTurn || winner != null) return

        if (marble.position == -1) {
            // Can only exit with King (13) or Ace (1)
            if (card.value == 13 || card.value == 1) {
                playerMarbles = playerMarbles.map {
                    if (it.id == marble.id) it.copy(position = 0) else it
                }
                playerHand = playerHand.filter { it.id != card.id }
                gameMessage = "أخرجت حجراً بـ ${card.labelAr} إلى نقطة البداية 🚀"
                endTurn()
            } else {
                gameMessage = "لإخراج حجر من القاعدة تحتاج إلى الشايب (K) أو القص (A)!"
            }
        } else if (marble.position in 0..23) {
            val newPos = when (card.value) {
                4 -> (marble.position - 4).coerceAtLeast(0)
                11 -> {
                    // Jack: swap
                    val opponentOnTrack = opponentMarbles.find { it.position in 0..23 }
                    if (opponentOnTrack != null) {
                        val playerPos = marble.position
                        val oppPos = opponentOnTrack.position
                        opponentMarbles = opponentMarbles.map { if (it.id == opponentOnTrack.id) it.copy(position = playerPos) else it }
                        oppPos
                    } else (marble.position + 3).coerceAtMost(24)
                }
                else -> (marble.position + card.value).coerceAtMost(24)
            }

            playerMarbles = playerMarbles.map {
                if (it.id == marble.id) it.copy(position = newPos) else it
            }
            playerHand = playerHand.filter { it.id != card.id }

            // Check if captured opponent
            opponentMarbles = opponentMarbles.map { opp ->
                if (opp.position == newPos && newPos < 24) {
                    gameMessage = "ضربة جاكارو معلم! أكلت حجر $opponentName وأرجعته لقاعدته 💥"
                    opp.copy(position = -1)
                } else opp
            }

            if (newPos == 24) {
                gameMessage = "ممتاز! حجر إضافي دخل بيت الأمان بأمان 🏆"
            }
            endTurn()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("jackaroo_game_screen")
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
            IconButton(onClick = onBack, modifier = Modifier.testTag("jackaroo_back_button")) {
                Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("تحدي جاكارو (Jackaroo) 🃏", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(
                    text = "${mode.titleAr} • الخصم: $opponentName",
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

        // Board representation
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
                // Opponent Status
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
                            Text("🔵", fontSize = 15.sp)
                        }
                        Column {
                            Text(opponentName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(if (!isPlayerTurn) "يفكر في ورقة..." else "ينتظرك", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        opponentMarbles.forEach { marble ->
                            Surface(
                                shape = CircleShape,
                                color = if (marble.position == -1) MujtamaTeal.copy(alpha = 0.3f)
                                else if (marble.position == 24) MujtamaGold else MujtamaTeal,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (marble.position == -1) "قاعدة" else if (marble.position == 24) "بيت" else "${marble.position}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Center Board Track Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF2C1E4A), Color(0xFF1B1230)))
                        )
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("طاولة جاكارو الملكية 🛡️", color = MujtamaGold, fontWeight = FontWeight.Black, fontSize = 12.sp)

                        // Safe Home Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MujtamaGold.copy(alpha = 0.25f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "🎯 منطقة الأمان والبيت النهائي (المربع 24)",
                                color = MujtamaGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("أحجارك البنفسجية", fontSize = 10.sp, color = Color(0xFFB388FF))
                                playerMarbles.forEach {
                                    Text(
                                        text = when (it.position) {
                                            -1 -> "في القاعدة (شايب/قص)"
                                            24 -> "في بيت الأمان 🏆"
                                            else -> "الموقع ${it.position}/24"
                                        },
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("أحجار الخصم", fontSize = 10.sp, color = MujtamaTeal)
                                opponentMarbles.forEach {
                                    Text(
                                        text = when (it.position) {
                                            -1 -> "في القاعدة"
                                            24 -> "في بيت الأمان 🏆"
                                            else -> "الموقع ${it.position}/24"
                                        },
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Player Status and Marbles
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
                                .background(Color(0xFF7C4DFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🟣", fontSize = 15.sp)
                        }
                        Column {
                            Text("أنت (اللاعب البنفسجي)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                if (isPlayerTurn) (if (selectedCard != null) "انقر الحجر للتحريك!" else "اختر ورقة من الأسفل") else "انتظر الخصم",
                                fontSize = 10.sp,
                                color = if (isPlayerTurn) MujtamaGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Clickable Player Marbles
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        playerMarbles.forEach { marble ->
                            val canClick = isPlayerTurn && selectedCard != null && (
                                    (marble.position == -1 && (selectedCard?.value == 13 || selectedCard?.value == 1)) ||
                                            marble.position in 0..23
                                    )
                            Surface(
                                shape = CircleShape,
                                color = if (marble.position == 24) MujtamaGold
                                else if (canClick) Color(0xFF7C4DFF)
                                else Color(0xFF7C4DFF).copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable(enabled = canClick) { playCardOnMarble(marble) }
                                    .then(if (canClick) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                                    .testTag("jackaroo_marble_${marble.id}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (marble.position == -1) "قاعدة" else if (marble.position == 24) "بيت" else "${marble.position}",
                                        fontSize = 9.sp,
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

        // Player's Cards Hand
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "أوراق اللعب الخاصة بك (انقر لتحديد الورقة):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                playerHand.forEach { card ->
                    val isSelected = selectedCard?.id == card.id
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = isPlayerTurn && winner == null) {
                                selectedCard = if (isSelected) null else card
                            }
                            .then(
                                if (isSelected) Modifier.border(2.dp, MujtamaGold, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                            .testTag("jackaroo_card_${card.value}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(card.suit, fontSize = 16.sp, color = if (card.suit in listOf("♥", "♦")) Color.Red else Color.Black)
                            Text(card.labelAr, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(card.description, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        if (winner != null) {
            Button(
                onClick = {
                    playerMarbles = listOf(JackarooMarble(1, true, -1), JackarooMarble(2, true, -1))
                    opponentMarbles = listOf(JackarooMarble(3, false, -1), JackarooMarble(4, false, -1))
                    playerHand = dealCards(3)
                    selectedCard = null
                    isPlayerTurn = true
                    winner = null
                    gameMessage = "بدأت جولة جاكارو جديدة! اختر ورقتك 🃏"
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

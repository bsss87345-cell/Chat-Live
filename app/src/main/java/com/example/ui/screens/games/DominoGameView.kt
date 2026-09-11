package com.example.ui.screens.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMatchMode
import com.example.ui.theme.MujtamaGold
import com.example.ui.theme.MujtamaGoldLight
import com.example.ui.theme.MujtamaOnlineGreen
import com.example.ui.theme.MujtamaPrimary
import kotlinx.coroutines.delay

/**
 * بيانات حجر الدومينو الكلاسيكي
 */
data class DominoTile(
    val id: String,
    val left: Int,
    val right: Int
) {
    val pipSum: Int get() = left + right
    val isDouble: Boolean get() = left == right
    fun reversed(): DominoTile = DominoTile(id, right, left)
}

enum class TileOrientation {
    HORIZONTAL,
    VERTICAL
}

data class PlacedBoardTile(
    val tile: DominoTile,
    val orientation: TileOrientation = TileOrientation.HORIZONTAL
)

/**
 * شاشة طاولة الدومينو لشخصين (1v1) بالطراز الفاخر:
 * - لاعب في الأسفل (أنت) ولاعب في الأعلى (الخصم) فقط، دون مقاعد يمين أو يسار.
 * - إطار خشبي داكن فخم بزخارف نباتية ذهبية في الأركان وسطح لعب أخضر زمردي عميق.
 * - إخفاء تام لقطع الخصم (رف خشبي مقفل ومكتوب عليه عدد القطع دون إظهار أي قيم).
 * - ظهور القطع فقط في المنطقة المركزية عند لعبها.
 * - بطاقة مجموع النقاط لكل لاعب في زاويته.
 * - أيقونات أسفل يمين الشاشة (الدردشة، الخروج، الإعدادات).
 */
@Composable
fun DominoGameView(
    mode: GameMatchMode,
    opponentName: String,
    onBack: () -> Unit,
    onWinReward: (Int) -> Unit
) {
    val displayName = remember(opponentName) {
        if (opponentName.isNotBlank()) opponentName else "علي الشمري"
    }

    // تجهيز أحجار الدومينو الـ 28 (Double-Six)
    val initialDeck = remember {
        val list = mutableListOf<DominoTile>()
        var counter = 1
        for (i in 0..6) {
            for (j in i..6) {
                list.add(DominoTile("t_${counter++}", i, j))
            }
        }
        list.shuffled()
    }

    var userTiles by remember { mutableStateOf(initialDeck.subList(0, 7)) }
    var opponentTiles by remember { mutableStateOf(initialDeck.subList(7, 14)) }
    var boneyardTiles by remember { mutableStateOf(initialDeck.subList(14, 28)) }

    // حجر افتتاح الجولة في منتصف الطاولة
    val startingDouble = remember {
        val combined = userTiles + opponentTiles
        combined.filter { it.isDouble }.maxByOrNull { it.pipSum } ?: DominoTile("start", 6, 6)
    }

    LaunchedEffect(Unit) {
        if (userTiles.any { it.id == startingDouble.id }) {
            userTiles = userTiles.filter { it.id != startingDouble.id }
        } else if (opponentTiles.any { it.id == startingDouble.id }) {
            opponentTiles = opponentTiles.filter { it.id != startingDouble.id }
        }
    }

    var boardChain by remember {
        mutableStateOf(
            listOf(
                PlacedBoardTile(
                    tile = startingDouble,
                    orientation = if (startingDouble.isDouble) TileOrientation.VERTICAL else TileOrientation.HORIZONTAL
                )
            )
        )
    }

    val leftEnd = boardChain.first().tile.left
    val rightEnd = boardChain.last().tile.right

    // 0 = دور اللاعب الحالي، 1 = دور الخصم
    var isUserTurn by remember { mutableStateOf(true) }
    var turnTimeRemaining by remember { mutableIntStateOf(60) }

    var userScore by remember { mutableIntStateOf(50) }
    var opponentScore by remember { mutableIntStateOf(45) }
    var userTotalPoints by remember { mutableIntStateOf(125) }
    var opponentTotalPoints by remember { mutableIntStateOf(110) }

    var statusMessage by remember { mutableStateOf("دورك للعب! اختر حجراً مناسباً للطرف [$leftEnd] أو [$rightEnd]") }
    var winnerName by remember { mutableStateOf<String?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showChatOverlay by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedEndDialogTile by remember { mutableStateOf<DominoTile?>(null) }

    // عداد الوقت للدور الحالي
    LaunchedEffect(isUserTurn) {
        turnTimeRemaining = 60
        while (turnTimeRemaining > 0 && winnerName == null) {
            delay(1000)
            turnTimeRemaining--
        }
        if (turnTimeRemaining == 0 && winnerName == null) {
            statusMessage = if (isUserTurn) "انتهى وقتك! تم تمرير الدور للخصم." else "انتهى وقت الخصم وتم تمرير الدور إليك."
            isUserTurn = !isUserTurn
        }
    }

    // فحص الفوز
    fun checkWinCondition() {
        if (winnerName != null) return
        if (userTiles.isEmpty()) {
            winnerName = "أنت"
            statusMessage = "🎉 مبروك! لقد أنهيت جميع قطعك وفزت بالجولة!"
            userScore += 50
            userTotalPoints += 50
            onWinReward(100)
        } else if (opponentTiles.isEmpty()) {
            winnerName = displayName
            statusMessage = "فاز $displayName بالجولة!"
            opponentScore += 50
            opponentTotalPoints += 50
        }
    }

    LaunchedEffect(userTiles.size, opponentTiles.size) {
        checkWinCondition()
    }

    // دور الخصم الذكي (AI)
    LaunchedEffect(isUserTurn, winnerName) {
        if (winnerName != null) return@LaunchedEffect

        if (!isUserTurn) {
            delay(1400)
            val currentL = boardChain.first().tile.left
            val currentR = boardChain.last().tile.right

            val matchLeft = opponentTiles.find { it.left == currentL || it.right == currentL }
            val matchRight = opponentTiles.find { it.left == currentR || it.right == currentR }

            if (matchLeft != null) {
                val tileToPlay = if (matchLeft.right == currentL) matchLeft else matchLeft.reversed()
                val orientation = if (tileToPlay.isDouble) TileOrientation.VERTICAL else TileOrientation.HORIZONTAL
                boardChain = listOf(PlacedBoardTile(tileToPlay, orientation)) + boardChain
                opponentTiles = opponentTiles.filter { it.id != matchLeft.id }
                opponentScore += tileToPlay.pipSum
                statusMessage = "$displayName لعب [${tileToPlay.left}|${tileToPlay.right}] على الطرف الأيسر."
                delay(600)
                isUserTurn = true
            } else if (matchRight != null) {
                val tileToPlay = if (matchRight.left == currentR) matchRight else matchRight.reversed()
                val orientation = if (tileToPlay.isDouble) TileOrientation.VERTICAL else TileOrientation.HORIZONTAL
                boardChain = boardChain + PlacedBoardTile(tileToPlay, orientation)
                opponentTiles = opponentTiles.filter { it.id != matchRight.id }
                opponentScore += tileToPlay.pipSum
                statusMessage = "$displayName لعب [${tileToPlay.left}|${tileToPlay.right}] على الطرف الأيمن."
                delay(600)
                isUserTurn = true
            } else if (boneyardTiles.isNotEmpty()) {
                val drawnTile = boneyardTiles.first()
                boneyardTiles = boneyardTiles.drop(1)
                opponentTiles = opponentTiles + drawnTile
                statusMessage = "$displayName سحب قطعة من بنك السحب."
                delay(800)
                // فحص إذا كان بإمكان الخصم لعب القطعة المسحوبة
                if (drawnTile.left == currentL || drawnTile.right == currentL) {
                    val tileToPlay = if (drawnTile.right == currentL) drawnTile else drawnTile.reversed()
                    val orientation = if (tileToPlay.isDouble) TileOrientation.VERTICAL else TileOrientation.HORIZONTAL
                    boardChain = listOf(PlacedBoardTile(tileToPlay, orientation)) + boardChain
                    opponentTiles = opponentTiles.filter { it.id != drawnTile.id }
                    opponentScore += tileToPlay.pipSum
                    statusMessage = "$displayName سحب ولعب [${tileToPlay.left}|${tileToPlay.right}] على الطرف الأيسر."
                } else if (drawnTile.left == currentR || drawnTile.right == currentR) {
                    val tileToPlay = if (drawnTile.left == currentR) drawnTile else drawnTile.reversed()
                    val orientation = if (tileToPlay.isDouble) TileOrientation.VERTICAL else TileOrientation.HORIZONTAL
                    boardChain = boardChain + PlacedBoardTile(tileToPlay, orientation)
                    opponentTiles = opponentTiles.filter { it.id != drawnTile.id }
                    opponentScore += tileToPlay.pipSum
                    statusMessage = "$displayName سحب ولعب [${tileToPlay.left}|${tileToPlay.right}] على الطرف الأيمن."
                }
                delay(500)
                isUserTurn = true
            } else {
                statusMessage = "$displayName لا يملك حركة مناسبة ومرّر الدور."
                delay(600)
                isUserTurn = true
            }
        }
    }

    fun executePlayerPlay(tile: DominoTile, playOnLeft: Boolean) {
        val currentL = boardChain.first().tile.left
        val currentR = boardChain.last().tile.right

        if (playOnLeft) {
            val orientedTile = if (tile.right == currentL) tile else tile.reversed()
            val orientation = if (orientedTile.isDouble) TileOrientation.VERTICAL else TileOrientation.HORIZONTAL
            boardChain = listOf(PlacedBoardTile(orientedTile, orientation)) + boardChain
            userTiles = userTiles.filter { it.id != tile.id }
            userScore += orientedTile.pipSum
            statusMessage = "لعبت [${orientedTile.left}|${orientedTile.right}] بنجاح!"
            isUserTurn = false
        } else {
            val orientedTile = if (tile.left == currentR) tile else tile.reversed()
            val orientation = if (orientedTile.isDouble) TileOrientation.VERTICAL else TileOrientation.HORIZONTAL
            boardChain = boardChain + PlacedBoardTile(orientedTile, orientation)
            userTiles = userTiles.filter { it.id != tile.id }
            userScore += orientedTile.pipSum
            statusMessage = "لعبت [${orientedTile.left}|${orientedTile.right}] بنجاح!"
            isUserTurn = false
        }
    }

    fun onUserTileClicked(tile: DominoTile) {
        if (!isUserTurn || winnerName != null) {
            statusMessage = "انتظر دورك للعب!"
            return
        }

        val currentL = boardChain.first().tile.left
        val currentR = boardChain.last().tile.right

        val canPlayLeft = tile.left == currentL || tile.right == currentL
        val canPlayRight = tile.left == currentR || tile.right == currentR

        if (canPlayLeft && canPlayRight && currentL != currentR) {
            selectedEndDialogTile = tile
        } else if (canPlayLeft) {
            executePlayerPlay(tile, playOnLeft = true)
        } else if (canPlayRight) {
            executePlayerPlay(tile, playOnLeft = false)
        } else {
            statusMessage = "هذا الحجر لا يطابق أي من الطرفين المفتوحين [$currentL] أو [$currentR] ⚠️"
        }
    }

    fun onDrawBoneyardClick() {
        if (!isUserTurn || winnerName != null) return
        if (boneyardTiles.isNotEmpty()) {
            val drawn = boneyardTiles.first()
            boneyardTiles = boneyardTiles.drop(1)
            userTiles = userTiles + drawn
            statusMessage = "سحبت حجراً جديداً من البنك."
        } else {
            statusMessage = "بنك السحب فارغ! إذا لم تكن تملك حركة، يمكنك تمرير الدور."
        }
    }

    val formattedTurnTime = remember(turnTimeRemaining) {
        val min = turnTimeRemaining / 60
        val sec = turnTimeRemaining % 60
        "$min:${if (sec < 10) "0$sec" else "$sec"}"
    }

    // واجهة الطاولة الرئيسية لشخصين (1v1)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF140804))
            .testTag("domino_2player_table_screen")
    ) {
        // خلفية الطاولة الخشبية الفاخرة وسطح الجوخ الأخضر والزخارف الذهبية
        LuxuryDominoTableCanvas(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // -------------------------------------------------------------
            // 1. المنطقة العلوية: الخصم + بطاقة مجموع نقاطه في الزاوية
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // بطاقة إحصائيات الجولة (1v1)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF2C150A),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFC7985D))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "⚔️", fontSize = 13.sp)
                        Text(
                            text = "مباراة ثنائية",
                            color = Color(0xFFF7E2C6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // مقعد الخصم في الأعلى + رفه المخفي بالكامل
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    PlayerSeatBadge2P(
                        name = displayName,
                        score = opponentScore,
                        timeStr = if (!isUserTurn) formattedTurnTime else "1:00",
                        avatarEmoji = "👨",
                        avatarBg = Color(0xFF2E7D32),
                        isCurrentTurn = !isUserTurn
                    )

                    // رف الخصم: قطع مخفية تماماً دون كشف قيمها مع إظهار عددها فقط
                    OpponentConcealedWoodRack(count = opponentTiles.size)
                }

                // بطاقة مجموع نقاط الخصم في الزاوية المقابلة
                TotalPointsBadgeWood2P(
                    title = "مجموع النقاط",
                    score = opponentTotalPoints
                )
            }

            // -------------------------------------------------------------
            // 2. المنطقة المركزية: سطح اللعب وسلسلة الأحجار الموضوعة
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // شارات الأطراف المفتوحة
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OpenEndBadgeLuxury(endValue = leftEnd, label = "الطرف الأيسر")
                        OpenEndBadgeLuxury(endValue = rightEnd, label = "الطرف الأيمن")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // شريط التمرير الأفقي للأحجار في المنتصف
                    val boardScroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(boardScroll)
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        boardChain.forEachIndexed { index, placed ->
                            ClassicDominoTileView2P(
                                tile = placed.tile,
                                orientation = placed.orientation,
                                isLeftEnd = index == 0,
                                isRightEnd = index == boardChain.size - 1
                            )
                            if (index < boardChain.size - 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // بنك السحب وزر السحب التفاعلي
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { onDrawBoneyardClick() },
                            enabled = isUserTurn && boneyardTiles.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF3B1C0F),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFC7985D)),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "🀄", fontSize = 13.sp)
                                Text(
                                    text = "سحب من البنك (${boneyardTiles.size})",
                                    color = if (isUserTurn && boneyardTiles.isNotEmpty()) MujtamaGoldLight else Color(0xFFB09988),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isUserTurn && !userTiles.any { it.left == leftEnd || it.right == leftEnd || it.left == rightEnd || it.right == rightEnd } && boneyardTiles.isEmpty()) {
                            // زر تمرير الدور عند عدم وجود حركة وبنك فارغ
                            Surface(
                                onClick = {
                                    statusMessage = "مرّرت دورك لعدم توفر حركة."
                                    isUserTurn = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF5E1B1B),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE57373))
                            ) {
                                Text(
                                    text = "تمرير الدور",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. المنطقة السفلية: المستخدم الحالي + رفه الواضح + أيقونات التحكم
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // شريط رسالة الحالة والتعليمات
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E1008).copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isUserTurn) Color(0xFFD4AF37) else Color(0xFFC7985D).copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = statusMessage,
                        color = if (isUserTurn) Color(0xFFFFF2D6) else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // صف مقعد المستخدم الحالي + بطاقة مجموع نقاطه
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // بطاقة مجموع نقاط اللاعب الحالي (في الزاوية السفلية المقابلة)
                    TotalPointsBadgeWood2P(
                        title = "مجموع نقاطك",
                        score = userTotalPoints
                    )

                    // مقعد اللاعب الحالي في الأسفل
                    PlayerSeatBadge2P(
                        name = "أنت",
                        score = userScore,
                        timeStr = if (isUserTurn) formattedTurnTime else "1:00",
                        avatarEmoji = "😎",
                        avatarBg = Color(0xFF1976D2),
                        isCurrentTurn = isUserTurn
                    )

                    // موازنة المحاذاة (تتسق مع الأيقونات في الأسفل)
                    Spacer(modifier = Modifier.width(60.dp))
                }

                // رف قطع اللاعب الحالي الواضحة + أيقونات التحكم في الركن السفلي
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    // رف قطع المستخدم الحالية الواضحة
                    UserClearWoodRack2P(
                        tiles = userTiles,
                        isUserTurn = isUserTurn,
                        leftEnd = leftEnd,
                        rightEnd = rightEnd,
                        onTileClick = { onUserTileClicked(it) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // أيقونات أسفل يمين الشاشة (الدردشة، الخروج، الإعدادات)
                    LuxuryBottomRightIcons(
                        onChatClick = { showChatOverlay = true },
                        onExitClick = { showExitDialog = true },
                        onSettingsClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 4.dp)
                    )
                }
            }
        }

        // النوافذ الحوارية
        if (showExitDialog) {
            DominoExitConfirmDialog2P(
                onDismiss = { showExitDialog = false },
                onConfirmExit = {
                    showExitDialog = false
                    onBack()
                }
            )
        }

        if (showChatOverlay) {
            DominoQuickChatDialog2P(
                onDismiss = { showChatOverlay = false },
                onSelectPhrase = { phrase ->
                    statusMessage = "أنت: $phrase"
                    showChatOverlay = false
                }
            )
        }

        if (showSettingsDialog) {
            DominoSettingsDialog2P(
                onDismiss = { showSettingsDialog = false }
            )
        }

        if (selectedEndDialogTile != null) {
            val tile = selectedEndDialogTile!!
            DominoChooseEndDialog2P(
                tile = tile,
                leftVal = leftEnd,
                rightVal = rightEnd,
                onSelectLeft = {
                    selectedEndDialogTile = null
                    executePlayerPlay(tile, playOnLeft = true)
                },
                onSelectRight = {
                    selectedEndDialogTile = null
                    executePlayerPlay(tile, playOnLeft = false)
                },
                onDismiss = { selectedEndDialogTile = null }
            )
        }
    }
}

/**
 * رسم الطاولة الفاخرة (إطار خشب داكن، زخارف ذهبية، وسطح أخضر زمردي عميق)
 */
@Composable
fun LuxuryDominoTableCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // خلفية داكنة خارجية
        drawRect(color = Color(0xFF140804))

        val woodMargin = 6.dp.toPx()
        val woodCorner = 28.dp.toPx()

        // إطار الخشب الداكن المصقول
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF4A1F0E),
                    Color(0xFF2B1006),
                    Color(0xFF190903)
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.75f
            ),
            topLeft = Offset(woodMargin, woodMargin),
            size = Size(w - woodMargin * 2, h - woodMargin * 2),
            cornerRadius = CornerRadius(woodCorner, woodCorner)
        )

        // خط حزام ذهبي رقيق حول الإطار الخشبي
        drawRoundRect(
            color = Color(0xFFC89958).copy(alpha = 0.55f),
            topLeft = Offset(woodMargin + 1.5.dp.toPx(), woodMargin + 1.5.dp.toPx()),
            size = Size(w - (woodMargin + 1.5.dp.toPx()) * 2, h - (woodMargin + 1.5.dp.toPx()) * 2),
            cornerRadius = CornerRadius(woodCorner - 1.5.dp.toPx(), woodCorner - 1.5.dp.toPx()),
            style = Stroke(width = 1.4.dp.toPx())
        )

        // سطح اللعب الأخضر الزمردي الهادئ (جوخ فاخر)
        val feltInsetX = 18.dp.toPx()
        val feltInsetY = 16.dp.toPx()
        val feltCorner = 22.dp.toPx()

        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF145E3B), // أخضر زمردي مشرق في المنتصف
                    Color(0xFF0D4329), // أخضر كازينو هادئ
                    Color(0xFF072A19)  // أخضر عميق على الحواف
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.58f
            ),
            topLeft = Offset(feltInsetX, feltInsetY),
            size = Size(w - feltInsetX * 2, h - feltInsetY * 2),
            cornerRadius = CornerRadius(feltCorner, feltCorner)
        )

        // خط ذهبي منقط يفصل بين الإطار والسطح الأخضر
        val dotLineInsetX = feltInsetX + 5.dp.toPx()
        val dotLineInsetY = feltInsetY + 5.dp.toPx()
        val dotLineCorner = feltCorner - 4.dp.toPx()

        drawRoundRect(
            color = Color(0xFFE4BC7E),
            topLeft = Offset(dotLineInsetX, dotLineInsetY),
            size = Size(w - dotLineInsetX * 2, h - dotLineInsetY * 2),
            cornerRadius = CornerRadius(dotLineCorner, dotLineCorner),
            style = Stroke(
                width = 1.4.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
            )
        )

        // زخارف نباتية ذهبية في الزوايا الأربع
        val ornamentSize = 36.dp.toPx()
        drawFloralGoldenCorner2P(feltInsetX + 4.dp.toPx(), feltInsetY + 4.dp.toPx(), ornamentSize, flipX = false, flipY = false)
        drawFloralGoldenCorner2P(w - feltInsetX - 4.dp.toPx(), feltInsetY + 4.dp.toPx(), ornamentSize, flipX = true, flipY = false)
        drawFloralGoldenCorner2P(feltInsetX + 4.dp.toPx(), h - feltInsetY - 4.dp.toPx(), ornamentSize, flipX = false, flipY = true)
        drawFloralGoldenCorner2P(w - feltInsetX - 4.dp.toPx(), h - feltInsetY - 4.dp.toPx(), ornamentSize, flipX = true, flipY = true)
    }
}

/**
 * رسم الزخرفة النباتية الذهبية في زوايا الطاولة
 */
fun DrawScope.drawFloralGoldenCorner2P(
    cornerX: Float,
    cornerY: Float,
    size: Float,
    flipX: Boolean,
    flipY: Boolean
) {
    val goldColor = Color(0xFFE5BF80)
    val dirX = if (flipX) -1f else 1f
    val dirY = if (flipY) -1f else 1f

    val path = Path().apply {
        moveTo(cornerX, cornerY + dirY * size * 0.8f)
        cubicTo(
            cornerX + dirX * size * 0.3f, cornerY + dirY * size * 0.7f,
            cornerX + dirX * size * 0.7f, cornerY + dirY * size * 0.3f,
            cornerX + dirX * size * 0.8f, cornerY
        )
        cubicTo(
            cornerX + dirX * size * 0.5f, cornerY + dirY * size * 0.15f,
            cornerX + dirX * size * 0.15f, cornerY + dirY * size * 0.5f,
            cornerX, cornerY + dirY * size * 0.8f
        )
    }

    drawPath(
        path = path,
        color = goldColor.copy(alpha = 0.85f),
        style = Stroke(width = 1.5.dp.toPx())
    )

    drawCircle(
        color = goldColor,
        radius = 2.dp.toPx(),
        center = Offset(cornerX + dirX * size * 0.45f, cornerY + dirY * size * 0.45f)
    )
    drawCircle(
        color = goldColor,
        radius = 1.5.dp.toPx(),
        center = Offset(cornerX + dirX * size * 0.65f, cornerY + dirY * size * 0.25f)
    )
    drawCircle(
        color = goldColor,
        radius = 1.5.dp.toPx(),
        center = Offset(cornerX + dirX * size * 0.25f, cornerY + dirY * size * 0.65f)
    )
}

/**
 * بطاقة مقعد اللاعب (صورة اللاعب، اسمه، نقاطه، والوقت المتبقي)
 */
@Composable
fun PlayerSeatBadge2P(
    name: String,
    score: Int,
    timeStr: String,
    avatarEmoji: String,
    avatarBg: Color,
    isCurrentTurn: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "seatPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        // صورة اللاعب
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(avatarBg)
                .border(
                    width = if (isCurrentTurn) 2.2.dp else 1.2.dp,
                    color = if (isCurrentTurn) MujtamaGold.copy(alpha = pulseAlpha) else Color(0xFFC7985D),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = avatarEmoji, fontSize = 19.sp)
        }

        // بطاقة اسم اللاعب الذهبية الفاخرة
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFDFAB62),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4C2712)),
            shadowElevation = 3.dp
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFDE1A6), Color(0xFFCF9544))
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = name,
                    color = Color(0xFF311506),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }

        // بطاقة النقاط والوقت المتبقي
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF2E150B).copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFC7985D))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "النقاط: $score",
                    color = Color(0xFFF7E2C6),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "الوقت: $timeStr",
                    color = if (isCurrentTurn) MujtamaOnlineGreen else Color(0xFFF7E2C6),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * بطاقة مجموع النقاط الخشبية المذهبة
 */
@Composable
fun TotalPointsBadgeWood2P(
    title: String,
    score: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF3B1C0F),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFC7985D)),
        shadowElevation = 5.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF532816), Color(0xFF2E1308))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$title:",
                color = Color(0xFFF3D5AE),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$score",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/**
 * رف الخصم العلوي:
 * يتم إخفاء قطع الخصم إطلاقاً وعدم إظهار أي قيم لها، ويظهر الرف الخشبي مع شارة عدد القطع المتبقية فقط
 */
@Composable
fun OpponentConcealedWoodRack(count: Int, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF35180C),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6B361B)),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF4C2313), Color(0xFF281108))
                    )
                )
                .padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "🔒", fontSize = 11.sp)
            Text(
                text = "قطع الخصم مخفية ($count قطع متبقية)",
                color = Color(0xFFDCC8B2),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            // فتحات خشبية رمزية دون أي أرقام أو وجوه
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(count.coerceAtMost(7)) {
                    Box(
                        modifier = Modifier
                            .size(width = 12.dp, height = 16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF1E0C06))
                            .border(0.6.dp, Color(0xFF7D4326), RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

/**
 * رف قطع اللاعب الحالي الواضحة (أسفل الشاشة):
 * تظهر القطع كاملة بقيمها ونقاطها العاجية، ويتم تمييز الصالح منها للعب
 */
@Composable
fun UserClearWoodRack2P(
    tiles: List<DominoTile>,
    isUserTurn: Boolean,
    leftEnd: Int,
    rightEnd: Int,
    onTileClick: (DominoTile) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF381A0E),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF7A452B)),
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF552717), Color(0xFF260F07))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                tiles.forEach { tile ->
                    val isPlayable = isUserTurn && (
                            tile.left == leftEnd || tile.right == leftEnd ||
                                    tile.left == rightEnd || tile.right == rightEnd
                            )

                    StandingUserTile2P(
                        tile = tile,
                        isPlayable = isPlayable,
                        onClick = { onTileClick(tile) }
                    )
                }
            }
        }
    }
}

/**
 * حجر الدومينو الواضح للمستخدم في الرف
 */
@Composable
fun StandingUserTile2P(
    tile: DominoTile,
    isPlayable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isPlayable) MujtamaGold else Color(0xFF594331)
    val borderWidth = if (isPlayable) 2.dp else 0.8.dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(5.dp),
        color = Color(0xFFFAF7EE),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        shadowElevation = if (isPlayable) 6.dp else 2.dp,
        modifier = modifier
            .size(width = 38.dp, height = 74.dp)
            .testTag("user_standing_tile_${tile.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFFDF7), Color(0xFFECE3CA))
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DominoPipsCanvas2P(pips = tile.left)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0xFF2E2217)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD4AF37))
                    )
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DominoPipsCanvas2P(pips = tile.right)
                }
            }
        }
    }
}

/**
 * رسم نقاط الدومينو الكلاسيكية
 */
@Composable
fun DominoPipsCanvas2P(pips: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val radius = 2.2.dp.toPx()
        val dotColor = Color(0xFF1E1712)
        val w = size.width
        val h = size.height

        val pL = w * 0.26f
        val pR = w * 0.74f
        val pC = w * 0.5f
        val pT = h * 0.26f
        val pB = h * 0.74f
        val pM = h * 0.5f

        fun dot(x: Float, y: Float) {
            drawCircle(color = dotColor, radius = radius, center = Offset(x, y))
        }

        when (pips) {
            1 -> dot(pC, pM)
            2 -> {
                dot(pL, pT)
                dot(pR, pB)
            }
            3 -> {
                dot(pL, pT)
                dot(pC, pM)
                dot(pR, pB)
            }
            4 -> {
                dot(pL, pT)
                dot(pR, pT)
                dot(pL, pB)
                dot(pR, pB)
            }
            5 -> {
                dot(pL, pT)
                dot(pR, pT)
                dot(pC, pM)
                dot(pL, pB)
                dot(pR, pB)
            }
            6 -> {
                dot(pL, pT)
                dot(pR, pT)
                dot(pL, pM)
                dot(pR, pM)
                dot(pL, pB)
                dot(pR, pB)
            }
        }
    }
}

/**
 * حجر الدومينو الموضوع في منطقة اللعب المركزية بالطاولة
 */
@Composable
fun ClassicDominoTileView2P(
    tile: DominoTile,
    orientation: TileOrientation,
    isLeftEnd: Boolean,
    isRightEnd: Boolean,
    modifier: Modifier = Modifier
) {
    val isVertical = orientation == TileOrientation.VERTICAL
    val width = if (isVertical) 32.dp else 56.dp
    val height = if (isVertical) 56.dp else 32.dp

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFFFAF7EE),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isLeftEnd || isRightEnd) 1.5.dp else 0.8.dp,
            color = if (isLeftEnd || isRightEnd) MujtamaGold else Color(0xFF453628)
        ),
        shadowElevation = 4.dp,
        modifier = modifier.size(width = width, height = height)
    ) {
        if (isVertical) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    DominoPipsCanvas2P(pips = tile.left)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(1.5.dp).background(Color(0xFF2E2217)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0xFFD4AF37)))
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    DominoPipsCanvas2P(pips = tile.right)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    DominoPipsCanvas2P(pips = tile.left)
                }
                Box(
                    modifier = Modifier.fillMaxHeight().width(1.5.dp).background(Color(0xFF2E2217)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0xFFD4AF37)))
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    DominoPipsCanvas2P(pips = tile.right)
                }
            }
        }
    }
}

/**
 * شارة الطرف المفتوح الفاخرة
 */
@Composable
fun OpenEndBadgeLuxury(endValue: Int, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF200F07).copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7985D)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = label, color = Color(0xFFE2CBB7), fontSize = 10.sp, fontWeight = FontWeight.Normal)
            Surface(
                shape = CircleShape,
                color = Color(0xFFD4AF37),
                modifier = Modifier.size(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$endValue",
                        color = Color(0xFF2A1508),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}

/**
 * أيقونات أسفل يمين الشاشة (الدردشة، الخروج، الإعدادات)
 */
@Composable
fun LuxuryBottomRightIcons(
    onChatClick: () -> Unit,
    onExitClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1B3D2F),
        border = androidx.compose.foundation.BorderStroke(1.4.dp, Color(0xFFC7985D)),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onChatClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A160C))
                    .border(1.2.dp, Color(0xFFC7985D), CircleShape)
                    .testTag("domino_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = "الدردشة",
                    tint = Color(0xFFF7E2C6),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onExitClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A160C))
                    .border(1.2.dp, Color(0xFFC7985D), CircleShape)
                    .testTag("domino_exit_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "الخروج",
                    tint = Color(0xFFF7E2C6),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A160C))
                    .border(1.2.dp, Color(0xFFC7985D), CircleShape)
                    .testTag("domino_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "الإعدادات",
                    tint = Color(0xFFF7E2C6),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun DominoExitConfirmDialog2P(
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الخروج من الطاولة", fontWeight = FontWeight.Bold) },
        text = { Text("هل ترغب في الاستسلام ومغادرة جولة الدومينو الحالية؟") },
        confirmButton = {
            Button(
                onClick = onConfirmExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.85f))
            ) {
                Text("نعم، خروج")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("متابعة اللعب")
            }
        }
    )
}

@Composable
fun DominoQuickChatDialog2P(
    onDismiss: () -> Unit,
    onSelectPhrase: (String) -> Unit
) {
    val phrases = listOf("لعبة موفقة! 🀄", "حظاً أوفر في الجولة القادمة! 🔥", "لعبة قوية! 👏", "أغلقت الطاولة! 🎯", "باقي لي قطعة واحدة! 😉")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الدردشة السريعة", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                phrases.forEach { phrase ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPhrase(phrase) }
                    ) {
                        Text(
                            text = phrase,
                            modifier = Modifier.padding(10.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
fun DominoSettingsDialog2P(
    onDismiss: () -> Unit
) {
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إعدادات الطاولة", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المؤثرات الصوتية")
                    Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الاهتزاز عند لعب الحجر")
                    Switch(checked = vibrationEnabled, onCheckedChange = { vibrationEnabled = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("تم") }
        }
    )
}

@Composable
fun DominoChooseEndDialog2P(
    tile: DominoTile,
    leftVal: Int,
    rightVal: Int,
    onSelectLeft: () -> Unit,
    onSelectRight: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر طرف اللعب", fontWeight = FontWeight.Bold) },
        text = {
            Text("الحجر [${tile.left}|${tile.right}] يطابق كلا الطرفين المفتوحين! على أي طرف ترغب في وضعه؟")
        },
        confirmButton = {
            Button(
                onClick = onSelectLeft,
                colors = ButtonDefaults.buttonColors(containerColor = MujtamaPrimary)
            ) {
                Text("الطرف الأيسر ($leftVal)")
            }
        },
        dismissButton = {
            Button(
                onClick = onSelectRight,
                colors = ButtonDefaults.buttonColors(containerColor = MujtamaGold)
            ) {
                Text("الطرف الأيمن ($rightVal)")
            }
        }
    )
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.GameMatchMode
import com.example.model.GameType
import com.example.ui.screens.games.*
import com.example.ui.theme.MujtamaGold
import com.example.ui.theme.MujtamaOnlineGreen
import com.example.ui.theme.MujtamaPrimary
import com.example.ui.theme.MujtamaTeal
import kotlinx.coroutines.delay

/**
 * بنية بيانات قابلة للتوسع لإضافة أي لعبة مستقبلاً
 * (الدومينو، لودو، جاكارو، لعبة السلم، الشطرنج) بنفس النمط دون إعادة هيكلة.
 */
data class GameCatalogItem(
    val gameType: GameType,
    val title: String,
    val subtitle: String,
    val categoryLabel: String,
    val iconEmoji: String,
    val coverDrawableRes: Int,
    val waitingDrawableRes: Int,
    val playersLabel: String,
    val requiredPlayers: Int,
    val isAvailable: Boolean = false
)

/**
 * قائمة كتالوج الألعاب:
 * - تظهر لعبة "الدومينو" حالياً فقط لأنها الوحيدة النشطة (isAvailable = true).
 * - باقي الألعاب مجهزة في الكتالوج ويمكن تفعيلها في أي وقت بتغيير القيمة فقط دون أي تعديل في التصميم أو الهيكلية.
 */
val ALL_CATALOG_GAMES = listOf(
    GameCatalogItem(
        gameType = GameType.DOMINO,
        title = "الدومينو الكلاسيكية",
        subtitle = "طابق النقاط وأغلق الطاولة",
        categoryLabel = "كلاسيكية",
        iconEmoji = "🀄",
        coverDrawableRes = R.drawable.img_domino_cover,
        waitingDrawableRes = R.drawable.img_domino_waiting,
        playersLabel = "لاعبان",
        requiredPlayers = 2,
        isAvailable = true // مفعلة حالياً ومطابقة للتصميم المطلوب
    ),
    GameCatalogItem(
        gameType = GameType.LUDO,
        title = "لودو",
        subtitle = "سباق الأحجار والنرد الشهير",
        categoryLabel = "حماسية",
        iconEmoji = "🎲",
        coverDrawableRes = R.drawable.img_domino_cover,
        waitingDrawableRes = R.drawable.img_domino_waiting,
        playersLabel = "4 لاعبين",
        requiredPlayers = 4,
        isAvailable = false // قابلة للإضافة لاحقاً
    ),
    GameCatalogItem(
        gameType = GameType.JACKAROO,
        title = "جاكارو",
        subtitle = "تحدي الأوراق والأحجار الجماعي",
        categoryLabel = "استراتيجية",
        iconEmoji = "🃏",
        coverDrawableRes = R.drawable.img_domino_cover,
        waitingDrawableRes = R.drawable.img_domino_waiting,
        playersLabel = "4 لاعبين",
        requiredPlayers = 4,
        isAvailable = false // قابلة للإضافة لاحقاً
    ),
    GameCatalogItem(
        gameType = GameType.SNAKES_AND_LADDERS,
        title = "لعبة السلم",
        subtitle = "تسلق السلالم وتفادَ الثعابين",
        categoryLabel = "عائلية",
        iconEmoji = "🪜",
        coverDrawableRes = R.drawable.img_domino_cover,
        waitingDrawableRes = R.drawable.img_domino_waiting,
        playersLabel = "لاعبان",
        requiredPlayers = 2,
        isAvailable = false // قابلة للإضافة لاحقاً
    ),
    GameCatalogItem(
        gameType = GameType.CHESS,
        title = "الشطرنج",
        subtitle = "صراع العقول والملوك الخالد",
        categoryLabel = "ذكاء",
        iconEmoji = "♟️",
        coverDrawableRes = R.drawable.img_domino_cover,
        waitingDrawableRes = R.drawable.img_domino_waiting,
        playersLabel = "لاعبان",
        requiredPlayers = 2,
        isAvailable = false // قابلة للإضافة لاحقاً
    )
)

/**
 * شاشة الألعاب الرئيسية:
 * 1. إذا كانت هناك لعبة نشطة، تعرض لوحة اللعب التفاعلية الكاملة.
 * 2. إذا اختار المستخدم لعبة، تنتقل لشاشة "الانتظار والمطابقة" الخاصة بها.
 * 3. خلاف ذلك، تعرض شبكة الألعاب (وحالياً لعبة الدومينو فقط كمربع بنمط مربعات غرف الدردشة).
 */
@Composable
fun GamesScreen(
    activeGame: GameType?,
    activeMode: GameMatchMode,
    activeOpponent: String,
    onLaunchGame: (GameType, GameMatchMode, String) -> Unit,
    onExitGame: () -> Unit,
    onWinReward: (Int, String) -> Unit
) {
    // 1. إذا كانت هناك لعبة جارية حالياً، عرض لوحة اللعبة التفاعلية
    if (activeGame != null) {
        when (activeGame) {
            GameType.DOMINO -> DominoGameView(
                mode = activeMode,
                opponentName = activeOpponent,
                onBack = onExitGame,
                onWinReward = { onWinReward(it, activeGame.titleAr) }
            )
            GameType.LUDO -> LudoGameView(
                mode = activeMode,
                opponentName = activeOpponent,
                onBack = onExitGame,
                onWinReward = { onWinReward(it, activeGame.titleAr) }
            )
            GameType.JACKAROO -> JackarooGameView(
                mode = activeMode,
                opponentName = activeOpponent,
                onBack = onExitGame,
                onWinReward = { onWinReward(it, activeGame.titleAr) }
            )
            GameType.SNAKES_AND_LADDERS -> SnakesLaddersGameView(
                mode = activeMode,
                opponentName = activeOpponent,
                onBack = onExitGame,
                onWinReward = { onWinReward(it, activeGame.titleAr) }
            )
            GameType.CHESS -> ChessGameView(
                mode = activeMode,
                opponentName = activeOpponent,
                onBack = onExitGame,
                onWinReward = { onWinReward(it, activeGame.titleAr) }
            )
        }
        return
    }

    // 2. حالة شاشة الانتظار للعبة المختارة
    var selectedWaitingGame by remember { mutableStateOf<GameCatalogItem?>(null) }

    if (selectedWaitingGame != null) {
        GameWaitingLobbyScreen(
            gameItem = selectedWaitingGame!!,
            onCancel = { selectedWaitingGame = null },
            onMatchSuccess = { mode, opponentName ->
                val target = selectedWaitingGame!!.gameType
                selectedWaitingGame = null
                onLaunchGame(target, mode, opponentName)
            }
        )
        return
    }

    // 3. شاشة استعراض الألعاب بنمط مربعات غرف الدردشة
    val availableGames = remember { ALL_CATALOG_GAMES.filter { it.isAvailable } }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .testTag("games_grid_screen"),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 84.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // شريط الرأس الترحيبي
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الألعاب التنافسية 🎮",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                    Text(
                        text = "اختر لعبتك وتحدَّ خصمك أو نافس أصدقاءك مباشرة",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "${availableGames.size} لعبة متاحة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // عرض بطاقات الألعاب المتاحة حالياً (الدومينو فقط) بنمط مربعات غرف الدردشة
        items(availableGames, key = { it.gameType.id }) { gameItem ->
            GameGridCard(
                game = gameItem,
                onCardClick = {
                    selectedWaitingGame = gameItem
                }
            )
        }
    }
}

/**
 * تصميم مربع اللعبة (Grid Card):
 * مطابق تماماً لنمط مربعات "غرف الدردشة" (RoomGridCard):
 * - صورة اللعبة المرفقة تملأ خلفية المربع
 * - حواف دائرية ناعمة (Rounded Corner 18.dp)
 * - تدرج حماية داكن يبرز اسم اللعبة بوضوح تام
 * - شارات الفئة وعدد اللاعبين في الأعلى
 * - زر زجاجي شفاف "دخول" بالأسفل للانتقال المباشر للانتظار
 */
@Composable
fun GameGridCard(
    game: GameCatalogItem,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onCardClick)
            .testTag("game_item_${game.gameType.id}"),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. خلفية المربع: صورة اللعبة المرفقة
            Image(
                painter = painterResource(id = game.coverDrawableRes),
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 2. طبقة تدرج لوني شبه شفاف (Scrim Gradient) لضمان وضوح النصوص والأزرار بالكامل
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // 3. المحتوى الداخلي للمربع
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // الجزء العلوي: فئة اللعبة + شارة عدد اللاعبين البارزة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // فئة اللعبة
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(game.iconEmoji, fontSize = 11.sp)
                            Text(
                                text = game.categoryLabel,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // شارة عدد اللاعبين (أيقونة أشخاص + رقم واضح)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MujtamaOnlineGreen.copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MujtamaOnlineGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = game.playersLabel,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // الجزء السفلي: اسم اللعبة واضح + وصف موجز + زر زجاجي شفاف
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = game.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )

                    Text(
                        text = game.subtitle,
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        maxLines = 1
                    )

                    // زر زجاجي شفاف بعبارة "دخول" داخل المربع مطابق لغرف الدردشة
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.22f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "دخول",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * شاشة الانتظار والمطابقة الخاصة باللعبة:
 * - تستخدم صورة الانتظار المرفقة كخلفية فاخرة
 * - تتيح اختيار وضع اللعب (فردي / جماعي / دعوة صديق) بنمط الأزرار الخشبية
 * - تحتوي على حالة البحث عن خصم/اللاعبين مع مؤقت ورادار بحث متحرك
 * - زر إلغاء واضح لإلغاء البحث في أي وقت
 * - تنتقل تلقائياً للعب فور اكتمال العدد المطلوب
 */
@Composable
fun GameWaitingLobbyScreen(
    gameItem: GameCatalogItem,
    onCancel: () -> Unit,
    onMatchSuccess: (GameMatchMode, String) -> Unit
) {
    var isSearching by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(GameMatchMode.RANDOM_OPPONENT) }
    var searchSeconds by remember { mutableIntStateOf(0) }
    var matchedPlayersCount by remember { mutableIntStateOf(1) }
    var matchFound by remember { mutableStateOf(false) }
    var matchedOpponentName by remember { mutableStateOf("") }

    // قائمة الخصوم الافتراضيين
    val possibleOpponents = remember {
        listOf("أحمد الشمري ⭐ 1420", "سالم القحطاني 🔥 1580", "نورة العتيبي 🌟 1390", "طارق الزهراني 🎯 1610")
    }

    // مؤقت البحث والمطابقة التلقائية
    LaunchedEffect(isSearching) {
        if (isSearching) {
            searchSeconds = 0
            matchedPlayersCount = 1
            matchFound = false

            while (isSearching && !matchFound) {
                delay(1000)
                searchSeconds++

                // بعد ثانيتين يتم العثور على الخصم تلقائياً واكتمال العدد المطلوب
                if (searchSeconds >= 2) {
                    matchedPlayersCount = gameItem.requiredPlayers
                    matchFound = true
                    matchedOpponentName = possibleOpponents.random()
                    // تأخير نصف ثانية لإشعار المستخدم بنجاح المطابقة ثم الانتقال التلقائي للعب
                    delay(800)
                    onMatchSuccess(searchMode, matchedOpponentName)
                    break
                }
            }
        }
    }

    // حركة الرادار والنبض أثناء البحث
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("game_waiting_screen")
    ) {
        // 1. خلفية شاشة الانتظار: صورة الانتظار المرفقة
        Image(
            painter = painterResource(id = gameItem.waitingDrawableRes),
            contentDescription = "شاشة انتظار ${gameItem.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // طبقة تظليل ناعمة لراحة القراءة
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
        )

        // 2. شريط علوي: زر العودة وشارة اللعبة
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("exit_waiting_room_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "خروج",
                    tint = Color.White
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.55f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MujtamaGold.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(gameItem.iconEmoji, fontSize = 14.sp)
                    Text(
                        text = gameItem.title,
                        color = MujtamaGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.size(40.dp))
        }

        // 3. المحتوى الأوسط: إما أزرار اختيار النمط أو حالة البحث النشطة
        if (isSearching) {
            // حالة البحث عن خصم / اللاعبين النشطة
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // شارة الرادار الدائري النابض
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MujtamaGold.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .border(2.dp, if (matchFound) MujtamaOnlineGreen else MujtamaGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (matchFound) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MujtamaOnlineGreen,
                            modifier = Modifier.size(52.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            color = MujtamaGold,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(76.dp)
                        )
                        Text("🀄", fontSize = 28.sp)
                    }
                }

                // نصوص حالة البحث
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (matchFound) "تم العثور على الخصم! 🎯" else "جاري البحث عن خصم مناسب...",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (matchFound)
                            "بدء الجولة مع $matchedOpponentName..."
                        else
                            "نبحث عن لاعبين متاحين الآن • الوقت: 00:${if (searchSeconds < 10) "0$searchSeconds" else "$searchSeconds"}",
                        color = if (matchFound) MujtamaGold else Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // عرض مقاعد اللاعبين (العدد المطلوب)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // مقعد اللاعب 1 (أنت)
                        PlayerSlotItem(
                            label = "أنت",
                            subLabel = "جاهز ✓",
                            isFilled = true,
                            isCurrentUser = true
                        )

                        Text("VS", fontWeight = FontWeight.Black, color = MujtamaGold, fontSize = 16.sp)

                        // مقعد الخصم
                        PlayerSlotItem(
                            label = if (matchFound) matchedOpponentName.split(" ").first() else "الخصم",
                            subLabel = if (matchFound) "انضم ✓" else "جاري البحث...",
                            isFilled = matchFound,
                            isCurrentUser = false
                        )
                    }
                }

                // زر إلغاء البحث
                Button(
                    onClick = { isSearching = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.8f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(46.dp)
                        .testTag("cancel_matchmaking_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "إلغاء البحث", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        } else {
            // شاشة اللوبي واختيار النمط (فردي، جماعي، دعوة صديق) بنمط الأزرار الخشبية المتطابقة مع الصورة
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // عنوان وشعار اللعبة
                Text(
                    text = "اختر وضع اللعب",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // الصف الأول: زر فردي + زر جماعي
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. زر "فردي" (1 ضد 1)
                    WoodGameButton(
                        text = "فردي",
                        icon = Icons.Default.Person,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            searchMode = GameMatchMode.RANDOM_OPPONENT
                            isSearching = true
                        },
                        testTag = "domino_single_button"
                    )

                    // 2. زر "جماعي" (4 لاعبين)
                    WoodGameButton(
                        text = "جماعي",
                        icon = Icons.Default.Groups,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            searchMode = GameMatchMode.RANDOM_OPPONENT
                            isSearching = true
                        },
                        testTag = "domino_multiplayer_button"
                    )
                }

                // الصف الثاني: زر دعوة صديق + زر مسابقة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 3. زر "دعوة صديق"
                    WoodGameButtonSmall(
                        text = "دعوة صديق",
                        icon = Icons.Default.PersonAdd,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            searchMode = GameMatchMode.WITH_FRIEND
                            isSearching = true
                        },
                        testTag = "domino_invite_button"
                    )

                    // 4. زر "مسابقة"
                    WoodGameButtonSmall(
                        text = "مسابقة",
                        icon = Icons.Default.EmojiEvents,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            searchMode = GameMatchMode.RANDOM_OPPONENT
                            isSearching = true
                        },
                        testTag = "domino_tournament_button"
                    )
                }
            }
        }
    }
}

/**
 * زر اللعب ذو النمط الخشبي المطابق للصورة المرفقة
 */
@Composable
fun WoodGameButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF6F3B22), // لون الخشب الداكن الغني
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2B785)), // إطار ذهبي خشبي
        shadowElevation = 6.dp,
        modifier = modifier
            .height(64.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF8D5335),
                            Color(0xFF5A2A14)
                        )
                    )
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // أيقونة في إطار داخلي
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.28f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFFFDE8D0),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = text,
                    color = Color(0xFFFDE8D0),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * زر خشبي مصغر للخيارات الإضافية (دعوة صديق، مسابقة)
 */
@Composable
fun WoodGameButtonSmall(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF5D301C),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFCCA275)),
        shadowElevation = 4.dp,
        modifier = modifier
            .height(50.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF744026),
                            Color(0xFF4C200E)
                        )
                    )
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFF6DAC0),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    color = Color(0xFFF6DAC0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * عنصر مقعد اللاعب داخل شاشة الانتظار
 */
@Composable
fun PlayerSlotItem(
    label: String,
    subLabel: String,
    isFilled: Boolean,
    isCurrentUser: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    if (isFilled)
                        if (isCurrentUser) MujtamaPrimary else MujtamaGold
                    else
                        Color.White.copy(alpha = 0.1f)
                )
                .border(
                    width = 1.5.dp,
                    color = if (isFilled) Color.White else Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isFilled) {
                Text(
                    text = if (isCurrentUser) "👤" else "🎯",
                    fontSize = 22.sp
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MujtamaGold,
                    strokeWidth = 2.dp
                )
            }
        }

        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1
        )

        Text(
            text = subLabel,
            color = if (isFilled) MujtamaOnlineGreen else MujtamaGold,
            fontSize = 10.sp
        )
    }
}

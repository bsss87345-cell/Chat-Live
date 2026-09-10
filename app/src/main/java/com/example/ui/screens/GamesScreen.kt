package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.GameMatchMode
import com.example.model.GameType
import com.example.ui.screens.games.*
import com.example.ui.theme.MujtamaGold
import com.example.ui.theme.MujtamaOnlineGreen
import com.example.ui.theme.MujtamaPrimary
import com.example.ui.theme.MujtamaTeal

@Composable
fun GamesScreen(
    activeGame: GameType?,
    activeMode: GameMatchMode,
    activeOpponent: String,
    onLaunchGame: (GameType, GameMatchMode, String) -> Unit,
    onExitGame: () -> Unit,
    onWinReward: (Int, String) -> Unit
) {
    // If an active game is selected, show its full interactive playable board
    if (activeGame != null) {
        when (activeGame) {
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
            GameType.DOMINO -> DominoGameView(
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

    // Modal state for choosing "العب مع صديق" vs "ابحث عن خصم"
    var selectedGameForLaunch by remember { mutableStateOf<GameType?>(null) }

    // Dialog for Game Launch Mode Options
    if (selectedGameForLaunch != null) {
        val game = selectedGameForLaunch!!
        Dialog(onDismissRequest = { selectedGameForLaunch = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("game_mode_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Game Icon & Title
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(game.tagColorHex),
                                        Color(game.tagColorHex).copy(alpha = 0.6f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(game.iconEmoji, fontSize = 32.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = game.titleAr,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = game.subtitleAr,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 2.dp))

                    Text(
                        text = "اختر طريقة اللعب:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Option 1: العب مع صديق
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val target = game
                                selectedGameForLaunch = null
                                onLaunchGame(target, GameMatchMode.WITH_FRIEND, "صديقك المقرب")
                            }
                            .testTag("play_with_friend_option"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MujtamaTeal.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👥", fontSize = 20.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("العب مع صديق", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("تحدَّ أصدقاءك في محادثة خاصة أو أنشئ رابطاً", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Option 2: ابحث عن خصم
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val target = game
                                selectedGameForLaunch = null
                                onLaunchGame(target, GameMatchMode.RANDOM_OPPONENT, "خصم عشوائي (بوت)")
                            }
                            .testTag("match_opponent_option"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MujtamaGold.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚡", fontSize = 20.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ابحث عن خصم", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("دخول فوري مع لاعب متاح أو خوض جولة تدريبية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Cancel
                    TextButton(
                        onClick = { selectedGameForLaunch = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // Main Games Screen: Displays the 5 Games directly
    val theFiveGames = remember {
        listOf(
            GameType.LUDO,
            GameType.JACKAROO,
            GameType.DOMINO,
            GameType.SNAKES_AND_LADDERS,
            GameType.CHESS
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("games_screen_list"),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 80.dp, top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Simple Safety Notice Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MujtamaTeal.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "أمان الألعاب",
                        tint = MujtamaTeal,
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = "ألعاب تنافسية مجانية وآمنة 100%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MujtamaTeal
                        )
                        Text(
                            text = "خض جولات حماسية في لودو، جاكارو، دومينو، السلم، والشطرنج بدون أي رهان مالي.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الألعاب المتوفرة 🎮",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                    Text(
                        text = "اختر لعبتك المفضلة وابدأ المنافسة فوراً",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "5 ألعاب",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // The Five Game Cards
        items(theFiveGames, key = { it.id }) { game ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { selectedGameForLaunch = game }
                    .testTag("game_card_${game.id}"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Game Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(game.tagColorHex),
                                            Color(game.tagColorHex).copy(alpha = 0.7f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = game.iconEmoji,
                                fontSize = 30.sp
                            )
                        }

                        // Game Information
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = game.titleAr,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(game.tagColorHex).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = game.playersCount,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(game.tagColorHex),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = game.subtitleAr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Game Description
                    Text(
                        text = game.description,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Action Buttons: "العب مع صديق" & "ابحث عن خصم"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onLaunchGame(game, GameMatchMode.WITH_FRIEND, "صديقك المقرب") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("👥 العب مع صديق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onLaunchGame(game, GameMatchMode.RANDOM_OPPONENT, "خصم عشوائي (بوت)") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(game.tagColorHex)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("⚡ ابحث عن خصم", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

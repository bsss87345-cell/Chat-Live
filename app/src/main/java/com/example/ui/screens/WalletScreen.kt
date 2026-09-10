package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StoreItem
import com.example.model.TransactionType
import com.example.model.WalletTransaction
import com.example.ui.theme.MujtamaCoral
import com.example.ui.theme.MujtamaGold
import com.example.ui.theme.MujtamaOnlineGreen
import com.example.ui.theme.MujtamaPrimary
import com.example.ui.theme.MujtamaTeal

@Composable
fun WalletScreen(
    balance: Int,
    dailyBonusClaimed: Boolean,
    walletFilter: String,
    transactions: List<WalletTransaction>,
    storeItems: List<StoreItem>,
    onClaimDailyBonus: () -> Unit,
    onFilterChange: (String) -> Unit,
    onBuyItem: (StoreItem) -> Unit
) {
    val filteredTransactions = transactions.filter { tx ->
        when (walletFilter) {
            "كسب (+)" -> tx.type == TransactionType.EARN
            "إنفاق (-)" -> tx.type == TransactionType.SPEND
            else -> true
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 80.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Digital Points Balance Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_balance_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF2E1C5E),
                                    Color(0xFF191033)
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "رصيد المكافآت والنقاط الافتراضية",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = "نقاط",
                                tint = MujtamaGold,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "$balance",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "نقطة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MujtamaGold
                            )
                        }

                        // Daily Bonus Claim Button
                        Button(
                            onClick = onClaimDailyBonus,
                            enabled = !dailyBonusClaimed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MujtamaGold,
                                disabledContainerColor = Color.White.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("claim_daily_bonus_button")
                        ) {
                            Icon(
                                imageVector = if (dailyBonusClaimed) Icons.Default.CheckCircle else Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = if (dailyBonusClaimed) Color.White.copy(alpha = 0.7f) else Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (dailyBonusClaimed) "تم استلام مكافأة اليوم (+150)" else "استلام هدية الدخول اليومية (+150 نقطة)",
                                fontWeight = FontWeight.Bold,
                                color = if (dailyBonusClaimed) Color.White.copy(alpha = 0.7f) else Color.Black
                            )
                        }
                    }
                }
            }
        }

        // Strict Policy & Disclaimer Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "سياسة النقاط",
                        tint = MujtamaTeal,
                        modifier = Modifier.size(26.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "ضمان الشفافية والأمان المالي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• الرصيد يُشحن حصرياً من قبل إدارة التطبيق كمكافآت للمسابقات والنشاط.\n• لا يوجد أي سحب نقدي أو تحويل خارج التطبيق.\n• لا يمكن مراهنة النقاط بين المستخدمين بأي شكل من الأشكال.",
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Internal Store (Spend points on virtual items)
        item {
            Text(
                text = "المتجر الداخلي للمكافآت والأوسمة 🎁",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(storeItems, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_item_${item.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isOwned) MujtamaTeal.copy(alpha = 0.2f)
                                else MujtamaGold.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.category) {
                                "إطارات" -> Icons.Default.FilterFrames
                                "أوسمة" -> Icons.Default.MilitaryTech
                                "ثيمات" -> Icons.Default.Palette
                                else -> Icons.Default.Bolt
                            },
                            contentDescription = item.title,
                            tint = if (item.isOwned) MujtamaTeal else MujtamaGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = item.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${item.cost} نقطة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MujtamaGold
                        )
                    }

                    if (item.isOwned) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MujtamaTeal.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "مملوك لديك ✓",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MujtamaTeal
                            )
                        }
                    } else {
                        Button(
                            onClick = { onBuyItem(item) },
                            enabled = balance >= item.cost,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("استبدال", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Transaction History Header & Filter
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل العمليات والنقاط 📜",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("الكل", "كسب (+)", "إنفاق (-)").forEach { filter ->
                            FilterChip(
                                selected = walletFilter == filter,
                                onClick = { onFilterChange(filter) },
                                label = { Text(filter, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Transactions List
        items(filteredTransactions, key = { it.id }) { tx ->
            val isEarn = tx.type == TransactionType.EARN
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_${tx.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEarn) MujtamaOnlineGreen.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEarn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = if (isEarn) "كسب" else "إنفاق",
                            tint = if (isEarn) MujtamaOnlineGreen else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tx.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${tx.note} • ${tx.date}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = if (isEarn) "+${tx.points}" else "-${tx.points}",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = if (isEarn) MujtamaOnlineGreen else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

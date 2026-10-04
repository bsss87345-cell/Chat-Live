package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MujtamaGold
import com.example.ui.theme.MujtamaPrimary

/**
 * باقة شحن: مبلغ بالدينار العراقي مقابل عدد نقاط.
 * نسبة التحويل الثابتة: كل 1,000 دينار = 100,000 نقطة.
 */
data class RechargePackage(
    val iqd: Int,
    val points: Int
)

/** نسبة التحويل: 100 نقطة لكل دينار واحد. */
const val POINTS_PER_IQD = 100

val rechargePackages: List<RechargePackage> = listOf(
    1_000, 5_000, 10_000, 25_000, 50_000, 100_000
).map { amount -> RechargePackage(iqd = amount, points = amount * POINTS_PER_IQD) }

/** تنسيق الأرقام بفواصل آلاف إنجليزية: 100000 -> 100,000 */
fun formatThousands(value: Int): String =
    String.format(java.util.Locale.US, "%,d", value)

/**
 * نافذة اختيار باقة الشحن.
 * الضغط على أي باقة يقفل النافذة ويفتح صفحة الدعم ومعها رقم المحفظة.
 */
@Composable
fun RechargeDialog(
    onDismiss: () -> Unit,
    onSelectPackage: (RechargePackage) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MujtamaGold,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "شحن الرصيد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "كل 1,000 دينار عراقي = 100,000 نقطة",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MujtamaGold
                )

                rechargePackages.chunked(2).forEach { rowPackages ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowPackages.forEach { pack ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectPackage(pack) }
                                    .testTag("recharge_package_${pack.iqd}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MujtamaPrimary.copy(alpha = 0.10f)
                                ),
                                border = BorderStroke(1.dp, MujtamaGold.copy(alpha = 0.45f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = formatThousands(pack.points),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = MujtamaGold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "نقطة",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${formatThousands(pack.iqd)} د.ع",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        // لو كان الصف الأخير فيه باقة واحدة، نملأ الفراغ حتى لا تتمدد
                        if (rowPackages.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Text(
                    text = "اضغط على الباقة للمتابعة مع الدعم والحصول على رقم المحفظة",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

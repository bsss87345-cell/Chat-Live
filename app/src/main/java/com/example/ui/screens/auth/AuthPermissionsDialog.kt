package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.window.Dialog
import com.example.model.AuthProvider
import com.example.model.AuthUserAccount
import com.example.ui.theme.MujtamaGold
import com.example.ui.theme.MujtamaOnlineGreen
import com.example.ui.theme.MujtamaPrimary
import com.example.ui.theme.MujtamaTeal

/**
 * نافذة استئذان صلاحيات الوصول (الاسم، البريد الإلكتروني، الصورة الشخصية)
 * لحساب جوجل أو فيسبوك، مع معالجة الرفض والموافقة.
 */
@Composable
fun AuthPermissionsConsentDialog(
    provider: AuthProvider,
    isSignUp: Boolean,
    onConfirmPermissions: (AuthUserAccount) -> Unit,
    onDenyPermissions: () -> Unit,
    onDismiss: () -> Unit
) {
    // حسابات النظام المقترحة أو إدخال مخصص لسهولة الاستخدام
    val candidateAccounts = remember(provider) {
        if (provider == AuthProvider.GOOGLE) {
            listOf(
                AuthUserAccount(
                    name = "مستخدم Google",
                    email = "user@gmail.com",
                    avatarEmoji = "👤",
                    provider = AuthProvider.GOOGLE
                )
            )
        } else {
            listOf(
                AuthUserAccount(
                    name = "مستخدم Facebook",
                    email = "user@facebook.com",
                    avatarEmoji = "👤",
                    provider = AuthProvider.FACEBOOK
                )
            )
        }
    }

    var selectedAccount by remember { mutableStateOf(candidateAccounts.first()) }
    var customName by remember { mutableStateOf("") }
    var customEmail by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .testTag("auth_permissions_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // رأس النافذة مع هوية المزود
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (provider == AuthProvider.GOOGLE) {
                        GoogleLogoIcon(modifier = Modifier.size(28.dp))
                    } else {
                        FacebookLogoIcon(modifier = Modifier.size(28.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSignUp) "إنشاء حساب عبر ${provider.providerNameAr}" else "تسجيل الدخول عبر ${provider.providerNameAr}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "صلاحيات الوصول المطلوبة",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MujtamaTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // نص توضيحي رسمي
                Text(
                    text = "يطلب تطبيق «مجتمعنا» الإذن للوصول إلى معلومات حسابك التالية لإنشاء ملفك الشخصي وتخصيص معرّف رقمي فريد لك:",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                // بطاقات الصلاحيات المطلوبة الثلاث: (الاسم، البريد، الصورة الشخصية)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PermissionItemRow(
                        icon = Icons.Default.Person,
                        title = "الاسم الكامل",
                        description = "لعرض اسمك في المحادثات، التحديات، والملف الشخصي"
                    )
                    PermissionItemRow(
                        icon = Icons.Default.Email,
                        title = "عنوان البريد الإلكتروني",
                        description = "لربط حسابك بأمان وتأكيد الحماية"
                    )
                    PermissionItemRow(
                        icon = Icons.Default.Image,
                        title = "الصورة الشخصية والرمز",
                        description = "كصورة رمزية افتراضية في حسابك"
                    )
                }

                // اختيار الحساب
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "الحساب المُختار للمتابعة:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        candidateAccounts.forEach { acc ->
                            val isSelected = (!showCustomInput && selectedAccount == acc)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MujtamaPrimary.copy(alpha = 0.12f) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.2.dp else 0.dp,
                                        color = if (isSelected) MujtamaPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        showCustomInput = false
                                        selectedAccount = acc
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MujtamaPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = acc.avatarEmoji, fontSize = 16.sp)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = acc.name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    Text(text = acc.email, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MujtamaPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // زرا الموافقة والرفض
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // زر الرفض / الإلغاء
                    OutlinedButton(
                        onClick = { onDenyPermissions() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("auth_deny_permissions_button")
                    ) {
                        Text(
                            text = "رفض الصلاحيات",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // زر الموافقة والمتابعة
                    Button(
                        onClick = {
                            val finalAccount = if (showCustomInput && customName.isNotBlank() && customEmail.isNotBlank()) {
                                AuthUserAccount(
                                    name = customName.trim(),
                                    email = customEmail.trim(),
                                    avatarEmoji = "👤",
                                    provider = provider
                                )
                            } else {
                                selectedAccount
                            }
                            onConfirmPermissions(finalAccount)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (provider == AuthProvider.FACEBOOK) Color(0xFF1877F2) else MujtamaPrimary
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("auth_confirm_permissions_button")
                    ) {
                        Text(
                            text = "موافقة ومتابعة",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MujtamaTeal.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MujtamaTeal,
                modifier = Modifier.size(17.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MujtamaOnlineGreen,
            modifier = Modifier.size(16.dp)
        )
    }
}

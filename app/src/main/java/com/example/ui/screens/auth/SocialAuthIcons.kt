package com.example.ui.screens.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MujtamaGold
import com.example.ui.theme.MujtamaPrimary
import com.example.ui.theme.MujtamaTeal

/**
 * أيقونة جوجل الرسمية رباعية الألوان (Google 4-Color 'G' Logo)
 */
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val outerRadius = w * 0.46f
        val strokeWidth = w * 0.21f

        val red = Color(0xFFEA4335)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)
        val blue = Color(0xFF4285F4)

        // رسم الأقواس الملونة لحرف G
        // 1. القوس العلوي الأحمر
        drawArc(
            color = red,
            startAngle = -140f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = Offset(cx - outerRadius, cy - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = strokeWidth)
        )

        // 2. القوس الأيسر الأصفر
        drawArc(
            color = yellow,
            startAngle = 140f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = Offset(cx - outerRadius, cy - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = strokeWidth)
        )

        // 3. القوس السفلي الأخضر
        drawArc(
            color = green,
            startAngle = 35f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = Offset(cx - outerRadius, cy - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = strokeWidth)
        )

        // 4. الجزء الأزرق وقضيب حرف G الأفقي
        drawArc(
            color = blue,
            startAngle = -35f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(cx - outerRadius, cy - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = strokeWidth)
        )

        // الخط الأفقي الأزرق في منتصف الحرف G
        drawRect(
            color = blue,
            topLeft = Offset(cx, cy - strokeWidth / 2f),
            size = Size(outerRadius, strokeWidth)
        )
    }
}

/**
 * أيقونة فيسبوك الرسمية (Facebook 'f' Logo)
 */
@Composable
fun FacebookLogoIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF1877F2)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.65f)) {
            val w = size.width
            val h = size.height

            val path = Path().apply {
                // حرف f باللون الأبيض
                moveTo(w * 0.7f, h)
                lineTo(w * 0.45f, h)
                lineTo(w * 0.45f, h * 0.55f)
                lineTo(w * 0.3f, h * 0.55f)
                lineTo(w * 0.3f, h * 0.38f)
                lineTo(w * 0.45f, h * 0.38f)
                lineTo(w * 0.45f, h * 0.25f)
                cubicTo(w * 0.45f, h * 0.10f, w * 0.55f, 0f, w * 0.78f, 0f)
                lineTo(w * 0.95f, 0f)
                lineTo(w * 0.95f, h * 0.18f)
                lineTo(w * 0.82f, h * 0.18f)
                cubicTo(w * 0.72f, h * 0.18f, w * 0.7f, h * 0.22f, w * 0.7f, h * 0.30f)
                lineTo(w * 0.7f, h * 0.38f)
                lineTo(w * 0.95f, h * 0.38f)
                lineTo(w * 0.90f, h * 0.55f)
                lineTo(w * 0.7f, h * 0.55f)
                close()
            }

            drawPath(path = path, color = Color.White, style = Fill)
        }
    }
}

/**
 * شعار تطبيق "مجتمعنا" الفاخر والهوية البصرية
 */
@Composable
fun AppBrandHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // اسم التطبيق
        Text(
            text = "Chat Live",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        // الشعار اللفظي
        Text(
            text = "دردشتك المباشرة للتواصل والألعاب",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

package com.example.ui.screens.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlowingMagenta
import com.example.ui.theme.MujtamaGold
import com.example.ui.theme.MujtamaOnlineGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

// ───────────────────────── الإعدادات والاقتصاد ─────────────────────────

private const val PLINKO_COST = 500
private const val PLINKO_ROWS = 16
private const val PLINKO_SEGMENTS = 17 // 1 سقوط أولي + 16 صف دبابيس
private const val PLINKO_PAD = 12 // dp: هامش جانبي مشترك بين الرسم وصف الخانات
private const val PLINKO_BUCKET_H = 34 // dp: ارتفاع صف الخانات

// 17 خانة متماثلة (من اليسار لليمين). المنتصف: ثلاث خانات x0 متجاورة.
private val PlinkoMultipliers = listOf(
    50f, 10f, 8f, 6f, 1.5f, 1f, 0.5f, 0f, 0f, 0f, 0.5f, 1f, 1.5f, 6f, 8f, 10f, 50f
)

// أوزان الاحتمال من 10000 (المجموع = 10000).
// x50 = 0.12% للخانتين معاً، العائد المتوسط ≈ 89%.
private val PlinkoWeights = listOf(
    6, 50, 60, 120, 750, 900, 850, 650, 3228, 650, 850, 900, 750, 120, 60, 50, 6
)

/** يختار الخانة النهائية بالاحتمالات المحددة أعلاه. */
internal fun pickPlinkoBucket(random: Random = Random): Int {
    var roll = random.nextInt(PlinkoWeights.sum())
    for (i in PlinkoWeights.indices) {
        roll -= PlinkoWeights[i]
        if (roll < 0) return i
    }
    return PlinkoWeights.lastIndex
}

internal fun plinkoPayout(bucket: Int): Int = (PLINKO_COST * PlinkoMultipliers[bucket]).toInt()

/** العائد المتوسط النظري (للتحقق/الاختبارات). */
internal fun plinkoExpectedReturn(): Double {
    var sum = 0.0
    for (i in PlinkoWeights.indices) sum += PlinkoWeights[i] * PlinkoMultipliers[i].toDouble()
    return sum / PlinkoWeights.sum()
}

// ───────────────────────── المسار والهندسة ─────────────────────────

/** مسار الكرة: عدد الارتدادات لليمين بعد كل خطوة (طوله 17). النتيجة النهائية = رقم الخانة. */
private fun buildPlinkoRights(bucket: Int): IntArray {
    val moves = MutableList(PLINKO_ROWS) { it < bucket }
    moves.shuffle()
    val rights = IntArray(PLINKO_ROWS + 1)
    for (i in 0 until PLINKO_ROWS) rights[i + 1] = rights[i] + if (moves[i]) 1 else 0
    return rights
}

private class PlinkoGeo(val w: Float, val h: Float, val pad: Float, val bucketH: Float) {
    val s = (w - 2f * pad) / 17f
    val cx = w / 2f
    val pinTop = h * 0.11f
    val rowH = (h - bucketH - pinTop) / PLINKO_ROWS
    val pinR = s * 0.11f
    val ballR = s * 0.30f
    val landingY = h - bucketH * 0.5f

    fun pinY(row: Int): Float = pinTop + row * rowH
    fun xAt(rights: Int, step: Int): Float = cx + (2 * rights - step) * s / 2f
}

private fun plinkoPoint(idx: Int, g: PlinkoGeo, rights: IntArray): Offset = when {
    idx == 0 -> Offset(g.cx, g.ballR)
    idx <= PLINKO_ROWS -> {
        val row = idx - 1
        Offset(g.xAt(rights[row], row), g.pinY(row) - g.pinR - g.ballR)
    }
    else -> Offset(g.xAt(rights[PLINKO_ROWS], PLINKO_ROWS), g.landingY)
}

private fun plinkoBallPos(t: Float, g: PlinkoGeo, rights: IntArray): Offset {
    val seg = t.toInt().coerceIn(0, PLINKO_SEGMENTS - 1)
    val f = (t - seg).coerceIn(0f, 1f)
    val a = plinkoPoint(seg, g, rights)
    val b = plinkoPoint(seg + 1, g, rights)
    val bounce = if (seg == 0) 0f else g.rowH * 0.45f
    val x = a.x + (b.x - a.x) * f
    val y = a.y + (b.y - a.y) * f * f - bounce * 4f * f * (1f - f)
    return Offset(x, y)
}

// ───────────────────────── الألوان والنصوص ─────────────────────────

private val PlinkoNavy = Color(0xFF070B2E)
private val PlinkoRoyal = Color(0xFF14258C)
private val PlinkoBoard = Color(0xFF0C1650)

private fun plinkoBucketColor(m: Float): Color = when {
    m <= 0f -> Color(0xFFC62828)
    m < 1f -> Color(0xFFEF6C00)
    m < 6f -> MujtamaOnlineGreen
    m < 50f -> MujtamaGold
    else -> GlowingMagenta
}

private fun plinkoLabelColor(m: Float): Color =
    if (m >= 1f && m < 50f) Color(0xFF101010) else Color.White

private fun plinkoLabel(m: Float): String =
    if (m == m.toInt().toFloat()) "x${m.toInt()}" else "x$m"

// ───────────────────────── قصاصات الاحتفال (x50) ─────────────────────────

private class PlinkoConfetti(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val sway: Float,
    val sz: Float,
    val rot: Float,
    val color: Color
)

private fun buildPlinkoConfetti(): List<PlinkoConfetti> {
    val palette = listOf(NeonGold, MujtamaGold, Color.White, GlowingMagenta)
    return List(70) {
        PlinkoConfetti(
            x = Random.nextFloat(),
            delay = Random.nextFloat() * 0.3f,
            speed = 0.6f + Random.nextFloat() * 0.6f,
            sway = Random.nextFloat() * 2f - 1f,
            sz = 6f + Random.nextFloat() * 8f,
            rot = Random.nextFloat() * 360f,
            color = palette[Random.nextInt(palette.size)]
        )
    }
}

// ───────────────────────── الواجهة ─────────────────────────

@Composable
fun PlinkoGameView(
    balance: Int,
    onSpend: (Int) -> Boolean,
    onEarn: (Int) -> Unit,
    onBack: () -> Unit,
    onResult: (multiplier: Float, payout: Int) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    val currentOnEarn by rememberUpdatedState(onEarn)
    val currentOnResult by rememberUpdatedState(onResult)

    val progress = remember { Animatable(0f) }
    val confettiProgress = remember { Animatable(1f) }
    var rights by remember { mutableStateOf<IntArray?>(null) }
    var dropping by remember { mutableStateOf(false) }
    var lastBucket by remember { mutableIntStateOf(-1) }
    var pendingPayout by remember { mutableIntStateOf(0) }
    var showJackpot by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<Float>() }
    val confetti = remember { buildPlinkoConfetti() }

    // لو خرج اللاعب والكرة بالهواء: الجائزة تنحسب له ولا تضيع
    DisposableEffect(Unit) {
        onDispose {
            if (pendingPayout > 0) {
                currentOnEarn(pendingPayout)
                pendingPayout = 0
            }
        }
    }

    fun startDrop() {
        if (dropping || balance < PLINKO_COST) return
        if (!onSpend(PLINKO_COST)) return
        val bucket = pickPlinkoBucket()
        val path = buildPlinkoRights(bucket)
        pendingPayout = plinkoPayout(bucket)
        lastBucket = -1
        dropping = true
        scope.launch {
            progress.snapTo(0f)
            rights = path
            progress.animateTo(
                PLINKO_SEGMENTS.toFloat(),
                tween(durationMillis = 3200, easing = LinearEasing)
            )
            val m = PlinkoMultipliers[bucket]
            val payout = pendingPayout
            pendingPayout = 0
            if (payout > 0) currentOnEarn(payout)
            lastBucket = bucket
            history.add(0, m)
            if (history.size > 6) history.removeAt(history.lastIndex)
            currentOnResult(m, payout)
            dropping = false
            if (m >= 50f) {
                showJackpot = true
                confettiProgress.snapTo(0f)
                confettiProgress.animateTo(1f, tween(durationMillis = 3500, easing = LinearEasing))
                showJackpot = false
            }
        }
    }

    val density = LocalDensity.current
    val padPx = with(density) { PLINKO_PAD.dp.toPx() }
    val bucketHPx = with(density) { PLINKO_BUCKET_H.dp.toPx() }
    val canDrop = !dropping && balance >= PLINKO_COST

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PlinkoNavy, PlinkoRoyal, PlinkoNavy)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // الشريط العلوي
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White
                    )
                }
                Text(
                    "بلينكو النقاط",
                    color = MujtamaGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(48.dp))
            }

            // الرصيد
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x33FFFFFF))
                    .border(1.dp, MujtamaGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الرصيد", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text(
                        "%,d".format(Locale.US, balance),
                        color = NeonGold,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // آخر النتائج
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (history.isEmpty()) {
                    Text(
                        "أسقط أول كرة وشوف حظك",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                history.forEach { m ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(plinkoBucketColor(m))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            plinkoLabel(m),
                            color = plinkoLabelColor(m),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // اللوحة (اتجاه ثابت LTR حتى تتطابق الخانات مع مسار الكرة)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.92f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(PlinkoBoard)
                        .border(1.5.dp, MujtamaGold.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                ) {
                    // الدبابيس
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val g = PlinkoGeo(size.width, size.height, padPx, bucketHPx)
                        for (row in 0 until PLINKO_ROWS) {
                            val n = row + 3
                            for (j in 0 until n) {
                                val c = Offset(g.cx + (j - (n - 1) / 2f) * g.s, g.pinY(row))
                                drawCircle(NeonCyan.copy(alpha = 0.15f), g.pinR * 2.2f, c)
                                drawCircle(Color.White.copy(alpha = 0.92f), g.pinR, c)
                            }
                        }
                    }

                    // الخانات
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(PLINKO_BUCKET_H.dp)
                            .padding(horizontal = PLINKO_PAD.dp)
                    ) {
                        PlinkoMultipliers.forEachIndexed { i, m ->
                            PlinkoBucket(m = m, highlighted = i == lastBucket)
                        }
                    }

                    // الكرة والقصاصات
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val g = PlinkoGeo(size.width, size.height, padPx, bucketHPx)
                        val r = rights
                        if (r != null) {
                            val pos = plinkoBallPos(progress.value, g, r)
                            drawCircle(NeonGold.copy(alpha = 0.25f), g.ballR * 1.9f, pos)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color.White, NeonGold, MujtamaGold),
                                    center = pos,
                                    radius = g.ballR
                                ),
                                radius = g.ballR,
                                center = pos
                            )
                        }
                        if (showJackpot) {
                            val cp = confettiProgress.value
                            confetti.forEach { p ->
                                val q = ((cp - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
                                if (q > 0f && q < 1f) {
                                    val x = p.x * size.width + sin(q * 8f + p.rot) * p.sway * 24f
                                    val y = -20f + q * p.speed * (size.height + 40f)
                                    rotate(degrees = p.rot + q * 540f, pivot = Offset(x, y)) {
                                        drawRect(
                                            color = p.color.copy(alpha = 1f - q * q),
                                            topLeft = Offset(x - p.sz / 2f, y - p.sz / 2f),
                                            size = Size(p.sz, p.sz * 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                                        AnimatedVisibility(
                        visible = showJackpot,
                        modifier = Modifier.align(Alignment.Center),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
    .align(Alignment.Center)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xCC000000))
                                .border(2.dp, NeonGold, RoundedCornerShape(16.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                "جائزة كبرى!",
                                color = NeonGold,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                                        }
                }
            }

            Spacer(Modifier.height(14.dp))

            // زر الإسقاط
            Button(
                onClick = { startDrop() },
                enabled = canDrop,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MujtamaGold,
                    contentColor = Color(0xFF1A1200),
                    disabledContainerColor = Color(0x55FFC400),
                    disabledContentColor = Color(0xAAFFFFFF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = when {
                        dropping -> "الكرة تسقط..."
                        balance < PLINKO_COST -> "رصيدك غير كافٍ (تحتاج $PLINKO_COST نقطة)"
                        else -> "إسقاط كرتك بـ $PLINKO_COST نقطة"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RowScope.PlinkoBucket(m: Float, highlighted: Boolean) {
    val c = plinkoBucketColor(m)
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(horizontal = 0.5.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Brush.verticalGradient(listOf(c, c.copy(alpha = 0.55f))))
            .then(
                if (highlighted) Modifier.border(2.dp, Color.White, RoundedCornerShape(5.dp))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = plinkoLabel(m),
            color = plinkoLabelColor(m),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}


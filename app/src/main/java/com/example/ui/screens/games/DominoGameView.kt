package com.example.ui.screens.games

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMatchMode
import com.example.ui.theme.MujtamaGold
import com.example.ui.theme.MujtamaGoldLight
import com.example.ui.theme.MujtamaOnlineGreen
import com.example.ui.theme.MujtamaPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * بيانات حجر الدومينو الكلاسيكي (Double-Six)
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

enum class SelectedChainEnd {
    NONE,
    LEFT,
    RIGHT
}

data class PlacedBoardTile(
    val tile: DominoTile,
    val orientation: TileOrientation = TileOrientation.HORIZONTAL
)

/**
 * مساعد تشغيل صوت الطقة الخشبية والاهتزاز عند وضع حجر الدومينو
 */
object DominoSoundAndHapticHelper {
    fun playWoodClack(context: Context, haptic: HapticFeedback, soundEnabled: Boolean = true, vibrationEnabled: Boolean = true) {
        if (vibrationEnabled) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(28)
                }
            } catch (_: Exception) {}
        }

        if (soundEnabled) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f)
                // توليد نغمة خشبية سريعة ودقيقة (Wooden Clack)
                Thread {
                    try {
                        val sampleRate = 22050
                        val numSamples = (sampleRate * 0.040).toInt()
                        val buffer = ShortArray(numSamples)
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            val envelope = Math.exp(-t * 110.0)
                            val freq = 1050.0 - (t * 6000.0).coerceAtMost(650.0)
                            val sample = (Math.sin(2.0 * Math.PI * freq * t) * envelope * 27000).toInt().coerceIn(-32767, 32767)
                            buffer[i] = sample.toShort()
                        }
                        val track = AudioTrack.Builder()
                            .setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_GAME)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(sampleRate)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build()
                            )
                            .setBufferSizeInBytes(buffer.size * 2)
                            .setTransferMode(AudioTrack.MODE_STATIC)
                            .build()
                        track.write(buffer, 0, buffer.size)
                        track.play()
                        track.setNotificationMarkerPosition(buffer.size)
                        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                            override fun onPeriodicNotification(t: AudioTrack?) {}
                            override fun onMarkerReached(t: AudioTrack?) {
                                try { t?.stop(); t?.release() } catch (_: Exception) {}
                            }
                        })
                    } catch (_: Exception) {}
                }.start()
            } catch (_: Exception) {}
        }
    }
}

/**
 * شاشة طاولة الدومينو الفاخرة بتخطيط عمودي (Portrait) لشخصين (1v1):
 * - ألوان الطاولة: قماش أخضر داكن كلاسيكي فاخر، شكل بيضاوي ممتد بالطول، إطار خارجي ذهبي بزخرفة ناعمة، وخلفية بني خشبي داكن فاخر (خشب الجوز/الماهوجني) بملمس واضح.
 * - إضاءة سبوت لايت ناعمة في منتصف الطاولة، وظلال خفيفة أسفل كل قطعة.
 * - قطع الدومينو: خلفية عاجية/كريمية دافئة، حواف ذهبية وبنية، نقاط سوداء كلاسيكية بتأثير بارز (Embossed)، والقطع المقلوبة بخلفية بنية غامقة/عنابية بنقش هندسي ذهبي وحواف ذهبية.
 * - الشريط العلوي (يمين → يسار): زر الإعدادات (⚙️) → عدد الجولة الحالية → النقاط "X/100" → عدد قطع السحب المتبقية كرقم فقط.
 * - صف الخصم: صورة الخصم من اليمين، وقطعه مخفية بجانبها، بدون أيقونات هدايا أو دردشة.
 * - منطقة اللعب: الطاولة البيضاوية الخضراء تعرض القطع المطروحة بمسار متعرج يتمدد تلقائياً لتفادي أي تداخل.
 * - صف المستخدم: صورة المستخدم من اليمين، بجانبها أيقونتان منفصلتان جنباً إلى جنب (الهدية 🎁 والدردشة 💬)، ثم قطع المستخدم مكشوفة.
 * - نظام النقاط ونهاية الجولة: تبدأ النقاط من صفر، وتُعرض بصيغة "X/100". عند وصول رصيد المستخدم إلى 100 نقطة بالضبط، تنتهي اللعبة تلقائياً وتظهر شاشة الفوز النهائية، مع ضمان عدم تجاوز الـ 100.
 * - مؤقت الدور: حلقة تقدّم دائرية حول صورة اللاعب صاحب الدور، مدته 15 ثانية بالضبط، يتناقص بصرياً ويتحول للأحمر في آخر 5 ثوانٍ.
 * - تأثير وضع القطعة: اهتزاز خفيف Haptic وصوت طقة خشبية قصيرة.
 * - السحب التلقائي عند عدم وجود حركة صالحة: يسحب تلقائياً قطعة واحدة، ويتوقف فوراً إذا كانت صالحة؛ وإلا يستمر حتى إيجاد قطعة صالحة أو نفاد الكومة.
 */
@Composable
fun DominoGameView(
    mode: GameMatchMode,
    opponentName: String,
    onBack: () -> Unit,
    onWinReward: (Int) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val displayName = remember(opponentName) {
        if (opponentName.isNotBlank()) opponentName else "الخصم"
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

    // نظام النقاط: يبدأ من 0 ومقيد بحد أقصى 100 بالضبط
    var userScore by remember { mutableIntStateOf(0) }
    var currentRound by remember { mutableIntStateOf(1) }

    // أدوار اللعب ومؤقت الـ 15 ثانية الدقيق
    var isUserTurn by remember { mutableStateOf(true) }
    var turnTimeRemaining by remember { mutableIntStateOf(15) }
    var isAutoDrawing by remember { mutableStateOf(false) }

    // آلية تحديد طرف السلسلة على الطاولة (من المنتصف/الأطراف المفتوحة)
    var selectedChainEnd by remember { mutableStateOf(SelectedChainEnd.NONE) }

    var statusMessage by remember { mutableStateOf("دورك للعب! اختر حجراً مناسباً للطرفين [$leftEnd] أو [$rightEnd]") }
    var isGameWonFinal by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showGiftDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // إعدادات الصوت والاهتزاز
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    // تشغيل التأثير عند وضع أي قطعة
    fun onTilePlayedFeedback() {
        DominoSoundAndHapticHelper.playWoodClack(context, haptic, soundEnabled, vibrationEnabled)
    }

    // إضافة النقاط بحد أقصى 100 بالضبط وفحص الفوز التلقائي
    fun addPointsToUser(points: Int) {
        val newScore = min(100, userScore + points)
        userScore = newScore
        if (newScore >= 100) {
            isGameWonFinal = true
            onWinReward(250)
            statusMessage = "🎉 انتصار ساحق! حققت 100/100 نقطة وفزت بالمباراة!"
        }
    }

    // مؤقت الدور: 15 ثانية بالضبط، يتناقص بصرياً ويتحول للأحمر في آخر 5 ثوانٍ
    LaunchedEffect(isUserTurn, isGameWonFinal, isAutoDrawing) {
        if (isGameWonFinal || isAutoDrawing) return@LaunchedEffect
        if (!isUserTurn) {
            selectedChainEnd = SelectedChainEnd.NONE
        }
        turnTimeRemaining = 15
        while (turnTimeRemaining > 0 && !isGameWonFinal && !isAutoDrawing) {
            delay(1000)
            turnTimeRemaining--
        }
        if (turnTimeRemaining == 0 && !isGameWonFinal && !isAutoDrawing) {
            statusMessage = if (isUserTurn) "انتهى وقتك (15 ثانية)! تم تمرير الدور للخصم." else "انتهى وقت الخصم وتم تمرير الدور إليك."
            selectedChainEnd = SelectedChainEnd.NONE
            isUserTurn = !isUserTurn
        }
    }

    // فحص انتهاء الجولة العادية
    fun checkRoundEnd() {
        if (isGameWonFinal) return
        if (userTiles.isEmpty()) {
            addPointsToUser(35)
            statusMessage = "🎉 أنهيت جميع قطعك وكسبت الجولة! (+35 نقطة)"
            if (userScore < 100) {
                currentRound++
            }
        } else if (opponentTiles.isEmpty()) {
            statusMessage = "أنهى $displayName قطعه وفاز بالجولة."
            currentRound++
        }
    }

    LaunchedEffect(userTiles.size, opponentTiles.size) {
        checkRoundEnd()
    }

    // تنفيذ لعب الحجر من قبل المستخدم
    fun executePlayerPlay(tile: DominoTile, playOnLeft: Boolean) {
        val currentL = boardChain.first().tile.left
        val currentR = boardChain.last().tile.right

        if (playOnLeft) {
            val orientedTile = if (tile.right == currentL) tile else tile.reversed()
            val orientation = if (orientedTile.isDouble) TileOrientation.HORIZONTAL else TileOrientation.VERTICAL
            boardChain = listOf(PlacedBoardTile(orientedTile, orientation)) + boardChain
            userTiles = userTiles.filter { it.id != tile.id }
            addPointsToUser(orientedTile.pipSum)
            onTilePlayedFeedback()
            statusMessage = "وضعت [${orientedTile.left}|${orientedTile.right}] بنجاح!"
            selectedChainEnd = SelectedChainEnd.NONE
            isUserTurn = false
        } else {
            val orientedTile = if (tile.left == currentR) tile else tile.reversed()
            val orientation = if (orientedTile.isDouble) TileOrientation.HORIZONTAL else TileOrientation.VERTICAL
            boardChain = boardChain + PlacedBoardTile(orientedTile, orientation)
            userTiles = userTiles.filter { it.id != tile.id }
            addPointsToUser(orientedTile.pipSum)
            onTilePlayedFeedback()
            statusMessage = "وضعت [${orientedTile.left}|${orientedTile.right}] بنجاح!"
            selectedChainEnd = SelectedChainEnd.NONE
            isUserTurn = false
        }
    }

    // الضغط على طرف موضوع على الطاولة لتحديد القطع المتوافقة في اليد
    fun onBoardEndClicked(isLeft: Boolean) {
        if (!isUserTurn || isGameWonFinal || isAutoDrawing) return

        val currentL = boardChain.first().tile.left
        val currentR = boardChain.last().tile.right

        if (boardChain.size == 1) {
            val hasMatchL = userTiles.any { it.left == currentL || it.right == currentL }
            val hasMatchR = userTiles.any { it.left == currentR || it.right == currentR }
            if (hasMatchL || hasMatchR) {
                selectedChainEnd = if (selectedChainEnd == SelectedChainEnd.NONE) {
                    if (hasMatchL) SelectedChainEnd.LEFT else SelectedChainEnd.RIGHT
                } else {
                    SelectedChainEnd.NONE
                }
                if (selectedChainEnd != SelectedChainEnd.NONE) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            } else {
                selectedChainEnd = SelectedChainEnd.NONE
            }
            return
        }

        if (isLeft) {
            val hasMatch = userTiles.any { it.left == currentL || it.right == currentL }
            if (hasMatch) {
                selectedChainEnd = if (selectedChainEnd == SelectedChainEnd.LEFT) {
                    SelectedChainEnd.NONE
                } else {
                    SelectedChainEnd.LEFT
                }
                if (selectedChainEnd == SelectedChainEnd.LEFT) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            } else {
                selectedChainEnd = SelectedChainEnd.NONE
            }
        } else {
            val hasMatch = userTiles.any { it.left == currentR || it.right == currentR }
            if (hasMatch) {
                selectedChainEnd = if (selectedChainEnd == SelectedChainEnd.RIGHT) {
                    SelectedChainEnd.NONE
                } else {
                    SelectedChainEnd.RIGHT
                }
                if (selectedChainEnd == SelectedChainEnd.RIGHT) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            } else {
                selectedChainEnd = SelectedChainEnd.NONE
            }
        }
    }

    fun onUserTileClicked(tile: DominoTile) {
        if (!isUserTurn || isGameWonFinal || isAutoDrawing) {
            statusMessage = "انتظر دورك للعب!"
            return
        }

        val currentL = boardChain.first().tile.left
        val currentR = boardChain.last().tile.right

        // عند تحديد طرف معين على الطاولة، يتم وضع الحجر المحدد تلقائياً في المكان الصحيح
        if (selectedChainEnd == SelectedChainEnd.LEFT) {
            if (tile.left == currentL || tile.right == currentL) {
                executePlayerPlay(tile, playOnLeft = true)
                selectedChainEnd = SelectedChainEnd.NONE
            }
            return
        } else if (selectedChainEnd == SelectedChainEnd.RIGHT) {
            if (tile.left == currentR || tile.right == currentR) {
                executePlayerPlay(tile, playOnLeft = false)
                selectedChainEnd = SelectedChainEnd.NONE
            }
            return
        }

        val canPlayLeft = tile.left == currentL || tile.right == currentL
        val canPlayRight = tile.left == currentR || tile.right == currentR

        if (canPlayLeft && canPlayRight && currentL != currentR) {
            statusMessage = "هذا الحجر يطابق الطرفين! اضغط على الطرف المطلوب في السلسلة أولاً 👆"
        } else if (canPlayLeft) {
            executePlayerPlay(tile, playOnLeft = true)
        } else if (canPlayRight) {
            executePlayerPlay(tile, playOnLeft = false)
        } else {
            statusMessage = "هذا الحجر لا يطابق أي من الطرفين المفتوحين [$currentL] أو [$currentR] ⚠️"
        }
    }

    // ميزة: السحب التلقائي عند عدم وجود حركة صالحة
    // عند عدم امتلاك اللاعب قطعة صالحة، يسحب تلقائياً قطعة واحدة، ويتوقف السحب فوراً إذا كانت صالحة للعب؛
    // وإلا يستمر السحب حتى إيجاد قطعة صالحة أو نفاد الكومة (عندها ينتقل الدور تلقائياً).
    LaunchedEffect(isUserTurn, boardChain.size, isGameWonFinal) {
        if (isGameWonFinal) return@LaunchedEffect

        val currentL = boardChain.first().tile.left
        val currentR = boardChain.last().tile.right

        if (isUserTurn) {
            val hasValidMove = userTiles.any { it.left == currentL || it.right == currentL || it.left == currentR || it.right == currentR }
            if (!hasValidMove) {
                isAutoDrawing = true
                statusMessage = "لا تملك حركة صالحة! جاري السحب التلقائي..."
                delay(600)

                var foundPlayable = false
                while (!foundPlayable && boneyardTiles.isNotEmpty() && !isGameWonFinal) {
                    val drawn = boneyardTiles.first()
                    boneyardTiles = boneyardTiles.drop(1)
                    userTiles = userTiles + drawn
                    onTilePlayedFeedback()

                    val isDrawnPlayable = (drawn.left == currentL || drawn.right == currentL || drawn.left == currentR || drawn.right == currentR)
                    if (isDrawnPlayable) {
                        foundPlayable = true
                        statusMessage = "تم سحب حجر صالح للعب [${drawn.left}|${drawn.right}]! يمكنك لعبه الآن."
                        break
                    } else {
                        statusMessage = "سحب [${drawn.left}|${drawn.right}] (غير صالح)، جاري السحب مجدداً..."
                        delay(500)
                    }
                }

                if (!foundPlayable && boneyardTiles.isEmpty()) {
                    statusMessage = "نفد بنك السحب ولا توجد حركة صالحة! تم تمرير الدور تلقائياً."
                    delay(800)
                    isAutoDrawing = false
                    isUserTurn = false
                } else {
                    isAutoDrawing = false
                }
            }
        } else {
            // دور الخصم الذكي (AI) مع السحب التلقائي
            delay(1000)
            if (isGameWonFinal) return@LaunchedEffect

            val matchLeft = opponentTiles.find { it.left == currentL || it.right == currentL }
            val matchRight = opponentTiles.find { it.left == currentR || it.right == currentR }

            if (matchLeft != null) {
                val tileToPlay = if (matchLeft.right == currentL) matchLeft else matchLeft.reversed()
                val orientation = if (tileToPlay.isDouble) TileOrientation.HORIZONTAL else TileOrientation.VERTICAL
                boardChain = listOf(PlacedBoardTile(tileToPlay, orientation)) + boardChain
                opponentTiles = opponentTiles.filter { it.id != matchLeft.id }
                onTilePlayedFeedback()
                statusMessage = "$displayName لعب [${tileToPlay.left}|${tileToPlay.right}] على الطرف العلوي."
                delay(500)
                isUserTurn = true
            } else if (matchRight != null) {
                val tileToPlay = if (matchRight.left == currentR) matchRight else matchRight.reversed()
                val orientation = if (tileToPlay.isDouble) TileOrientation.HORIZONTAL else TileOrientation.VERTICAL
                boardChain = boardChain + PlacedBoardTile(tileToPlay, orientation)
                opponentTiles = opponentTiles.filter { it.id != matchRight.id }
                onTilePlayedFeedback()
                statusMessage = "$displayName لعب [${tileToPlay.left}|${tileToPlay.right}] على الطرف السفلي."
                delay(500)
                isUserTurn = true
            } else {
                // سحب تلقائي للخصم
                statusMessage = "$displayName لا يملك حركة صالحة، يسحب تلقائياً..."
                var opponentFound = false

                while (!opponentFound && boneyardTiles.isNotEmpty() && !isGameWonFinal) {
                    delay(600)
                    val drawn = boneyardTiles.first()
                    boneyardTiles = boneyardTiles.drop(1)
                    opponentTiles = opponentTiles + drawn
                    onTilePlayedFeedback()

                    if (drawn.left == currentL || drawn.right == currentL) {
                        opponentFound = true
                        val tileToPlay = if (drawn.right == currentL) drawn else drawn.reversed()
                        val orientation = if (tileToPlay.isDouble) TileOrientation.HORIZONTAL else TileOrientation.VERTICAL
                        boardChain = listOf(PlacedBoardTile(tileToPlay, orientation)) + boardChain
                        opponentTiles = opponentTiles.filter { it.id != drawn.id }
                        statusMessage = "$displayName سحب ولعب [${tileToPlay.left}|${tileToPlay.right}] على الطرف العلوي."
                        break
                    } else if (drawn.left == currentR || drawn.right == currentR) {
                        opponentFound = true
                        val tileToPlay = if (drawn.left == currentR) drawn else drawn.reversed()
                        val orientation = if (tileToPlay.isDouble) TileOrientation.HORIZONTAL else TileOrientation.VERTICAL
                        boardChain = boardChain + PlacedBoardTile(tileToPlay, orientation)
                        opponentTiles = opponentTiles.filter { it.id != drawn.id }
                        statusMessage = "$displayName سحب ولعب [${tileToPlay.left}|${tileToPlay.right}] على الطرف السفلي."
                        break
                    }
                }

                if (!opponentFound && boneyardTiles.isEmpty()) {
                    statusMessage = "$displayName لا يملك حركة ونفد بنك السحب، مرّر الدور لك."
                }
                delay(500)
                isUserTurn = true
            }
        }
    }

    // فرض اتجاه اليمين لليسار لكامل واجهة الشاشة
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF140804))
                .systemBarsPadding()
                .testTag("domino_luxury_portrait_screen")
        ) {
            // رسم الطاولة البيضاوية الخضراء والخلفية الخشبية الماهوجني مع الإضاءة والزخرفة
            LuxuryDominoOvalTableCanvas(modifier = Modifier.fillMaxSize())

            // التخطيط العمودي للشاشة (من الأعلى للأسفل)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // -------------------------------------------------------------
                // 1. الشريط العلوي (يمين → يسار):
                // زر الإعدادات (⚙️) → عدد الجولة الحالية → النقاط "X/100" → عدد قطع السحب المتبقية كرقم فقط
                // -------------------------------------------------------------
                DominoTopBarLuxury(
                    currentRound = currentRound,
                    userScore = userScore,
                    boneyardCount = boneyardTiles.size,
                    onSettingsClick = { showSettingsDialog = true }
                )

                // -------------------------------------------------------------
                // 2. صف الخصم:
                // صورة الخصم من اليمين (مع مؤقت الدور)، وقطعه مخفية (Face-down) بجانبها — بدون أي أيقونات هدايا/دردشة هنا
                // -------------------------------------------------------------
                OpponentRowLuxury(
                    name = displayName,
                    tileCount = opponentTiles.size,
                    isTurn = !isUserTurn,
                    timeRemaining = turnTimeRemaining
                )

                // -------------------------------------------------------------
                // 3. منطقة اللعب:
                // الطاولة البيضاوية الخضراء تعرض القطع المطروحة من المنتصف وتتمدد تدريجياً، نظيفة تماماً بدون أي نصوص
                // -------------------------------------------------------------
                DominoPlayAreaSerpentine(
                    boardChain = boardChain,
                    leftEnd = leftEnd,
                    rightEnd = rightEnd,
                    selectedChainEnd = selectedChainEnd,
                    userTiles = userTiles,
                    isUserTurn = isUserTurn,
                    onEndTileClick = { isLeft -> onBoardEndClicked(isLeft) },
                    initialTileId = startingDouble.id,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // -------------------------------------------------------------
                // 4. صف المستخدم:
                // صورة المستخدم من اليمين (مع مؤقت الدور)، بجانبها أيقونتان منفصلتان جنباً إلى جنب (الهدية 🎁 والدردشة 💬)
                // ثم قطع المستخدم مكشوفة (Face-up) مع التحديد البصري (Highlight)
                // -------------------------------------------------------------
                UserRowLuxury(
                    userTiles = userTiles,
                    isUserTurn = isUserTurn,
                    timeRemaining = turnTimeRemaining,
                    leftEnd = leftEnd,
                    rightEnd = rightEnd,
                    selectedChainEnd = selectedChainEnd,
                    onTileClick = { onUserTileClicked(it) },
                    onGiftClick = { showGiftDialog = true },
                    onChatClick = { showChatDialog = true }
                )
            }

            // -------------------------------------------------------------
            // النوافذ الحوارية والشاشات المنبثقة
            // -------------------------------------------------------------
            if (showSettingsDialog) {
                DominoSettingsDialog2P(
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                    onSoundToggle = { soundEnabled = it },
                    onVibrationToggle = { vibrationEnabled = it },
                    onExitGame = {
                        showSettingsDialog = false
                        showExitDialog = true
                    },
                    onDismiss = { showSettingsDialog = false }
                )
            }

            if (showExitDialog) {
                DominoExitConfirmDialog2P(
                    onDismiss = { showExitDialog = false },
                    onConfirmExit = {
                        showExitDialog = false
                        onBack()
                    }
                )
            }

            if (showChatDialog) {
                DominoQuickChatDialog2P(
                    onDismiss = { showChatDialog = false },
                    onSelectPhrase = { phrase ->
                        statusMessage = "أنت: $phrase"
                        showChatDialog = false
                    }
                )
            }

            if (showGiftDialog) {
                DominoSendGiftDialog(
                    opponentName = displayName,
                    onSendGift = { giftName ->
                        statusMessage = "أرسلت $giftName إلى $displayName! 🎁"
                        showGiftDialog = false
                    },
                    onDismiss = { showGiftDialog = false }
                )
            }

            // شاشة النتيجة النهائية عند وصول الرصيد إلى 100 نقطة بالضبط
            if (isGameWonFinal) {
                DominoFinalVictoryDialog(
                    score = userScore,
                    onRestart = {
                        userScore = 0
                        currentRound = 1
                        isGameWonFinal = false
                        val reshuffled = initialDeck.shuffled()
                        userTiles = reshuffled.subList(0, 7)
                        opponentTiles = reshuffled.subList(7, 14)
                        boneyardTiles = reshuffled.subList(14, 28)
                        val newStart = (userTiles + opponentTiles).filter { it.isDouble }.maxByOrNull { it.pipSum } ?: DominoTile("start", 6, 6)
                        boardChain = listOf(PlacedBoardTile(newStart, TileOrientation.VERTICAL))
                        isUserTurn = true
                    },
                    onExit = onBack
                )
            }
        }
    }
}

/**
 * رسم الطاولة المربعة/المستطيلة الخضراء والخلفية الخشبية الماهوجني مع الإضاءة والزخرفة:
 * - سطح الطاولة بلون أخضر داكن (قماش كلاسيكي فاخر)، مستطيلة الشكل بحواف دائرية ناعمة.
 * - إطار خارجي ذهبي بزخرفة بسيطة.
 * - خلفية الشاشة خارج الطاولة بني خشبي داكن فاخر (خشب الجوز/الماهوجني) بملمس حبيبات الخشب.
 * - سبوت لايت ناعمة في منتصف الطاولة.
 */
@Composable
fun LuxuryDominoOvalTableCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. خلفية الخشب الداكن الفاخر (الجوز / الماهوجني) مع تدرج وملمس
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF2B1309), // بني ماهوجني عميق في المنتصف
                    Color(0xFF1B0A04), // بني جوز داكن
                    Color(0xFF0F0502)  // حواف خشبية شبه سوداء
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = h * 0.65f
            )
        )

        // خطوط ملمس الخشب الدقيقة (Wood Grain)
        val woodLineColor = Color(0xFF381A0E).copy(alpha = 0.28f)
        val lineSpacing = 16.dp.toPx()
        var currentY = 0f
        while (currentY < h) {
            drawLine(
                color = woodLineColor,
                start = Offset(0f, currentY),
                end = Offset(w, currentY + 8.dp.toPx()),
                strokeWidth = 1.2.dp.toPx()
            )
            currentY += lineSpacing
        }

        // 2. الطاولة المستطيلة بحواف دائرية (Rounded rectangle table)
        val tableInsetX = 8.dp.toPx()
        val tableInsetY = 16.dp.toPx()
        val tableWidth = w - tableInsetX * 2
        val tableHeight = h - tableInsetY * 2
        val tableCorner = CornerRadius(22.dp.toPx(), 22.dp.toPx())

        // الإطار الخارجي الذهبي بزخرفة ناعمة
        val outerGoldThickness = 4.dp.toPx()
        drawRoundRect(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFD4AF37),
                    Color(0xFFF7E2C6),
                    Color(0xFFAA8036),
                    Color(0xFFD4AF37)
                ),
                center = Offset(w * 0.5f, h * 0.5f)
            ),
            topLeft = Offset(tableInsetX, tableInsetY),
            size = Size(tableWidth, tableHeight),
            cornerRadius = tableCorner,
            style = Stroke(width = outerGoldThickness)
        )

        // خط الزخرفة الذهبي المنقط الداخلي
        val innerDashedInset = 5.dp.toPx()
        drawRoundRect(
            color = Color(0xFFE4BC7E).copy(alpha = 0.75f),
            topLeft = Offset(tableInsetX + innerDashedInset, tableInsetY + innerDashedInset),
            size = Size(tableWidth - innerDashedInset * 2, tableHeight - innerDashedInset * 2),
            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
            style = Stroke(
                width = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()), 0f)
            )
        )

        // 3. سطح الطاولة الأخضر الداكن (قماش كلاسيكي فاخر) مع سبوت لايت ناعمة في المنتصف
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF145E3B), // سبوت لايت مشرق وناعم في المركز
                    Color(0xFF0E462B), // قماش كلاسيكي أخضر كازينو
                    Color(0xFF072918), // أخضر داكن فاخر
                    Color(0xFF04190E)  // أطراف داكنة وظلال على الحواف
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = tableWidth * 0.75f
            ),
            topLeft = Offset(tableInsetX + 6.dp.toPx(), tableInsetY + 6.dp.toPx()),
            size = Size(tableWidth - 12.dp.toPx(), tableHeight - 12.dp.toPx()),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
        )

        // حزام ذهبي رقيق في منتصف القماش لتحديد ملعب الدومينو
        drawRoundRect(
            color = Color(0xFFC7985D).copy(alpha = 0.35f),
            topLeft = Offset(tableInsetX + 18.dp.toPx(), tableInsetY + 24.dp.toPx()),
            size = Size(tableWidth - 36.dp.toPx(), tableHeight - 48.dp.toPx()),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
            style = Stroke(width = 0.8.dp.toPx())
        )
    }
}

/**
 * الشريط العلوي (يمين → يسار):
 * زر الإعدادات (⚙️) → عدد الجولة الحالية → النقاط "X/100" → عدد قطع السحب المتبقية كرقم فقط (بدون عرضها على الطاولة)
 */
@Composable
fun DominoTopBarLuxury(
    currentRound: Int,
    userScore: Int,
    boneyardCount: Int,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF241007).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFC7985D)),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("domino_top_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. زر الإعدادات (⚙️) على اليمين (في بيئة RTL)
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B1B0E))
                    .border(1.dp, Color(0xFFD4AF37), CircleShape)
                    .testTag("domino_top_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "الإعدادات",
                    tint = Color(0xFFF7E2C6),
                    modifier = Modifier.size(20.dp)
                )
            }

            // 2. عدد الجولة الحالية
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF381A0E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5A2B))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "🏆", fontSize = 12.sp)
                    Text(
                        text = "الجولة $currentRound",
                        color = Color(0xFFFBE5C8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. النقاط بصيغة "X/100" (بدأت من صفر وتصل لـ 100)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF104A2C),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFD4AF37))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "⭐", fontSize = 12.sp)
                    Text(
                        text = "$userScore/100",
                        color = Color(0xFFFFE082),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // 4. عدد قطع السحب المتبقية كرقم فقط (بدون عرضها على الطاولة)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF381A0E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5A2B))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "🀄", fontSize = 12.sp)
                    Text(
                        text = "السحب: $boneyardCount",
                        color = Color(0xFFE2CBB7),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * صف الخصم:
 * صورة الخصم من اليمين (مع مؤقت الدور الـ 15 ثانية)، وقطعه مخفية (Face-down) بجانبها — بدون أي أيقونات هدايا/دردشة هنا
 */
@Composable
fun OpponentRowLuxury(
    name: String,
    tileCount: Int,
    isTurn: Boolean,
    timeRemaining: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // صورة الخصم من اليمين مع مؤقت الدور الدائري (15 ثانية)
        PlayerAvatarWithTimerRing(
            name = name,
            emoji = "👨",
            avatarBg = Color(0xFF2E7D32),
            isCurrentTurn = isTurn,
            timeRemaining = timeRemaining
        )

        Spacer(modifier = Modifier.width(12.dp))

        // قطع الخصم مخفية (Face-down) بخلفية بنية غامقة/عنابية ونقش هندسي ذهبي
        OpponentFaceDownTilesCluster(count = tileCount)
    }
}

/**
 * حلقة مؤقت الدور الدائرية (15 ثانية بالضبط):
 * تتناقص بصرياً، وتتحول للأحمر في آخر 5 ثوانٍ
 */
@Composable
fun PlayerAvatarWithTimerRing(
    name: String,
    emoji: String,
    avatarBg: Color,
    isCurrentTurn: Boolean,
    timeRemaining: Int,
    modifier: Modifier = Modifier
) {
    val progress = remember(timeRemaining) {
        (timeRemaining / 15f).coerceIn(0f, 1f)
    }

    // التحول للأحمر في آخر 5 ثوانٍ
    val isCritical = timeRemaining <= 5
    val ringColor = if (isCurrentTurn) {
        if (isCritical) Color(0xFFE53935) else Color(0xFFD4AF37)
    } else {
        Color(0xFF5A3B28)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            // شريط التقدم الدائري للـ 15 ثانية
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 3.dp.toPx()
                // خلفية الحلقة
                drawCircle(
                    color = Color(0xFF2D1409),
                    radius = (size.minDimension - strokeW) / 2f,
                    style = Stroke(width = strokeW)
                )
                // تقدم الوقت الفعلي
                if (isCurrentTurn) {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = strokeW)
                    )
                }
            }

            // صورة اللاعب
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }

            // عرض الثواني المتبقية كبادج صغير عند دوره
            if (isCurrentTurn) {
                Surface(
                    shape = CircleShape,
                    color = if (isCritical) Color(0xFFD32F2F) else Color(0xFF381A0E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37)),
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$timeRemaining",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // اسم اللاعب
        Text(
            text = name,
            color = Color(0xFFF7E2C6),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * قطع الخصم المقلوبة (Face-down):
 * خلفية بنية غامقة/عنابية بنقش هندسي بسيط وحواف ذهبية
 */
@Composable
fun OpponentFaceDownTilesCluster(count: Int, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy((-8).dp), // تراكب جزئي أنيق كالورق
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) {
            DominoFaceDownTileView()
        }
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF291208).copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF8B5A2B))
        ) {
            Text(
                text = "$count قطع",
                color = Color(0xFFD7BA9D),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * رسم ظهر حجر الدومينو المقلوب:
 * خلفية بنية غامقة/عنابية بنقش هندسي ذهبي وحواف ذهبية
 */
@Composable
fun DominoFaceDownTileView(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF361009), // عنابي / بني غامق
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFD4AF37)), // حواف ذهبية
        shadowElevation = 3.dp,
        modifier = modifier.size(width = 24.dp, height = 44.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // تدرج لوني عنابي داكن
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF4A1810), Color(0xFF240A06))
                )
            )

            // نقش هندسي ماسي ذهبي بسيط في المنتصف (Geometric Diamond Lattice)
            val goldColor = Color(0xFFD4AF37).copy(alpha = 0.6f)
            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.25f)
                lineTo(w * 0.85f, h * 0.5f)
                lineTo(w * 0.5f, h * 0.75f)
                lineTo(w * 0.15f, h * 0.5f)
                close()
            }
            drawPath(path = path, color = goldColor, style = Stroke(width = 1.dp.toPx()))

            drawCircle(
                color = goldColor,
                radius = 1.5.dp.toPx(),
                center = Offset(w * 0.5f, h * 0.5f)
            )
        }
    }
}

/**
 * منطقة اللعب المركزية:
 * 1. الانعطاف التلقائي (Auto-turn / Zigzag Layout): تحافظ جميع القطع على حجمها
 *    الطبيعي الثابت دائماً دون أي تصغير.
 * 2. عند اقتراب السلسلة (لأعلى أو لأسفل) من حافة منطقة اللعب، تنعطف تلقائياً
 *    بزاوية 90 درجة وتكمل أفقياً، وإذا اقتربت لاحقاً من حافة جانبية تنعطف مجدداً
 *    للاتجاه العمودي، بنمط متعرج/حلزوني (S) حتى نهاية الجولة.
 * 3. جميع القطع تبقى مرئية بالكامل ضمن حدود منطقة اللعب مهما طالت السلسلة.
 * 4. الحفاظ على نقطة انطلاق السلسلة من منتصف الطاولة تماماً والتمدد المتماثل
 *    للأعلى وللأسفل مع ثبات المنتصف الأصلي.
 * 5. بقاء صفي الخصم والمستخدم بحجمهما الطبيعي الكامل دون أي تصغير.
 * 6. الحفاظ على نظافة ساحة اللعب مع التحديد التفاعلي (Highlight) للأطراف المفتوحة.
 */

private enum class SnakeDir { UP, DOWN, LEFT, RIGHT }

@Composable
fun DominoPlayAreaSerpentine(
    boardChain: List<PlacedBoardTile>,
    leftEnd: Int,
    rightEnd: Int,
    selectedChainEnd: SelectedChainEnd,
    userTiles: List<DominoTile>,
    isUserTurn: Boolean,
    onEndTileClick: (isLeft: Boolean) -> Unit,
    initialTileId: String = "",
    modifier: Modifier = Modifier
) {
    if (boardChain.isEmpty()) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    val centerTileId = remember(initialTileId) {
        if (initialTileId.isNotEmpty()) initialTileId else (boardChain.firstOrNull()?.tile?.id ?: "")
    }

    val centerIndex = remember(boardChain, centerTileId) {
        val idx = boardChain.indexOfFirst { it.tile.id == centerTileId }
        if (idx >= 0) idx else (boardChain.size / 2).coerceAtLeast(0)
    }

    val baseTileW = 33.dp
    val baseTileH = 58.dp
    val tileSpacing = 4.dp

    fun rawDims(orientation: TileOrientation): Pair<Dp, Dp> =
        if (orientation == TileOrientation.VERTICAL) baseTileW to baseTileH else baseTileH to baseTileW

    // يحدد الاتجاه الفعلي لرسم القطعة (طولية أم عرضية) بحيث تبقى القطع "المزدوجة"
    // عمودية دائماً على مسار السلسلة، أياً كان اتجاه المسار الحالي (عمودي أو أفقي)
    fun effectiveOrientation(originalOrientation: TileOrientation, dir: SnakeDir): TileOrientation {
        val isCrosswise = originalOrientation == TileOrientation.HORIZONTAL
        val pathIsVertical = dir == SnakeDir.UP || dir == SnakeDir.DOWN
        val shouldRenderWide = if (pathIsVertical) isCrosswise else !isCrosswise
        return if (shouldRenderWide) TileOrientation.HORIZONTAL else TileOrientation.VERTICAL
    }

    data class Placement(val x: Dp, val y: Dp, val w: Dp, val h: Dp, val renderOrientation: TileOrientation)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val margin = 10.dp
        val topLimit = margin
        val bottomLimit = maxHeight - margin
        val leftLimit = margin
        val rightLimit = maxWidth - margin
        val centerX = maxWidth / 2
        val centerY = maxHeight / 2

        val placements = remember(boardChain, centerIndex, maxWidth, maxHeight) {
            val result = arrayOfNulls<Placement>(boardChain.size)

            val centerOrientation = boardChain[centerIndex].orientation
            val (centerW, centerH) = rawDims(centerOrientation)
            result[centerIndex] = Placement(centerX - centerW / 2, centerY - centerH / 2, centerW, centerH, centerOrientation)

            fun walk(indices: List<Int>, initialDir: SnakeDir, jogStart: SnakeDir) {
                var dir = initialDir
                var jog = jogStart
                var lastCenterX = centerX
                var lastCenterY = centerY
                var lastW = centerW
                var lastH = centerH

                for (idx in indices) {
                    val orientation = boardChain[idx].orientation

                    fun place(direction: SnakeDir): Placement {
                        val renderOrientation = effectiveOrientation(orientation, direction)
                        val (w, h) = rawDims(renderOrientation)
                        val cx: Dp
                        val cy: Dp
                        when (direction) {
                            SnakeDir.UP -> { cx = lastCenterX; cy = lastCenterY - lastH / 2 - tileSpacing - h / 2 }
                            SnakeDir.DOWN -> { cx = lastCenterX; cy = lastCenterY + lastH / 2 + tileSpacing + h / 2 }
                            SnakeDir.LEFT -> { cx = lastCenterX - lastW / 2 - tileSpacing - w / 2; cy = lastCenterY }
                            SnakeDir.RIGHT -> { cx = lastCenterX + lastW / 2 + tileSpacing + w / 2; cy = lastCenterY }
                        }
                        return Placement(cx - w / 2, cy - h / 2, w, h, renderOrientation)
                    }

                    var p = place(dir)
                    val needsTurn = when (dir) {
                        SnakeDir.UP -> p.y < topLimit
                        SnakeDir.DOWN -> (p.y + p.h) > bottomLimit
                        SnakeDir.LEFT -> p.x < leftLimit
                        SnakeDir.RIGHT -> (p.x + p.w) > rightLimit
                    }

                    if (needsTurn) {
                        dir = if (dir == SnakeDir.UP || dir == SnakeDir.DOWN) {
                            val newDir = jog
                            jog = if (jog == SnakeDir.RIGHT) SnakeDir.LEFT else SnakeDir.RIGHT
                            newDir
                        } else {
                            initialDir
                        }
                        p = place(dir)
                    }

                    result[idx] = p
                    lastCenterX = p.x + p.w / 2
                    lastCenterY = p.y + p.h / 2
                    lastW = p.w
                    lastH = p.h
                }
            }

            walk((centerIndex - 1 downTo 0).toList(), SnakeDir.UP, SnakeDir.LEFT)
            walk((centerIndex + 1 until boardChain.size).toList(), SnakeDir.DOWN, SnakeDir.RIGHT)

            result
        }

        boardChain.forEachIndexed { index, placed ->
            val p = placements.getOrNull(index) ?: return@forEachIndexed
            val isLeftEnd = index == 0
            val isRightEnd = index == boardChain.size - 1
            val isEnd = isLeftEnd || isRightEnd

            val isSelected = when {
                isLeftEnd && selectedChainEnd == SelectedChainEnd.LEFT -> true
                isRightEnd && selectedChainEnd == SelectedChainEnd.RIGHT -> true
                else -> false
            }

            val hasMatchingTile = when {
                boardChain.size == 1 -> userTiles.any {
                    it.left == leftEnd || it.right == leftEnd || it.left == rightEnd || it.right == rightEnd
                }
                isLeftEnd -> userTiles.any { it.left == leftEnd || it.right == leftEnd }
                isRightEnd -> userTiles.any { it.left == rightEnd || it.right == rightEnd }
                else -> false
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = p.x, y = p.y)
            ) {
                ClassicDominoTileView2P(
                    tile = placed.tile,
                    orientation = p.renderOrientation,
                    isLeftEnd = isLeftEnd,
                    isRightEnd = isRightEnd,
                    isSelected = isSelected,
                    isPlayableEnd = isUserTurn && isEnd && hasMatchingTile,
                    scale = 1.0f
                )

                if (isUserTurn && isEnd) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clickable { onEndTileClick(isLeftEnd) }
                    )
                }
            }
        }
    }
}
/**
 * صف المستخدم:
 * صورة المستخدم من اليمين (مع مؤقت الدور الـ 15 ثانية)،
 * بجانبها أيقونتان منفصلتان جنباً إلى جنب — أيقونة الهدايا (🎁) وأيقونة الدردشة (💬) — بدون دمجهما في أيقونة واحدة!
 * ثم قطع المستخدم مكشوفة (Face-up) مع التحديد البصري (Highlight) للقطع المتوافقة عند اختيار طرف على الطاولة
 */
@Composable
fun UserRowLuxury(
    userTiles: List<DominoTile>,
    isUserTurn: Boolean,
    timeRemaining: Int,
    leftEnd: Int,
    rightEnd: Int,
    selectedChainEnd: SelectedChainEnd,
    onTileClick: (DominoTile) -> Unit,
    onGiftClick: () -> Unit,
    onChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2B1207).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFC7985D)),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // 1. صورة المستخدم من اليمين مع مؤقت الدور الـ 15 ثانية
            PlayerAvatarWithTimerRing(
                name = "أنت",
                emoji = "😎",
                avatarBg = Color(0xFF1976D2),
                isCurrentTurn = isUserTurn,
                timeRemaining = timeRemaining
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 2. أيقونتان منفصلتان جنباً إلى جنب: أيقونة الهدايا (🎁) وأيقونة الدردشة (💬)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // أيقونة الهدايا المنفصلة 🎁
                IconButton(
                    onClick = onGiftClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3E1909))
                        .border(1.2.dp, Color(0xFFD4AF37), CircleShape)
                        .testTag("domino_gift_button")
                ) {
                    Text(text = "🎁", fontSize = 16.sp)
                }

                // أيقونة الدردشة المنفصلة 💬
                IconButton(
                    onClick = onChatClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3E1909))
                        .border(1.2.dp, Color(0xFFD4AF37), CircleShape)
                        .testTag("domino_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "الدردشة",
                        tint = Color(0xFFF7E2C6),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 3. قطع المستخدم مكشوفة (Face-up) في شريط تمرير سلس مع إبراز وتحديد القطع المتوافقة
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                userTiles.forEach { tile ->
                    // تحديد بصري واضح (Highlight) إذا تم الضغط على طرف في الطاولة وتوافق الحجر معه
                    val isHighlighted = isUserTurn && when (selectedChainEnd) {
                        SelectedChainEnd.LEFT -> tile.left == leftEnd || tile.right == leftEnd
                        SelectedChainEnd.RIGHT -> tile.left == rightEnd || tile.right == rightEnd
                        SelectedChainEnd.NONE -> false
                    }

                    // تعتيم خفيف للقطع غير المتوافقة عند اختيار طرف محدد لتأكيد الـ Highlight
                    val isDimmed = isUserTurn && selectedChainEnd != SelectedChainEnd.NONE && !isHighlighted

                    val isPlayable = isUserTurn && (
                        tile.left == leftEnd || tile.right == leftEnd ||
                        tile.left == rightEnd || tile.right == rightEnd
                    )

                    StandingUserTile2P(
                        tile = tile,
                        isPlayable = isPlayable,
                        isHighlighted = isHighlighted,
                        isDimmed = isDimmed,
                        onClick = { onTileClick(tile) }
                    )
                }
            }
        }
    }
}

/**
 * حجر الدومينو المكشوف للمستخدم:
 * خلفية عاجية/كريمية دافئة، حواف ذهبية وبنية، نقاط سوداء كلاسيكية بارزة (Embossed)،
 * مع مسمار ذهبي في المنتصف وتحديد بصري واضح (Highlight) عند توافق الحجر مع الطرف المختار
 */
@Composable
fun StandingUserTile2P(
    tile: DominoTile,
    isPlayable: Boolean,
    isHighlighted: Boolean = false,
    isDimmed: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isHighlighted -> Color(0xFFFFD700) // تحديد بصري ذهبي ساطع (Highlight)
        isPlayable -> Color(0xFFD4AF37)
        else -> Color(0xFF594331)
    }
    val borderWidth = when {
        isHighlighted -> 3.dp
        isPlayable -> 2.dp
        else -> 1.dp
    }
    val elevation = when {
        isHighlighted -> 14.dp
        isPlayable -> 7.dp
        else -> 3.dp
    }

    // حركة رفع لأعلى عند التحديد البصري (Highlight)
    val yOffset = if (isHighlighted) (-10).dp else 0.dp
    val alpha = if (isDimmed) 0.42f else 1f

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFFAF6EB), // عاجي كريمي دافئ
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        shadowElevation = elevation,
        modifier = modifier
            .offset(y = yOffset)
            .graphicsLayer { this.alpha = alpha }
            .size(width = 38.dp, height = 74.dp)
            .testTag("user_standing_tile_${tile.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isHighlighted) {
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFFDE7), Color(0xFFFFF8E1), Color(0xFFEADBAC))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFFDF8), Color(0xFFECE3CA))
                        )
                    }
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // الجزء العلوي
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DominoPipsCanvas2P(pips = tile.left)
                }

                // الخط الفاصل مع المسمار الذهبي في المنتصف
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0xFF332213)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isHighlighted) 5.dp else 4.dp)
                            .clip(CircleShape)
                            .background(if (isHighlighted) Color(0xFFFFD700) else Color(0xFFD4AF37))
                    )
                }

                // الجزء السفلي
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DominoPipsCanvas2P(pips = tile.right)
                }
            }

            // مؤشر متوهج أعلى الحجر عند التحديد (Highlight)
            if (isHighlighted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD700))
                )
            }
        }
    }
}

/**
 * رسم نقاط الدومينو الكلاسيكية بتأثير بروز خفيف (Embossed Effect)
 * وتصميم متجاوب يتكيف تماماً مع الحجم المصغر تلقائياً للحفاظ على الوضوح والقراءة
 */
@Composable
fun DominoPipsCanvas2P(pips: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize().padding(1.dp)) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)

        // النصف قطر يتناسب مع أبعاد الحاوية بدقة، مع حد أدنى لتبقى النقاط واضحة ومقروءة ومتباعدة
        val radius = (minDim * 0.125f).coerceIn(1.1.dp.toPx(), 2.8.dp.toPx())
        val dotColor = Color(0xFF14100D) // أسود كلاسيكي
        val highlightColor = Color(0x66FFFFFF) // تأثير البروز العلوي
        val shadowColor = Color(0x88000000)    // تأثير الظل السفلي

        val pL = w * 0.25f
        val pR = w * 0.75f
        val pC = w * 0.50f
        val pT = h * 0.25f
        val pB = h * 0.75f
        val pM = h * 0.50f

        val shouldEmboss = radius >= 1.6.dp.toPx()

        fun drawEmbossedDot(x: Float, y: Float) {
            if (shouldEmboss) {
                // ظل سفلي خفيف لإبراز النقطة في الأحجام المتوسطة والكبيرة
                drawCircle(color = shadowColor, radius = radius + 0.4.dp.toPx(), center = Offset(x, y + 0.4.dp.toPx()))
            }
            // النقطة السوداء الرئيسية عالية التباين
            drawCircle(color = dotColor, radius = radius, center = Offset(x, y))
            if (shouldEmboss) {
                // إضاءة علوية خفيفة للبروز
                drawCircle(color = highlightColor, radius = radius * 0.45f, center = Offset(x - 0.3.dp.toPx(), y - 0.3.dp.toPx()))
            }
        }

        when (pips) {
            1 -> drawEmbossedDot(pC, pM)
            2 -> {
                drawEmbossedDot(pL, pT)
                drawEmbossedDot(pR, pB)
            }
            3 -> {
                drawEmbossedDot(pL, pT)
                drawEmbossedDot(pC, pM)
                drawEmbossedDot(pR, pB)
            }
            4 -> {
                drawEmbossedDot(pL, pT)
                drawEmbossedDot(pR, pT)
                drawEmbossedDot(pL, pB)
                drawEmbossedDot(pR, pB)
            }
            5 -> {
                drawEmbossedDot(pL, pT)
                drawEmbossedDot(pR, pT)
                drawEmbossedDot(pC, pM)
                drawEmbossedDot(pL, pB)
                drawEmbossedDot(pR, pB)
            }
            6 -> {
                drawEmbossedDot(pL, pT)
                drawEmbossedDot(pR, pT)
                drawEmbossedDot(pL, pM)
                drawEmbossedDot(pR, pM)
                drawEmbossedDot(pL, pB)
                drawEmbossedDot(pR, pB)
            }
        }
    }
}

/**
 * حجر الدومينو الموضوع في منطقة اللعب المركزية بالطاولة:
 * يدعم التصغير التلقائي المتناسب (scale) مع الحفاظ على التباين والعمق والتحديد
 */
@Composable
fun ClassicDominoTileView2P(
    tile: DominoTile,
    orientation: TileOrientation,
    isLeftEnd: Boolean,
    isRightEnd: Boolean,
    isSelected: Boolean = false,
    isPlayableEnd: Boolean = false,
    scale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val isVertical = orientation == TileOrientation.VERTICAL
    val width = (if (isVertical) 33.dp else 58.dp) * scale
    val height = (if (isVertical) 58.dp else 33.dp) * scale

    val borderColor = when {
        isSelected -> Color(0xFFFFD700) // ذهبي ناصع للطرف المختار
        isPlayableEnd -> Color(0xFFE5C178) // إطار ذهبي للأطراف التي يملك اللاعب أحجاراً لها
        isLeftEnd || isRightEnd -> Color(0xFFC7985D)
        else -> Color(0xFF5A4330)
    }

    val borderWidth = (when {
        isSelected -> 2.6.dp
        isPlayableEnd -> 1.8.dp
        isLeftEnd || isRightEnd -> 1.4.dp
        else -> 0.9.dp
    } * scale).coerceAtLeast(0.7.dp)

    val elevation = (when {
        isSelected -> 10.dp
        isPlayableEnd -> 6.dp
        else -> 4.dp
    } * scale).coerceAtLeast(1.dp)

    val cornerRadius = (4.dp * scale).coerceAtLeast(1.5.dp)

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = if (isSelected) Color(0xFFFFFDF5) else Color(0xFFFAF6EB),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        shadowElevation = elevation,
        modifier = modifier.size(width = width, height = height)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isVertical) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DominoPipsCanvas2P(pips = tile.left)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((1.5.dp * scale).coerceAtLeast(0.6.dp))
                            .background(Color(0xFF332213)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(((if (isSelected) 4.dp else 3.dp) * scale).coerceAtLeast(1.5.dp))
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFFFD700) else Color(0xFFD4AF37))
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DominoPipsCanvas2P(pips = tile.right)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        DominoPipsCanvas2P(pips = tile.left)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width((1.5.dp * scale).coerceAtLeast(0.6.dp))
                            .background(Color(0xFF332213)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(((if (isSelected) 4.dp else 3.dp) * scale).coerceAtLeast(1.5.dp))
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFFFD700) else Color(0xFFD4AF37))
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        DominoPipsCanvas2P(pips = tile.right)
                    }
                }
            }

            // مؤشر ذهبي صغير عند اختيار الطرف
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding((2.dp * scale).coerceAtLeast(1.dp))
                        .size((5.dp * scale).coerceAtLeast(2.5.dp))
                        .clip(CircleShape)
                        .background(Color(0xFFFFD700))
                )
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
 * نافذة إرسال الهدايا للخصم (الهدية 🎁)
 */
@Composable
fun DominoSendGiftDialog(
    opponentName: String,
    onSendGift: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val gifts = listOf(
        Pair("فنجان قهوة ☕", 10),
        Pair("باقة ورد 💐", 25),
        Pair("تاج ذهبي 👑", 50),
        Pair("ساعة فاخرة ⌚", 75),
        Pair("سيارة رياضية 🏎️", 100)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎁", fontSize = 18.sp)
                Text("إرسال هدية إلى $opponentName", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "اختر هدية لإرسالها أثناء اللعب:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                gifts.forEach { (gift, price) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSendGift(gift) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = gift, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFD4AF37).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$price نقطة",
                                    color = Color(0xFFB8860B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * نافذة النتيجة النهائية (الفوز عند 100/100 نقطة بالضبط)
 */
@Composable
fun DominoFinalVictoryDialog(
    score: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "👑🏆👑", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "مبروك الفوز الساحق!",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = Color(0xFFD4AF37)
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "لقد وصلت إلى رصيد $score/100 نقطة وأنهيت المباراة بنجاح تام!",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F472A),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFD4AF37)),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = "+250 نقطة مكافأة الفوز ⚡",
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                Text("جولة جديدة 🔄")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onExit) {
                Text("خروج للقائمة")
            }
        }
    )
}

/**
 * نافذة إعدادات الطاولة
 */
@Composable
fun DominoSettingsDialog2P(
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    onVibrationToggle: (Boolean) -> Unit,
    onExitGame: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إعدادات طاولة الدومينو ⚙️", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("المؤثرات الصوتية (طقة الخشب)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("صوت وضع الأحجار الكلاسيكي", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = soundEnabled, onCheckedChange = onSoundToggle)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الاهتزاز اللمسي (Haptic)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("نبضة اهتزاز خفيفة عند وضع الحجر", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = vibrationEnabled, onCheckedChange = onVibrationToggle)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Button(
                    onClick = onExitGame,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("الاستسلام ومغادرة الجولة", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("تم") }
        }
    )
}

/**
 * نافذة تأكيد الخروج من الطاولة
 */
@Composable
fun DominoExitConfirmDialog2P(
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مغادرة طاولة الدومينو", fontWeight = FontWeight.Bold) },
        text = { Text("هل ترغب في مغادرة الجولة الحالية؟ ستُحسب الجولة انسحاباً.") },
        confirmButton = {
            Button(
                onClick = onConfirmExit,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("نعم، مغادرة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("متابعة اللعب")
            }
        }
    )
}

/**
 * نافذة الدردشة السريعة
 */
@Composable
fun DominoQuickChatDialog2P(
    onDismiss: () -> Unit,
    onSelectPhrase: (String) -> Unit
) {
    val phrases = listOf("لعبة موفقة! 🀄", "حظاً أوفر في الجولة القادمة! 🔥", "لعبة قوية وذكية! 👏", "أغلقت الطاولة! 🎯", "باقي لي قطعة واحدة! 😉", "سحب سريع! ⚡")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الدردشة السريعة 💬", fontWeight = FontWeight.Bold) },
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
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
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

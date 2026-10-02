package com.example.ui

import android.view.LayoutInflater
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.R

@OptIn(UnstableApi::class)
@Composable
fun GiftVideoOverlay(
    @RawRes videoRes: Int,
    modifier: Modifier = Modifier,
    fadeFraction: Float = 0.18f,
    volume: Float = 0f,
    onFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentOnFinished by rememberUpdatedState(onFinished)

    val player = remember(videoRes) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(videoRes)))
            repeatMode = Player.REPEAT_MODE_OFF
            this.volume = volume
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) currentOnFinished()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val f = fadeFraction
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        f to Color.Black,
                        1f - f to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        f to Color.Black,
                        1f - f to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(R.layout.view_post_player, null) as PlayerView)
                    .also { it.player = player }
            }
        )
    }
}

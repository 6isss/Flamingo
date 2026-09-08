package yos.music.player.ui.widgets.effects

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yos.music.player.code.utils.others.BitmapResolver
import yos.music.player.data.libraries.SettingsLibrary.NowplayingBackgroundEffect
import yos.music.player.ui.pages.NowPlayingPage
import yos.music.player.ui.widgets.basic.YosWrapper

@Composable
fun YosFloatingLight(
    modifier: Modifier,
    album: () -> Uri?,
    isPlaying: () -> Boolean,
    nowPage: () -> String,
    showMiniPlayer: () -> Boolean
) {
    // Keep the last resolved artwork while the next one is decoding, so the
    // background never flashes back to the bare page behind the player.
    val drawable = remember {
        mutableStateOf<Drawable?>(null)
    }

    val context = LocalContext.current
    val imageLoader = remember(context) { ImageLoader(context) }
    YosWrapper {
        LaunchedEffect(album()) {
            val albumUri = album() ?: return@LaunchedEffect
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(albumUri)
                    .build()
                val thisBitmap = imageLoader.execute(request).drawable?.toBitmap()?.run {
                    BitmapResolver.bitmapCompress(this)
                }
                if (thisBitmap != null) {
                    drawable.value = imageResolve(
                        thisBitmap
                    ).toDrawable(context.resources)
                    thisBitmap.recycle()
                }
            }
        }
    }

    YosWrapper {
        val lossEffect = remember("YosFloatingLight_lossEffect") {
            derivedStateOf {
                nowPage() != NowPlayingPage.Lyric
            }
        }

        val useBackground = remember("YosFloatingLight_useBackground") {
            derivedStateOf {
                album() == null && drawable.value == null
            }
        }

        if (NowplayingBackgroundEffect) {
            YosWrapper {
                // The background transition follows only resolved artwork. It no
                // longer restarts when playback briefly toggles during next/prev.
                Crossfade(
                    targetState = drawable.value,
                    animationSpec = tween(
                        durationMillis = 900,
                        easing = FastOutSlowInEasing
                    ),
                    label = "PlayerBackgroundArtwork"
                ) { backgroundDrawable ->
                    AsyncImage(
                        model = backgroundDrawable,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = modifier.drawWithCache {
                            onDrawBehind {
                                if (useBackground.value) drawRect(Color.Black)
                            }
                        }
                    )
                }
            }
        } else {
            YosWrapper {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(data = drawable.value)
                        .crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithCache {
                            onDrawBehind {
                                if (useBackground.value) {
                                    drawRect(Color.Black)
                                }
                            }
                        }
                )
            }
        }

        YosWrapper {
            val alpha = animateFloatAsState(
                targetValue = if (lossEffect.value) 0.618f else 0f, animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
            AsyncImage(
                model = ImageRequest.Builder(context).data(data = drawable.value)
                    .crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        this.alpha = alpha.value
                    },
                colorFilter = ColorFilter.tint(Color(0x33000000), BlendMode.Overlay)
            )
        }
    }
}

fun imageResolve(image: Bitmap, moreLight: Boolean = false): Bitmap {
    var resizedBitmap = image.copy(Bitmap.Config.ARGB_8888, true)
    resizedBitmap.applyCanvas {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        paint.isDither = true

        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(3f)

        paint.colorFilter = ColorMatrixColorFilter(saturationMatrix)
        drawBitmap(resizedBitmap, 0f, 0f, paint)

        if (moreLight) {
            drawColor((0x1AFFFFFF).toInt())
            drawColor((0xFFFFFFFF).toInt(), PorterDuff.Mode.OVERLAY)
            drawColor((0x52FFFFFF).toInt())
            drawColor((0xBFFFFFFF).toInt(), PorterDuff.Mode.OVERLAY)
        } else {
            drawColor((0x33000000).toInt(), PorterDuff.Mode.OVERLAY)
            drawColor((0x40000000).toInt())
        }
    }
    resizedBitmap = BitmapResolver.blurBitmap(resizedBitmap, 25)
    return resizedBitmap
}

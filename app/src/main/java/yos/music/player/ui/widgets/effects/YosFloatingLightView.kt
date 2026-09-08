package yos.music.player.ui.widgets.effects

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yos.music.player.code.utils.others.BitmapResolver
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
    // Two fixed layers: the artwork that is on screen, and the one fading in.
    // Nothing is decoded while the fade runs, so the transition stays smooth.
    val currentImage = remember { mutableStateOf<ImageBitmap?>(null) }
    val previousImage = remember { mutableStateOf<ImageBitmap?>(null) }
    val fade = remember { Animatable(1f) }

    val context = LocalContext.current
    val imageLoader = remember(context) { ImageLoader(context) }
    YosWrapper {
        LaunchedEffect(album()) {
            val albumUri = album() ?: return@LaunchedEffect
            val prepared = withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(albumUri)
                    .allowHardware(false)
                    .build()
                val thisBitmap = imageLoader.execute(request).drawable?.toBitmap()?.run {
                    BitmapResolver.bitmapCompress(this)
                }
                if (thisBitmap != null) {
                    val resolved = imageResolve(thisBitmap)
                    if (resolved != thisBitmap) thisBitmap.recycle()
                    resolved.asImageBitmap()
                } else null
            } ?: return@LaunchedEffect

            previousImage.value = currentImage.value
            currentImage.value = prepared
            if (previousImage.value == null) {
                fade.snapTo(1f)
            } else {
                fade.snapTo(0f)
                fade.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                )
                previousImage.value = null
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
                album() == null && currentImage.value == null
            }
        }

        YosWrapper {
            Box(modifier = modifier.drawWithCache {
                onDrawBehind {
                    if (useBackground.value) drawRect(Color.Black)
                }
            }) {
                previousImage.value?.let { previous ->
                    Image(
                        bitmap = previous,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                currentImage.value?.let { current ->
                    Image(
                        bitmap = current,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { this.alpha = fade.value }
                    )
                }
            }
        }

        YosWrapper {
            val alpha = animateFloatAsState(
                targetValue = if (lossEffect.value) 0.618f else 0f, animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                ),
                label = "YosFloatingLight_overlayAlpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        this.alpha = alpha.value
                    }
            ) {
                previousImage.value?.let { previous ->
                    Image(
                        bitmap = previous,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        colorFilter = ColorFilter.tint(Color(0x33000000), BlendMode.Overlay)
                    )
                }
                currentImage.value?.let { current ->
                    Image(
                        bitmap = current,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { this.alpha = fade.value },
                        colorFilter = ColorFilter.tint(Color(0x33000000), BlendMode.Overlay)
                    )
                }
            }
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

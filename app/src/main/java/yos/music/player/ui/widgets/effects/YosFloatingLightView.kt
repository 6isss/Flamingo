package yos.music.player.ui.widgets.effects

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yos.music.player.code.utils.others.BitmapResolver
import yos.music.player.ui.widgets.basic.YosWrapper


@Composable
fun YosFloatingLight(
    modifier: Modifier,
    album: () -> Uri?,
    isPlaying: () -> Boolean,
    nowPage: () -> String,
    showMiniPlayer: () -> Boolean,
    revealProgress: () -> Float
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
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(14.dp * revealProgress(), BlurredEdgeTreatment.Unbounded)
                            .graphicsLayer {
                                // Overscale so no softened edge of the blurred artwork
                                // is visible: the glow runs past every screen edge.
                                scaleX = 1.2f
                                scaleY = 1.2f
                                alpha = (1f - fade.value) * (0.72f + (0.28f * revealProgress()))
                            }
                    )
                }
                currentImage.value?.let { current ->
                    Image(
                        bitmap = current,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(14.dp * revealProgress(), BlurredEdgeTreatment.Unbounded)
                            .graphicsLayer {
                                scaleX = 1.2f
                                scaleY = 1.2f
                                this.alpha = fade.value * (0.72f + (0.28f * revealProgress()))
                            }
                    )
                }
            }
        }

        // The darkened overlay copy used to sit on top of the blurred artwork and
        // read as a vignette / edge shadow. The ambient glow now reaches the edges.
    }
}

/**
 * Average perceived brightness of a bitmap, sampled on a coarse grid so it stays cheap.
 */
private fun averageLuminance(bitmap: Bitmap): Float {
    val steps = 16
    var total = 0f
    var samples = 0
    for (y in 0 until steps) {
        for (x in 0 until steps) {
            val px = bitmap.getPixel(
                (bitmap.width - 1) * x / (steps - 1),
                (bitmap.height - 1) * y / (steps - 1)
            )
            val r = ((px shr 16) and 0xFF) / 255f
            val g = ((px shr 8) and 0xFF) / 255f
            val b = (px and 0xFF) / 255f
            total += (0.299f * r) + (0.587f * g) + (0.114f * b)
            samples++
        }
    }
    return if (samples == 0) 0f else total / samples
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
        }
    }
    resizedBitmap = BitmapResolver.blurBitmap(resizedBitmap, 25)

    // Very bright artwork washes out the controls, so dim it in proportion to how
    // bright it actually is. Dark artwork is left untouched.
    if (!moreLight) {
        val luminance = averageLuminance(resizedBitmap)
        if (luminance > 0.55f) {
            val dim = (((luminance - 0.55f) / 0.45f).coerceIn(0f, 1f) * 0.45f)
            val alpha = (dim * 255f).toInt().coerceIn(0, 255)
            resizedBitmap.applyCanvas {
                drawColor((alpha shl 24))
            }
        }
    }
    return resizedBitmap
}

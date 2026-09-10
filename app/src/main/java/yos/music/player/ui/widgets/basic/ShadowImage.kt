package yos.music.player.ui.widgets.basic

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import yos.music.player.R
import yos.music.player.ui.theme.YosRoundedCornerShape
import yos.music.player.ui.widgets.effects.ShadowType
import yos.music.player.ui.widgets.effects.dropShadow

@Stable
enum class ImageQuality {
    RAW, LOW, HIGH
}

private fun getSizeFromQuality(quality: ImageQuality): Int {
    return when (quality) {
        ImageQuality.RAW -> 0
        ImageQuality.LOW -> 128
        ImageQuality.HIGH -> 400
    }
}

@Composable
fun ShadowImage(
    dataLambda: () -> Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shadowAlpha: Float = 0.23f,
    shadowType: ShadowType = ShadowType.Large,
    shadowOverlay: Boolean = false,
    cornerRadius: Dp = 10.dp,
    imageQuality: ImageQuality
) = YosWrapper {
    val shape = YosRoundedCornerShape(cornerRadius)
    val url = dataLambda()
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(data = url).crossfade(true)
            .error(R.drawable.placeholder_music_default_artwork)
            .placeholder(R.drawable.placeholder_music_default_artwork)
            .fallback(R.drawable.placeholder_music_default_artwork)
            .placeholderMemoryCacheKey(url.toString())
            .diskCacheKey(url.toString())
            .allowHardware(true)
            .crossfade(true)
            .apply {
                if (imageQuality != ImageQuality.RAW) {
                    val size = getSizeFromQuality(imageQuality)
                    this.size(size)
                    if (imageQuality == ImageQuality.LOW) {
                        this.precision(Precision.INEXACT)
                    }
                }
            }
            .build(),
        contentDescription = contentDescription.toString(),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .dropShadow(shape, shadowAlpha, shadowType, shadowOverlay)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                clip = true
                this.shape = shape
            }

    )
}

@Composable
fun ShadowImageWithCache(
    dataLambda: () -> Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shadowAlpha: Float = 0.23f,
    shadowType: ShadowType = ShadowType.Large,
    shadowOverlay: Boolean = false,
    cornerRadius: Dp = 8.dp,
    imageQuality: ImageQuality,
    crossfade: Boolean = true,
    crossfadeDurationMillis: Int = 220,
    overlayContent: (@Composable BoxScope.() -> Unit)? = null
) = YosWrapper {
    val shape = YosRoundedCornerShape(cornerRadius)
    val url = dataLambda()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .dropShadow(shape, shadowAlpha, shadowType, shadowOverlay)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                clip = true
                this.shape = shape
            }
    ) {
        val request = ImageRequest.Builder(LocalContext.current).data(data = url)
                .error(R.drawable.placeholder_music_default_artwork)
                // No placeholder frame: the cover fades in from the tile surface.

                .fallback(R.drawable.placeholder_music_default_artwork)
                .placeholderMemoryCacheKey(url.toString())
                .memoryCacheKey(url.toString())
                .allowHardware(true)
                .crossfade(if (crossfade) crossfadeDurationMillis else 0)
                .apply {
                    if (imageQuality != ImageQuality.RAW) {
                        val size = getSizeFromQuality(imageQuality)
                        this.size(size)
                        if (imageQuality == ImageQuality.LOW) {
                            this.precision(Precision.INEXACT)
                        }
                    } else {
                        this.precision(Precision.EXACT)
                        this.size(coil.size.Size.ORIGINAL)
                    }
                }
                .build()

        if (crossfade) {
            AsyncImage(
                model = request,
                contentDescription = contentDescription.toString(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val painter = rememberAsyncImagePainter(request)
            val state = painter.state
            var retainedPainter by remember { mutableStateOf<Painter?>(null) }
            LaunchedEffect(state) {
                if (state is AsyncImagePainter.State.Success) {
                    retainedPainter = state.painter
                }
            }
            Image(
                painter = retainedPainter ?: painter,
                contentDescription = contentDescription.toString(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        overlayContent?.invoke(this)
    }
}

@file:Suppress("DEPRECATION")

package yos.music.player.ui.pages

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.ripple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderPositions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.blankj.utilcode.util.TimeUtils
import com.google.accompanist.insets.navigationBarsHeight
import com.google.accompanist.insets.statusBarsHeight
import com.google.accompanist.insets.statusBarsPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yos.music.player.R
import yos.music.player.code.MediaController
import yos.music.player.code.MediaController.mediaControl
import yos.music.player.code.MediaController.musicPlaying
import yos.music.player.code.MediaController.playingMusicList
import yos.music.player.code.MediaController.queueShuffleEnabled
import yos.music.player.code.SystemMediaControlResolver
import yos.music.player.code.VolumeChangeReceiver
import yos.music.player.code.YosPlaybackService
import yos.music.player.code.utils.lrc.YosMediaEvent
import yos.music.player.code.utils.lrc.YosUIConfig
import yos.music.player.code.utils.others.Vibrator
import yos.music.player.code.utils.player.FadeExo.fadePause
import yos.music.player.code.utils.player.FadeExo.fadePlay
import yos.music.player.data.libraries.FavPlayListLibrary
import yos.music.player.data.libraries.SettingsLibrary
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.data.libraries.artistsList
import yos.music.player.data.libraries.artistsName
import yos.music.player.data.libraries.defaultArtistsName
import yos.music.player.data.libraries.defaultTitle
import yos.music.player.data.models.MainViewModel
import yos.music.player.data.models.MediaViewModel
import yos.music.player.data.objects.MediaViewModelObject
import yos.music.player.data.objects.LibraryObject
import yos.music.player.ui.pages.NowPlayingPage.Album
import yos.music.player.ui.pages.NowPlayingPage.Lyric
import yos.music.player.ui.pages.NowPlayingPage.PlayingList
import yos.music.player.ui.theme.YosRoundedCornerShape
import yos.music.player.ui.UI
import yos.music.player.ui.markNextNavigationFromNowPlaying
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.YosLyricView
import yos.music.player.ui.widgets.effects.YosFloatingLight
import yos.music.player.ui.widgets.audio.MusicQualityIndicator
import coil.compose.AsyncImage
import coil.request.ImageRequest
import yos.music.player.ui.widgets.basic.ActionItem
import yos.music.player.ui.widgets.basic.SheetAnimatedContent
import yos.music.player.ui.widgets.basic.SheetNavigationBackward
import yos.music.player.ui.widgets.basic.SheetNavigationForward
import yos.music.player.ui.widgets.basic.ActionSheetBody
import yos.music.player.ui.widgets.basic.AnimatedAlbumCoverState
import yos.music.player.ui.widgets.basic.AnimatedAlbumCoverOverlay
import yos.music.player.ui.widgets.basic.ImageQuality
import yos.music.player.ui.widgets.basic.YosBottomSheetDialog
import yos.music.player.ui.widgets.basic.rememberAnimatedAlbumCoverState
import yos.music.player.ui.widgets.playlist.PlayListPickerContent
import yos.music.player.ui.widgets.sleeptimer.SleepTimerContent
import yos.music.player.code.SleepTimer
import yos.music.player.code.SleepTimerState
import yos.music.player.ui.widgets.basic.ShadowImageWithCache
import yos.music.player.ui.widgets.basic.YosWrapper
import yos.music.player.ui.widgets.effects.ShadowType
import yos.music.player.ui.widgets.effects.overlayEffect
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.abs


@Stable
object NowPlayingPage {
    const val Album = "Album"
    const val PlayingList = "PlayingList"
    const val Lyric = "Lyric"
}

private const val AnimDurationMillis = 300
private const val ShareAlbumKey = "album"
private val QueueRowHeight = 64.dp
private val QueueDraggingItemShape = RoundedCornerShape(0.dp)

private data class QueueReorderTarget(
    val nextInQueue: Boolean,
    val index: Int,
)

private fun resolveQueueReorderTarget(
    lazyListIndex: Int,
    nextInQueueSize: Int,
    upNextSize: Int,
): QueueReorderTarget? {
    var cursor = 1

    if (nextInQueueSize > 0) {
        cursor += 1

        if (lazyListIndex in cursor until cursor + nextInQueueSize) {
            return QueueReorderTarget(true, lazyListIndex - cursor)
        }

        cursor += nextInQueueSize
    }

    if (upNextSize > 0) {
        cursor += 1

        if (lazyListIndex in cursor until cursor + upNextSize) {
            return QueueReorderTarget(false, lazyListIndex - cursor)
        }
    }

    return null
}

private fun queueItemKey(
    sectionKey: String,
    music: YosMediaItem,
    index: Int,
    sectionItems: List<YosMediaItem>,
): String {
    val duplicateOrdinal = sectionItems
        .take(index + 1)
        .count { it.uri == music.uri && it.mediaId == music.mediaId }

    return "$sectionKey:${music.uri}:${music.mediaId}:$duplicateOrdinal"
}

/*
private val MaterialFadeInTransitionSpec
    get() = SharedElementsTransitionSpec(
        pathMotionFactory = LinearMotionFactory,
        durationMillis = AnimDurationMillis,
        fadeMode = FadeMode.In,
        easing = EaseOutQuart
    )

private val MaterialFadeOutTransitionSpec
    get() = SharedElementsTransitionSpec(
        pathMotionFactory = LinearMotionFactory,
        durationMillis = AnimDurationMillis,
        fadeMode = FadeMode.Out,
        easing = EaseOutQuart
    )
*/

@ExperimentalSharedTransitionApi
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun NowPlaying(
    mainViewModel: MainViewModel,
    mediaViewModel: MediaViewModel,
    navController: NavController,
    onMinimizeNowPlaying: suspend () -> Unit,
    isPlayingStatusLambda: () -> Boolean,
    isPlayingOnChanged: (Boolean) -> Unit,
    nowPageLambda: () -> String,
    showNowPlaying: () -> Boolean,
    showMiniPlayer: () -> Boolean,
    playerRevealProgress: () -> Float,
    nowPageOnChanged: (String) -> Unit
) =
    Surface(
        modifier = Modifier.fillMaxSize(),
        contentColor = Color.White,
        color = Color.Transparent
    ) {
        val context = LocalContext.current

        val lrcEntries: MutableState<List<List<Pair<Float, String>>>> =
            MediaViewModelObject.lrcEntries
        val bitmap: MutableState<Uri?> = MediaViewModelObject.bitmap
        val lyricLineEndTimes = MediaViewModelObject.lyricLineEndTimes
        val lyricLineTransliterations = MediaViewModelObject.lyricLineTransliterations
        val lyricLineSubtitles = MediaViewModelObject.lyricLineSubtitles
        val isTtmlLyrics = MediaViewModelObject.isTtmlLyrics

        val thisMusicPlaying = remember("NowPlaying_thisMusicPlaying") {
            musicPlaying
        }
        val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
        val animatedAlbumCoverState = rememberAnimatedAlbumCoverState(
            music = thisMusicPlaying.value,
            isPlaying = isPlayingStatusLambda(),
            active = nowPageLambda() == Album &&
                showNowPlaying() &&
                lifecycleState.value.isAtLeast(Lifecycle.State.STARTED)
        )

        val lastClickTime = rememberSaveable(key = "NowPlaying_lastClickTime") {
            mutableLongStateOf(0L)
        }

        val showControl = rememberSaveable(key = "NowPlaying_showControl") {
            mutableStateOf(true)
        }

        val translation = rememberSaveable(key = "NowPlaying_translation") {
            mutableStateOf(SettingsLibrary.NowPlayingTranslation)
        }

        val shuffleModeEnabled = rememberSaveable(key = "NowPlaying_shuffleModeEnabled") {
            mutableStateOf(queueShuffleEnabled.value)
        }
        val repeatMode = rememberSaveable(key = "NowPlaying_repeatMode") {
            mutableIntStateOf(mediaControl?.repeatMode ?: REPEAT_MODE_OFF)
        }

        /*val nowPage = rememberSaveable(key = "NowPlaying_nowPage") {
            MainViewModelObject.nowPage
        }*/

        // 触摸超时
        YosWrapper {
            LaunchedEffect(showControl.value, nowPageLambda(), lastClickTime.longValue) {
                if (nowPageLambda() != Lyric && !showControl.value) {
                    showControl.value = true
                }
                if (showControl.value) {
                    val time = 2500L
                    delay(time)
                    withContext(Dispatchers.Main) {
                        if (TimeUtils.getNowMills() - lastClickTime.longValue >= time && nowPageLambda() == Lyric) {
                            showControl.value = false
                        }
                    }
                }
            }
        }


        // 背景流光
        YosWrapper {
            /*BlendBackgroundView(
        bitmapLambda = { bitmap.value },
        isPlayingLambda = { isPlaying.value },
        nowPage = { nowPage.value }
    )*/
            YosFloatingLight(
                album = { bitmap.value },
                isPlaying = isPlayingStatusLambda,
                modifier = Modifier.fillMaxSize(),
                nowPage = { nowPageLambda() },
                showMiniPlayer = showMiniPlayer,
                revealProgress = playerRevealProgress
            )
        }


        // 实际显示区
        YosWrapper {
            /*
        val controlAlpha = animateFloatAsState(
            targetValue = if (showControl.value) 1f else 0f,
            tween(200)
        )

        val buttonEnabled = remember("NowPlaying_buttonEnabled") {
            derivedStateOf { controlAlpha.value != 0f }
        }

        val translationButtonEnabled = remember("NowPlaying_translationButtonEnabled") {
            derivedStateOf { buttonEnabled.value && alpha.value != 0f }
        }*/

            val scope = rememberCoroutineScope()

            val alphaAnim = remember { Animatable(0f) }

            YosWrapper {
                LaunchedEffect(nowPageLambda()) {
                    val targetAlpha = if (nowPageLambda() == Lyric) 1f else 0f
                    scope.launch {
                        alphaAnim.animateTo(targetAlpha)
                    }
                }
            }

            val lyricSecondaryTextAvailable = remember("NowPlaying_lyricSecondaryTextAvailable") {
                derivedStateOf {
                    if (isTtmlLyrics.value) {
                        lyricLineTransliterations.any { !it.isNullOrBlank() } ||
                                lyricLineSubtitles.any { !it.isNullOrBlank() }
                    } else {
                        lrcEntries.value.any { it.lastOrNull()?.second?.isNotBlank() == true }
                    }
                }
            }

            val translationButtonEnabled = remember("NowPlaying_translationButtonEnabled") {
                derivedStateOf {
                    showControl.value && alphaAnim.value != 0f && lyricSecondaryTextAvailable.value
                }
            }

            // 歌词
            YosWrapper {

                Column(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy =
                                CompositingStrategy.ModulateAlpha
                            this.alpha = alphaAnim.value
                        }
                ) {
                    Lyric(
                        lrcEntries = { lrcEntries.value },
                        lineEndTimes = { lyricLineEndTimes },
                        lineTransliterations = { lyricLineTransliterations },
                        lineSubtitles = { lyricLineSubtitles },
                        isTtmlLyrics = { isTtmlLyrics.value },
                        weightLambda = { showControl.value },
                        translationLambda = { translation.value },
                        onBackClick = {
                            showControl.value = true
                            lastClickTime.longValue =
                                TimeUtils.getNowMills()
                        },
                        mainViewModel = mainViewModel,
                        mediaViewModel = mediaViewModel
                    )
                }
            }

            // 这是小把手
            YosWrapper {
                Column(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 20.dp), contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .overlayEffect()
                                .size(
                                    width = 32.dp,
                                    height = 4.5.dp
                                )
                                .background(Color(0x4DFFFFFF), RoundedCornerShape(2.25.dp))
                                .clip(RoundedCornerShape(2.25.dp))
                        )
                    }
                }
            }

            // 主 View
            YosWrapper {
                SharedTransitionLayout {
                    Crossfade(
                        targetState = nowPageLambda(),
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = 22.dp)
                    ) {
                        //println("nowPage: ${nowPageLambda()}")
                        //println("nowPageIt: $it")
                        when (it) {
                            Album ->
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .clickable(enabled = false, onClick = {})
                                ) {
                                    YosWrapper {
                                        Column(Modifier.fillMaxHeight(0.595f)) {
                                            val isVisible = nowPageLambda() == Album

                                            Album(
                                                modifier = Modifier.sharedElementWithCallerManagedVisibility(
                                                    sharedContentState = rememberSharedContentState(
                                                        key = ShareAlbumKey
                                                    ),
                                                    visible = isVisible
                                                ),
                                                music = { thisMusicPlaying.value },
                                                animatedAlbumCoverState = animatedAlbumCoverState,
                                                isPlaying = isPlayingStatusLambda
                                            )
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 32.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Only the name and artist fade between
                                                // tracks; the buttons stay put.
                                                AnimatedContent(
                                                    targetState = thisMusicPlaying.value,
                                                    transitionSpec = {
                                                        fadeIn(tween(250, delayMillis = 250, easing = EaseInOut)) togetherWith
                                                                fadeOut(tween(250, easing = EaseInOut)) using SizeTransform(
                                                            clip = false,
                                                            sizeAnimationSpec = { _, _ -> snap() }
                                                        )
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(end = 15.dp)
                                                ) {
                                                    Column(
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .height(52.dp),
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        ScrollingSongTitle(
                                                            text = it?.title
                                                                ?: defaultTitle,
                                                            fontSize = 19.5.sp,
                                                            lineHeight = 26.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(26.dp)
                                                        )
                                                        val artistNavScope =
                                                            rememberCoroutineScope()
                                                        val firstArtist =
                                                            it?.artistsList.orEmpty()
                                                                .firstOrNull { name -> name.isNotBlank() }
                                                        ScrollingSongTitle(
                                                            text = it?.artistsName
                                                                ?: defaultArtistsName,
                                                            fontSize = 18.5.sp,
                                                            lineHeight = 26.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            color = Color.White.copy(alpha = 0.35f),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(26.dp)
                                                                .clickable(
                                                                    enabled = firstArtist != null,
                                                                    indication = null,
                                                                    interactionSource = remember { MutableInteractionSource() }
                                                                ) {
                                                                    val artistName =
                                                                        firstArtist ?: return@clickable
                                                                    LibraryObject.setTargetArtistName(
                                                                        artistName
                                                                    )
                                                                    LibraryObject.setArtistSongsSearchOnOpen(
                                                                        false
                                                                    )
                                                                    navController.markNextNavigationFromNowPlaying()
                                                                    artistNavScope.launch {
                                                                        onMinimizeNowPlaying()
                                                                    }
                                                                    navController.toUI(UI.ArtistInfo)
                                                                }
                                                        )
                                                    }
                                                }

                                                YosWrapper {
                                                    ActionButtonsRow(navController, onMinimizeNowPlaying) {
                                                        thisMusicPlaying.value
                                                    }
                                                }
                                            }

                                        }
                                    }
                                }

                            Lyric ->
                                Column(Modifier.fillMaxSize()) {
                                    YosWrapper {
                                        val isVisible = nowPageLambda() == Lyric
                                        PlayingBar(
                                            modifier = Modifier.sharedElementWithCallerManagedVisibility(
                                                sharedContentState = rememberSharedContentState(
                                                    key = ShareAlbumKey
                                                ),
                                                visible = isVisible
                                            ),
                                            navController = navController,
                                            onMinimizeNowPlaying = onMinimizeNowPlaying,
                                            albumUrlLambda = {
                                                thisMusicPlaying.value?.thumb
                                            },
                                            musicPlayingLambda = { thisMusicPlaying.value }) {
                                            nowPageOnChanged(Album)
                                        }
                                    }
                                }

                            PlayingList ->
                                YosWrapper {
                                    Column(
                                        Modifier
                                            .fillMaxSize()
                                            .clickable(enabled = false, onClick = {})
                                    ) {
                                        val isVisible = nowPageLambda() == PlayingList
                                        PlayingBar(
                                            modifier = Modifier.sharedElementWithCallerManagedVisibility(
                                                sharedContentState = rememberSharedContentState(
                                                    key = ShareAlbumKey
                                                ),
                                                visible = isVisible
                                            ),
                                            navController = navController,
                                            onMinimizeNowPlaying = onMinimizeNowPlaying,
                                            albumUrlLambda = {
                                                thisMusicPlaying.value?.thumb
                                            },
                                            musicPlayingLambda = { thisMusicPlaying.value }) {
                                            nowPageOnChanged(Album)
                                        }
                                    }
                                }
                        }
                    }
                }
            }

            YosWrapper {
                AnimatedVisibility(
                    visible = nowPageLambda() == PlayingList,
                    enter = fadeIn(tween(AnimDurationMillis)),
                    exit = fadeOut(tween(AnimDurationMillis)),
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 114.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .clickable(enabled = false, onClick = {})
                    ) {
                        PlayingList(
                            shuffleModeEnabledLambda = { shuffleModeEnabled.value },
                            shuffleModeOnChanged = { shuffleModeSet ->
                                shuffleModeEnabled.value = shuffleModeSet
                            },
                            repeatModeLambda = { repeatMode.intValue },
                            repeatModeOnChanged = { repeatModeSet ->
                                repeatMode.intValue = repeatModeSet
                            },
                            thisMusicPlayingLambda = { thisMusicPlaying.value }
                        )
                    }
                }
            }

            // 音乐控制
            YosWrapper {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding(), verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        Modifier
                            /*.fillMaxHeight(0.385f)*/
                            .fillMaxHeight(0.437f)
                            .fillMaxWidth()
                    ) {
                        YosWrapper {
                            if (showControl.value) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 40.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                //showControl.value = true
                                                /*lastClickTime.longValue =
                                                TimeUtils.getNowMills()*/
                                            })
                                )
                            }
                        }

                        YosWrapper {
                            Column(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                AnimatedVisibility(
                                    visible = showControl.value,
                                    enter = fadeIn() + expandVertically(
                                        expandFrom = Alignment.Top,
                                        initialHeight = { (it / 1.4).toInt() }),
                                    exit = fadeOut() + shrinkVertically(
                                        shrinkTowards = Alignment.Top,
                                        targetHeight = { (it / 1.4).toInt() })
                                ) {
                                    YosWrapper {
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 32.dp)
                                                .graphicsLayer {
                                                    compositingStrategy =
                                                        CompositingStrategy.ModulateAlpha
                                                    this.alpha = alphaAnim.value
                                                },
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            YosWrapper {
                                                Box(
                                                    modifier = Modifier
                                                        .overlayEffect()
                                                        .alpha(0.4f)
                                                        .clickable(
                                                            enabled = translationButtonEnabled.value,
                                                            onClick = {
                                                                Vibrator.click(context)
                                                                translation.value =
                                                                    !translation.value
                                                                showControl.value = true
                                                                lastClickTime.longValue =
                                                                    TimeUtils.getNowMills()
                                                                SettingsLibrary.NowPlayingTranslation =
                                                                    translation.value
                                                            },
                                                            indication = null,
                                                            interactionSource = remember { MutableInteractionSource() }),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AnimatedContent(
                                                        targetState = translation.value,
                                                        transitionSpec = {
                                                            fadeIn() togetherWith fadeOut()
                                                        }) {
                                                        if (it) {
                                                            Icon(
                                                                painterResource(id = R.drawable.ic_nowplaying_translateon),
                                                                contentDescription = null,
                                                                tint = Color.Unspecified,
                                                                modifier = Modifier
                                                                    .size(30.dp)
                                                            )
                                                        } else {
                                                            Icon(
                                                                painterResource(id = R.drawable.ic_nowplaying_translate),
                                                                contentDescription = null,
                                                                tint = Color.Unspecified,
                                                                modifier = Modifier
                                                                    .size(30.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    PlayerControl(
                                        isPlayingLambda = isPlayingStatusLambda,
                                        isPlayingOnChanged = isPlayingOnChanged,
                                        onPrevious = {
                                            // Past the first five seconds, "previous" restarts
                                            // the current track instead of skipping back.
                                            val position = mediaControl?.currentPosition ?: 0L
                                            if (position > 5000L) {
                                                mediaControl?.seekTo(0L)
                                            } else {
                                                mediaControl?.seekToPreviousMediaItem()
                                            }
                                            showControl.value = true
                                            lastClickTime.longValue = TimeUtils.getNowMills()
                                        },
                                        onStatus = { status ->
                                            if (status) {
                                                mediaControl?.fadePlay()
                                            } else {
                                                mediaControl?.fadePause()
                                            }
                                            showControl.value = true
                                            lastClickTime.longValue = TimeUtils.getNowMills()
                                        },
                                        onNext = {
                                            mediaControl?.seekToNextMediaItem()
                                            showControl.value = true
                                            lastClickTime.longValue = TimeUtils.getNowMills()
                                        },
                                        onSeek = { position ->
                                            mediaControl?.seekTo(position.toLong())
                                        },
                                        onLyrics = {
                                            if (nowPageLambda() == Lyric) {
                                                nowPageOnChanged(Album)
                                            } else {
                                                nowPageOnChanged(Lyric)
                                            }
                                        },
                                        onPlaylist = {
                                            if (nowPageLambda() == PlayingList) {
                                                nowPageOnChanged(Album)
                                            } else {
                                                nowPageOnChanged(PlayingList)
                                            }
                                        },
                                        nowPage = {
                                            nowPageLambda()
                                        },
                                        onSlider = {
                                            showControl.value = true
                                            lastClickTime.longValue = TimeUtils.getNowMills()
                                        },
                                        modifier = Modifier
                                            /*.graphicsLayer {
                                                compositingStrategy =
                                                    CompositingStrategy.Offscreen
                                                //this.alpha = controlAlpha.value
                                            }*/
                                            .padding(top = 52.dp),
                                        onWhile = {
                                            shuffleModeEnabled.value =
                                                queueShuffleEnabled.value
                                            repeatMode.intValue =
                                                mediaControl?.repeatMode ?: REPEAT_MODE_OFF
                                        })
                                }
                            }
                        }
                    }
                }
            }

        }
    }

/** True while the playback scrubber is being held, so the artwork can recede. */
private val scrubbingArtwork = mutableStateOf(false)

@Composable
private fun ColumnScope.Album(
    modifier: Modifier,
    music: () -> YosMediaItem?,
    animatedAlbumCoverState: AnimatedAlbumCoverState,
    isPlaying: () -> Boolean
) = Box(
    Modifier
        .weight(1f)
        .padding(top = 20.dp)
        .padding(horizontal = 15.dp)
        .padding(bottom = 33.dp),
    contentAlignment = Alignment.BottomCenter
) {
    // Playback reports a short paused state while a track changes. React instantly to a
    // real pause, but ignore a pause that lands right around a track switch.
    val settledPlaying = remember("Album_settledPlaying") { mutableStateOf(isPlaying()) }
    val lastTrackChange = remember("Album_lastTrackChange") { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        snapshotFlow { music()?.thumb }.collect {
            lastTrackChange.longValue = System.currentTimeMillis()
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { isPlaying() }.collectLatest { playing ->
            if (playing) {
                settledPlaying.value = true
            } else {
                val sinceSwitch = System.currentTimeMillis() - lastTrackChange.longValue
                if (sinceSwitch < 300L) {
                    // Likely a track-change hiccup; only honour it if it sticks.
                    delay(300L - sinceSwitch)
                }
                settledPlaying.value = false
            }
        }
    }

    // Upstream bounce: spring in on resume, ease-out tween on pause. Driven by the
    // settled state above so a track switch never triggers it.
    val springSpec: AnimationSpec<Float> = remember("Album_springSpec") {
        SpringSpec(stiffness = 300f, dampingRatio = 1f, visibilityThreshold = 0.001f)
    }
    val tweenSpec: AnimationSpec<Float> = remember("Album_tweenSpec") {
        TweenSpec(durationMillis = 350, easing = EaseOutCubic)
    }
    // While scrubbing the cover recedes all the way, like the paused state, and
    // glides back forward on release.
    val scrubbing = scrubbingArtwork.value
    val target = if (!settledPlaying.value || scrubbing) 1f else 0f

    val scale = animateFloatAsState(
        targetValue = target,
        animationSpec = if (target < 1f) springSpec else tweenSpec,
        visibilityThreshold = 0.001f,
        label = "AlbumBounce"
    )

    YosWrapper {
        val dp = (7 + (27 * scale.value)).dp
        ShadowImageWithCache(
            dataLambda = { music()?.thumb }, contentDescription = null, modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .padding(start = dp, end = dp, bottom = dp)
                .then(modifier),
            imageQuality = ImageQuality.RAW,
            cornerRadius = 9.dp,
            crossfade = false,
            shadowOverlay = true,
            overlayContent = {
                AnimatedAlbumCoverOverlay(animatedAlbumCoverState)
            }
        )
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayingList(
    shuffleModeEnabledLambda: () -> Boolean,
    shuffleModeOnChanged: (Boolean) -> Unit,
    repeatModeLambda: () -> Int,
    repeatModeOnChanged: (Int) -> Unit,
    thisMusicPlayingLambda: () -> YosMediaItem?
) {
    val context = LocalContext.current

    Spacer(modifier = Modifier.height(12.dp))

    val musicList = remember("PlayingList_musicList") {
        playingMusicList
    }
    val nextInQueueList = remember("PlayingList_nextInQueueList") {
        MediaController.nextInQueueMusicList
    }
    val scope = rememberCoroutineScope()

    YosWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.545f),
        ) {
            val hide = remember("PlayingList_hide") {
                derivedStateOf {
                    nextInQueueList.value.isEmpty() && musicList.value.isNullOrEmpty()
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .padding(top = 10.dp)
                    .height(65.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.page_library_playlists),
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier/*.padding(top = 10.dp)*/
                    )
                    Text(
                        text = stringResource(
                            id = R.string.page_library_playlists_music_total,
                            nextInQueueList.value.size + (musicList.value?.size ?: 0)
                        ),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .overlayEffect()
                            .alpha(0.35f)
                    )
                }

                Row(
                    modifier = Modifier
                        .overlayEffect()
                        .alpha(0.6f)
                ) {
                    val dp = 36.dp
                    YosWrapper {
                        val shuffleBackgroundAlpha =
                            animateFloatAsState(targetValue = if (shuffleModeEnabledLambda()) 0.9f else 0f)
                        Box(
                            modifier = Modifier
                                .clickable(
                                    onClick = {
                                        Vibrator.click(context)
                                        shuffleModeOnChanged(!shuffleModeEnabledLambda())
                                        scope.launch(Dispatchers.IO) {
                                            MediaController.toggleShuffleMode()
                                        }
                                    },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() })
                                .size(36.dp)
                                .background(
                                    Color.White.copy(alpha = shuffleBackgroundAlpha.value),
                                    shape = YosRoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            YosWrapper {
                                val shuffleIconTint =
                                    animateColorAsState(targetValue = if (shuffleModeEnabledLambda()) Color.Black else Color.White)
                                Icon(
                                    painterResource(id = R.drawable.ic_nowplaying_shuffle),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(dp),
                                    tint = shuffleIconTint.value
                                )
                            }
                        }
                    }
                    YosWrapper {
                        val repeatHighlight =
                            repeatModeLambda() == REPEAT_MODE_ALL || repeatModeLambda() == REPEAT_MODE_ONE
                        val repeatBackgroundAlpha =
                            animateFloatAsState(targetValue = if (repeatHighlight) 0.9f else 0f)
                        Box(
                            modifier = Modifier
                                .clickable(
                                    onClick = {
                                        Vibrator.click(context)
                                        val targetMode = when (repeatModeLambda()) {
                                            REPEAT_MODE_OFF -> {
                                                REPEAT_MODE_ALL
                                            }

                                            REPEAT_MODE_ALL -> {
                                                REPEAT_MODE_ONE
                                            }

                                            else -> {
                                                REPEAT_MODE_OFF
                                            }
                                        }
                                        mediaControl?.repeatMode = targetMode
                                        mediaControl?.let { YosPlaybackService().setCustomButtons(it) }
                                        repeatModeOnChanged(targetMode)
                                    },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() })
                                .padding(start = 10.dp)
                                .size(36.dp)
                                .background(
                                    Color.White.copy(alpha = repeatBackgroundAlpha.value),
                                    shape = YosRoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            YosWrapper {
                                AnimatedContent(targetState = repeatModeLambda(), transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                }) {
                                    when (it) {
                                        REPEAT_MODE_ONE -> Icon(
                                            painterResource(id = R.drawable.ic_nowplaying_repeatone),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(dp),
                                            tint = animateColorAsState(targetValue = if (repeatHighlight) Color.Black else Color.White).value
                                        )

                                        else -> Icon(
                                            painterResource(id = R.drawable.ic_nowplaying_repeat),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(dp),
                                            tint = animateColorAsState(targetValue = if (repeatHighlight) Color.Black else Color.White).value
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }


            if (hide.value) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_uitabbar_library),
                        contentDescription = null,
                        modifier = Modifier
                            .overlayEffect()
                            .size(70.dp)
                            .alpha(0.6f)
                    )
                    Text(
                        text = stringResource(id = R.string.playlist_unavailable_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 18.dp, bottom = 12.dp)
                    )
                    YosWrapper {
                        Text(
                            text = stringResource(id = R.string.playlist_unavailable_desc),
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier
                                .overlayEffect()
                                .alpha(0.4f)
                        )
                    }
                }
            } else {
                val state = rememberLazyListState(
                    initialFirstVisibleItemIndex = 0,
                    initialFirstVisibleItemScrollOffset = -15
                )
                val nextInQueue = nextInQueueList.value
                val upNext = musicList.value ?: emptyList()
                var draggingQueueItemKey by remember {
                    mutableStateOf<String?>(null)
                }
                val reorderableState = rememberReorderableLazyListState(state) { from, to ->
                    val source = resolveQueueReorderTarget(
                        from.index,
                        nextInQueue.size,
                        upNext.size,
                    ) ?: return@rememberReorderableLazyListState
                    val destination = resolveQueueReorderTarget(
                        to.index,
                        nextInQueue.size,
                        upNext.size,
                    ) ?: return@rememberReorderableLazyListState

                    if (source.nextInQueue != destination.nextInQueue || source.index == destination.index) {
                        return@rememberReorderableLazyListState
                    }

                    Vibrator.click(context)
                    if (source.nextInQueue) {
                        MediaController.moveNextInQueueItemDuringDrag(source.index, destination.index)
                    } else {
                        MediaController.moveUpNextItemDuringDrag(source.index, destination.index)
                    }
                }

                YosWrapper {
                    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {

                        LazyColumn(state = state, modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithCache {
                                onDrawWithContent {
                                    val colors = listOf(
                                        Color.Transparent,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Transparent
                                    )

                                    drawContent()

                                    drawRect(
                                        brush = Brush.verticalGradient(colors),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                            }/*, contentPadding = PaddingValues(vertical = 12.dp)*/
                        ) {
                            item("blank_before") {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            if (nextInQueue.isNotEmpty()) {
                                item("next_in_queue_header") {
                                    QueueSectionHeader(
                                        title = stringResource(id = R.string.queue_next_in_queue),
                                        onClear = {
                                            scope.launch(Dispatchers.IO) {
                                                MediaController.clearNextInQueue()
                                            }
                                        },
                                    )
                                }

                                itemsIndexed(
                                    nextInQueue,
                                    key = { indexOfMusic, music -> queueItemKey("next", music, indexOfMusic, nextInQueue) }
                                ) { indexOfMusic, music ->
                                    val itemKey = queueItemKey("next", music, indexOfMusic, nextInQueue)

                                    ReorderableItem(reorderableState, key = itemKey) { isDragging ->
                                        QueueMusicListItem(
                                            music = music,
                                            reorderEnabled = nextInQueue.size > 1,
                                            isDragging = isDragging || draggingQueueItemKey == itemKey,
                                            reorderHandleModifier = Modifier.draggableHandle(
                                                onDragStarted = {
                                                    draggingQueueItemKey = itemKey
                                                    Vibrator.longClick(context)
                                                },
                                                onDragStopped = {
                                                    draggingQueueItemKey = null
                                                    Vibrator.click(context)
                                                    scope.launch(Dispatchers.IO) {
                                                        MediaController.saveQueueState()
                                                    }
                                                },
                                            ),
                                            onMoveToNextQueue = null,
                                            onRemove = {
                                                MediaController.removeNextInQueueItem(indexOfMusic)
                                            },
                                        ) {
                                            scope.launch(Dispatchers.IO) {
                                                MediaController.skipToNextInQueueItem(indexOfMusic)
                                            }
                                        }
                                    }
                                }
                            }

                            if (upNext.isNotEmpty()) {
                                item("up_next_header") {
                                    QueueSectionHeader(title = stringResource(id = R.string.queue_up_next))
                                }

                                itemsIndexed(
                                    upNext,
                                    key = { indexOfMusic, music -> queueItemKey("up", music, indexOfMusic, upNext) }
                                ) { indexOfMusic, music ->
                                    val itemKey = queueItemKey("up", music, indexOfMusic, upNext)

                                    ReorderableItem(reorderableState, key = itemKey) { isDragging ->
                                        QueueMusicListItem(
                                            music = music,
                                            reorderEnabled = upNext.size > 1,
                                            isDragging = isDragging || draggingQueueItemKey == itemKey,
                                            reorderHandleModifier = Modifier.draggableHandle(
                                                onDragStarted = {
                                                    draggingQueueItemKey = itemKey
                                                    Vibrator.longClick(context)
                                                },
                                                onDragStopped = {
                                                    draggingQueueItemKey = null
                                                    Vibrator.click(context)
                                                    scope.launch(Dispatchers.IO) {
                                                        MediaController.saveQueueState()
                                                    }
                                                },
                                            ),
                                            onMoveToNextQueue = {
                                                MediaController.moveUpNextToNextQueue(indexOfMusic)
                                            },
                                            onRemove = {
                                                MediaController.removeUpNextItem(indexOfMusic)
                                            },
                                        ) {
                                            scope.launch(Dispatchers.IO) {
                                                MediaController.skipToUpNextItem(indexOfMusic)
                                            }
                                        }
                                    }
                                }
                            }

                            item("blank_after") {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSectionHeader(
    title: String,
    onClear: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp)
            .padding(top = 12.dp, bottom = 6.dp)
            .overlayEffect(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )

        if (onClear != null) {
            Text(
                text = stringResource(id = R.string.queue_clear),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.64f),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    Vibrator.click(context)
                    onClear()
                },
            )
        }
    }
}

@Composable
private fun QueueMusicListItem(
    music: YosMediaItem,
    reorderEnabled: Boolean,
    isDragging: Boolean,
    reorderHandleModifier: Modifier,
    onMoveToNextQueue: (suspend () -> Boolean)?,
    onRemove: (suspend () -> Boolean)?,
    itemClick: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val addedToQueueToast = stringResource(id = R.string.queue_added_toast)
    var rowWidthPx by remember(music.uri, music.mediaId) {
        mutableFloatStateOf(0f)
    }
    var rowHeightPx by remember(music.uri, music.mediaId) {
        mutableFloatStateOf(0f)
    }
    var swipeOffsetPx by remember(music.uri, music.mediaId) {
        mutableFloatStateOf(0f)
    }
    var deleteHeightPx by remember(music.uri, music.mediaId) {
        mutableFloatStateOf(0f)
    }
    var resetAnimationJob by remember(music.uri, music.mediaId) {
        mutableStateOf<Job?>(null)
    }
    var deleteAnimating by remember(music.uri, music.mediaId) {
        mutableStateOf(false)
    }
    var deleteCollapsing by remember(music.uri, music.mediaId) {
        mutableStateOf(false)
    }

    val swipeRightEnabled = onMoveToNextQueue != null
    val swipeLeftEnabled = onRemove != null
    val triggerOffsetPx = rowWidthPx * 0.20f
    val minSwipeOffsetPx = if (swipeLeftEnabled) -rowWidthPx else 0f
    val maxSwipeOffsetPx = if (swipeRightEnabled) rowWidthPx else 0f
    val absoluteSwipeOffsetPx = if (swipeOffsetPx < 0f) -swipeOffsetPx else swipeOffsetPx
    val swipeProgress = if (rowWidthPx > 0f) {
        (absoluteSwipeOffsetPx / rowWidthPx).coerceIn(0f, 1f)
    } else {
        0f
    }
    val rowHeight = with(density) {
        rowHeightPx.toDp()
    }
    val swipeRevealWidth = with(density) {
        absoluteSwipeOffsetPx.toDp()
    }
    val deleteHeight = with(density) {
        deleteHeightPx.coerceAtLeast(0f).toDp()
    }

    val swipeModifier = if ((swipeRightEnabled || swipeLeftEnabled) && !deleteAnimating) {
        Modifier.draggable(
            orientation = Orientation.Horizontal,
            state = rememberDraggableState { delta ->
                if (rowWidthPx <= 0f || deleteAnimating) {
                    return@rememberDraggableState
                }

                resetAnimationJob?.cancel()

                val wasPastAddThreshold = swipeOffsetPx >= triggerOffsetPx
                val wasPastDeleteThreshold = swipeOffsetPx <= -triggerOffsetPx
                swipeOffsetPx = (swipeOffsetPx + delta).coerceIn(minSwipeOffsetPx, maxSwipeOffsetPx)
                val isPastAddThreshold = swipeOffsetPx >= triggerOffsetPx
                val isPastDeleteThreshold = swipeOffsetPx <= -triggerOffsetPx

                if (wasPastAddThreshold != isPastAddThreshold || wasPastDeleteThreshold != isPastDeleteThreshold) {
                    if (isPastAddThreshold || isPastDeleteThreshold) {
                        Vibrator.longClick(context)
                    } else {
                        Vibrator.click(context)
                    }
                }
            },
            onDragStopped = {
                val shouldMoveToNextQueue = triggerOffsetPx > 0f && swipeOffsetPx >= triggerOffsetPx
                val shouldRemove = triggerOffsetPx > 0f && swipeOffsetPx <= -triggerOffsetPx

                resetAnimationJob?.cancel()
                resetAnimationJob = coroutineScope.launch {
                    if (shouldRemove && onRemove != null) {
                        deleteAnimating = true
                        deleteHeightPx = if (rowHeightPx > 0f) {
                            rowHeightPx
                        } else {
                            with(density) { QueueRowHeight.toPx() }
                        }

                        animate(
                            initialValue = swipeOffsetPx,
                            targetValue = -rowWidthPx,
                            animationSpec = tween(
                                durationMillis = 110,
                                easing = EaseOutQuart,
                            ),
                        ) { value, _ ->
                            swipeOffsetPx = value
                        }

                        swipeOffsetPx = 0f
                        deleteCollapsing = true

                        animate(
                            initialValue = deleteHeightPx,
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = 170,
                                easing = EaseOutQuart,
                            ),
                        ) { value, _ ->
                            deleteHeightPx = value
                        }

                        onRemove.invoke()
                        return@launch
                    }

                    if (shouldMoveToNextQueue && onMoveToNextQueue?.invoke() == true) {
                        Toast.makeText(context, addedToQueueToast, Toast.LENGTH_SHORT).show()
                    }

                    val animationStart = swipeOffsetPx

                    animate(
                        initialValue = animationStart,
                        targetValue = 0f,
                        animationSpec = SpringSpec(
                            dampingRatio = 0.72f,
                            stiffness = 420f,
                            visibilityThreshold = 0.5f,
                        ),
                    ) { value, _ ->
                        swipeOffsetPx = value
                    }

                }
            },
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging || deleteAnimating) 1f else 0f)
            .then(if (deleteCollapsing) Modifier.height(deleteHeight) else Modifier)
            .onSizeChanged {
                rowWidthPx = it.width.toFloat()
                if (!deleteCollapsing) {
                    rowHeightPx = it.height.toFloat()
                }
            }
    ) {
        if (deleteCollapsing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(deleteHeight)
                    .background(Color(0xFFD32F2F)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_swipe_delete),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        } else if (swipeRightEnabled && swipeOffsetPx > 0f) {
            Box(
                modifier = Modifier
                    .width(swipeRevealWidth)
                    .height(rowHeight)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_swipe_queue),
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            val iconScale = 0.82f + (swipeProgress * 0.18f)
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                )
            }
        }

        if (!deleteCollapsing && swipeLeftEnabled && swipeOffsetPx < 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(swipeRevealWidth)
                    .height(rowHeight)
                    .background(Color(0xFFD32F2F)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_swipe_delete),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            val iconScale = 0.82f + (swipeProgress * 0.18f)
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                )
            }
        }

        if (!deleteCollapsing) {
            SmallMusicListItem(
                music = music,
                reorderEnabled = reorderEnabled,
                isDragging = isDragging,
                modifier = Modifier
                    .graphicsLayer {
                        translationX = swipeOffsetPx
                    }
                    .then(swipeModifier),
                reorderHandleModifier = reorderHandleModifier,
                itemClick = itemClick,
            )
        }
    }
}

@Composable
private fun SmallMusicListItem(
    music: YosMediaItem,
    reorderEnabled: Boolean,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    reorderHandleModifier: Modifier,
    itemClick: () -> Unit,
) {
    val draggedItemBackground by animateColorAsState(
        targetValue = if (isDragging) { Color.White.copy(alpha = 0.08f) } else { Color.Transparent },
        label = "QueueDraggedItemBackground",
    )
    Surface(
        modifier = modifier
            .height(QueueRowHeight)
            .fillMaxWidth(),
        color = draggedItemBackground,
        contentColor = Color.White,
        shape = QueueDraggingItemShape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    itemClick()
                }
                .padding(horizontal = 30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            println("重组：播放界面歌曲列表 ${music.title}")
            ShadowImageWithCache(
                dataLambda = { music.thumb },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                cornerRadius = 4.dp,
                shadowAlpha = 0f,
                imageQuality = ImageQuality.LOW
            )

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 12.dp)
            ) {
                Text(
                    text = music.title ?: defaultTitle,
                    modifier = Modifier.padding(bottom = 1.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                )

                Text(
                    text = music.artistsName ?: defaultArtistsName,
                    modifier = Modifier.alpha(0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.5.sp,
                    lineHeight = 11.5.sp,
                )
            }

            if (reorderEnabled) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .then(reorderHandleModifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_queue_reorder),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.34f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Lyric(
    lrcEntries: () -> List<List<Pair<Float, String>>>,
    lineEndTimes: () -> List<Float>,
    lineTransliterations: () -> List<String?>,
    lineSubtitles: () -> List<String?>,
    isTtmlLyrics: () -> Boolean,
    weightLambda: () -> Boolean,
    translationLambda: () -> Boolean,
    mainViewModel: MainViewModel,
    mediaViewModel: MediaViewModel,
    onBackClick: () -> Unit
) = YosWrapper {

    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
    ) {
        YosWrapper {
            Spacer(modifier = Modifier.statusBarsHeight(110.dp))

            YosLyricView(
                //mediaViewModel = mediaViewModel,
                lrcEntriesLambda = lrcEntries,
                lineEndTimesLambda = lineEndTimes,
                lineTransliterationsLambda = lineTransliterations,
                lineSubtitlesLambda = lineSubtitles,
                isTtmlLyricsLambda = isTtmlLyrics,
                liveTimeLambda = {
                    (mediaControl?.currentPosition ?: 0).toInt()
                },
                mediaEvent = object : YosMediaEvent {
                    override fun onSeek(position: Int) {
                        mediaControl?.seekTo(position.toLong())
                    }
                },
                translationLambda = translationLambda,
                blurLambda = {
                    SettingsLibrary.LyricBlurEffect
                },
                uiConfig = YosUIConfig(
                    noLrcText = stringResource(id = R.string.tip_no_lyrics)
                ),
                weightLambda = weightLambda,
                modifier = Modifier.drawWithCache {
                    onDrawWithContent {
                        val overlayPaint = Paint().apply {
                            blendMode = BlendMode.Plus
                        }
                        val rect = Rect(0f, 0f, size.width, size.height)
                        val canvas = this.drawContext.canvas

                        canvas.saveLayer(rect, overlayPaint)

                        val colors = if (weightLambda()) {
                            listOf(
                                Color.Transparent,
                                Color(0x59000000),
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color(0x59000000),
                                Color(0x21000000),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color.Transparent,
                                Color(0x59000000),
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                /*Color(0xD9000000),
                                Color(0xA6000000),
                                Color(0x73000000),
                                Color(0x59000000),
                                Color(0x3F000000),
                                Color(0x21000000),
                                Color(0x0C000000),*/
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black
                            )
                        }

                        drawContent()

                        drawRect(
                            brush = Brush.verticalGradient(colors),
                            blendMode = BlendMode.DstIn
                        )

                        canvas.restore()
                    }
                },
                onBackClick = onBackClick
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    navController: NavController,
    onMinimizeNowPlaying: suspend () -> Unit,
    musicPlayingLambda: () -> YosMediaItem?,
) {
    Row(
        modifier = Modifier
            .overlayEffect(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dp = 28.dp

        val context = LocalContext.current

        // Overflow menu state. The sheet renders only when this is true; the icon
        // swaps to its filled variant while open (PRD §5.1 FR-OM-2).
        val overflowSheetOpen = remember { mutableStateOf(false) }
        // Snapshot of the song that was playing when the sheet was opened
        // (PRD §5.3 FR-OM-8). All actions in the sheet refer to this item.
        val snapshotSong = remember { mutableStateOf<YosMediaItem?>(null) }
        val actionButtonScope = rememberCoroutineScope()

        NowPlayingOverflowSheet(
            isOpen = overflowSheetOpen,
            song = snapshotSong.value,
            navController = navController,
            onOpenLibraryTarget = {
                actionButtonScope.launch {
                    onMinimizeNowPlaying()
                }
                navController.toUI(it.route)
            },
        )

        Box(
            modifier = Modifier
                .clickable(
                    onClick = {
                        //println("收藏 开始")
                        val musicPlaying = musicPlayingLambda()
                        //println("收藏 $musicPlaying")
                        if (musicPlaying != null) {
                            Vibrator.click(context)
                            //println("收藏 切换状态
                            if (musicPlaying.let { FavPlayListLibrary.isFavorite(it) }) {
                                FavPlayListLibrary.removeMusic(musicPlaying)
                            } else {
                                FavPlayListLibrary.addMusic(musicPlaying)
                            }
                            //println("收藏 完毕")
                        }
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .size(dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = musicPlayingLambda()?.let { FavPlayListLibrary.isFavorite(it) }
                    ?: false,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }) {
                if (it) {
                    Icon(
                        painterResource(id = R.drawable.ic_nowplaying_favorited),
                        contentDescription = null,
                        modifier = Modifier
                            .size(dp)
                    )
                } else {
                    Icon(
                        painterResource(id = R.drawable.ic_nowplaying_favorite),
                        contentDescription = null,
                        modifier = Modifier
                            .overlayEffect()
                            .size(dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // No graphicsLayer rotation here: the underlying drawable
        // (ic_nowplaying_more / _fill) is drawn natively as a horizontal
        // three-dot ellipsis. A previous version applied a 90deg
        // graphicsLayer rotation to a vertical drawable; the rotation
        // changed the visible orientation but not the layout hitbox,
        // shifting the tap target to the right of the icon. Both the
        // drawable and hitbox are now horizontal and aligned.
        Box(
            modifier = Modifier
                .clickable(
                    onClick = {
                        // Snapshot the currently-playing song at open time
                        // (PRD FR-OM-8) and surface the sheet. The sheet wrapper
                        // handles its own haptic on open.
                        snapshotSong.value = musicPlayingLambda()
                        overflowSheetOpen.value = true
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() })
                .size(dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = overflowSheetOpen.value,
                // Match the Material3 ModalBottomSheet close animation
                // (~300ms) so the icon de-highlights exactly as the sheet
                // finishes sliding away. The default fadeIn/fadeOut uses a
                // spring spec that's noticeably slower than the sheet,
                // which leaves the icon visibly highlighted after the
                // sheet has already closed.
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 300))
                }) {
                if (it) {
                    Icon(
                        painterResource(id = R.drawable.ic_nowplaying_more_fill),
                        contentDescription = null,
                        modifier = Modifier
                            .size(dp)
                    )
                } else {
                    Icon(
                        painterResource(id = R.drawable.ic_nowplaying_more),
                        contentDescription = null,
                        modifier = Modifier
                            .overlayEffect()
                            .size(dp)
                    )
                }
            }
        }
    }
}

/**
 * Now Playing overflow menu. Opened from the three-dots icon in [ActionButtonsRow].
 *
 * Per PRD §5.3 FR-OM-8 this composable receives a *snapshot* of the song that
 * was playing when the sheet was opened — it does not re-subscribe to
 * [MediaController.musicPlaying] for the header, so subsequent track changes
 * while the sheet is open do not mutate the displayed song.
 *
 * Layout uses a single bottom sheet that swaps its body between three
 * internal screens (Menu / Playlist picker / Sleep timer). Selecting a row
 * doesn't dismiss + re-open a sub-sheet — the same sheet stays mounted and
 * its content changes in place, avoiding the visible close + reopen
 * animation that two separate sheets would cause.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NowPlayingOverflowSheet(
    isOpen: MutableState<Boolean>,
    song: YosMediaItem?,
    navController: NavController,
    onOpenLibraryTarget: (OverflowLibraryTarget) -> Unit,
) {
    if (!isOpen.value) return

    var screen by remember { mutableStateOf(OverflowScreen.Menu) }
    val navigationDirection = remember {
        mutableIntStateOf(SheetNavigationForward)
    }

    // Skip the partially-expanded state so the sheet always settles at the
    // height of its current content. The body swaps between screens of
    // very different heights (Menu ~3 rows, SleepTimer ~9 rows); with the
    // default partial state the sheet stays at the previously-settled
    // height and clips the taller content's bottom corners off-screen.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val onDismiss: () -> Unit = {
        isOpen.value = false
        screen = OverflowScreen.Menu
        navigationDirection.intValue = SheetNavigationForward
    }

    YosBottomSheetDialog(
        bottomSheetState = sheetState,
        blurred = true,
        onDismissRequest = onDismiss,
    ) {
        SheetAnimatedContent(
            targetState = screen,
            navigationDirection = navigationDirection.intValue,
            modifier = Modifier.fillMaxWidth(),
            label = "NowPlayingOverflowSheet",
        ) { currentScreen ->
            when (currentScreen) {
                OverflowScreen.Menu -> OverflowMenuBody(
                    song = song,
                    navController = navController,
                    onOpenLibraryTarget = onOpenLibraryTarget,
                    onDismiss = onDismiss,
                    onPickPlaylist = {
                        navigationDirection.intValue = SheetNavigationForward
                        screen = OverflowScreen.Playlist
                    },
                    onPickSleepTimer = {
                        navigationDirection.intValue = SheetNavigationForward
                        screen = OverflowScreen.SleepTimer
                    },
                )

                OverflowScreen.Playlist -> PlayListPickerContent(
                    songToAdd = song,
                    onDone = onDismiss,
                    onBack = {
                        navigationDirection.intValue = SheetNavigationBackward
                        screen = OverflowScreen.Menu
                    },
                )

                OverflowScreen.SleepTimer -> SleepTimerContent(
                    onDone = {},
                    onBack = {
                        navigationDirection.intValue = SheetNavigationBackward
                        screen = OverflowScreen.Menu
                    },
                )
            }
        }
    }
}

private enum class OverflowScreen { Menu, Playlist, SleepTimer }

private enum class OverflowLibraryTarget(val route: String)
{
    Artist(UI.ArtistInfo),
    Album(UI.AlbumInfo),
}

@Composable
private fun OverflowMenuBody(
    song: YosMediaItem?,
    navController: NavController,
    onOpenLibraryTarget: (OverflowLibraryTarget) -> Unit,
    onDismiss: () -> Unit,
    onPickPlaylist: () -> Unit,
    onPickSleepTimer: () -> Unit,
) {
    val addToPlaylistLabel = stringResource(R.string.now_playing_overflow_add_to_playlist)
    val sleepTimerLabel = stringResource(R.string.now_playing_overflow_sleep_timer)

    // PRD FR-OM-13: highlight the Sleep Timer row when a timer is running.
    // No subtitle is shown — only the tint changes.
    val sleepTimerActive = SleepTimer.state.value is SleepTimerState.Active
    val accent = MaterialTheme.colorScheme.primary

    val items = remember(
        addToPlaylistLabel, sleepTimerLabel, sleepTimerActive, accent,
        onPickPlaylist, onPickSleepTimer,
    ) {
        listOf(
            ActionItem(
                iconRes = R.drawable.ic_action_add,
                label = addToPlaylistLabel,
                onClick = onPickPlaylist,
            ),
            ActionItem(
                iconRes = R.drawable.ic_setting_moon,
                label = sleepTimerLabel,
                tint = if (sleepTimerActive) accent else null,
                onClick = onPickSleepTimer,
            ),
        )
    }

    ActionSheetBody(
        header = if (song != null) {
            {
                NowPlayingOverflowHeader(
                    song = song,
                    navController = navController,
                    onOpenLibraryTarget = onOpenLibraryTarget,
                    onDismiss = onDismiss,
                )
            }
        } else null,
        items = items,
    )
}

/**
 * Header row for the Now Playing overflow sheet.
 *
 * 64dp rounded album thumbnail + three lines (title bold, artist, album).
 * Receives a snapshot [song] — content does NOT update if the playing track
 * changes while the sheet is open (PRD §5.3 FR-OM-8).
 */
@Composable
private fun NowPlayingOverflowHeader(
    song: YosMediaItem,
    navController: NavController,
    onOpenLibraryTarget: (OverflowLibraryTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val shape = YosRoundedCornerShape(8.dp)
    val targetArtistNames = remember(song) {
        song.artistsList.orEmpty().filter { it.isNotBlank() }
    }
    val targetAlbumName = remember(song) {
        song.album?.takeIf { it.isNotBlank() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.thumb)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    clip = true
                    this.shape = shape
                },
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title.orEmpty(),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (targetArtistNames.isNotEmpty()) {
                val artistAnnotatedText = remember(targetArtistNames) {
                    buildAnnotatedString {
                        targetArtistNames.forEachIndexed { index, artistName ->
                            pushStringAnnotation(tag = "artist", annotation = artistName)
                            append(artistName)
                            pop()
                            if (index < targetArtistNames.lastIndex) {
                                append(", ")
                            }
                        }
                    }
                }

                ClickableText(
                    text = artistAnnotatedText,
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    modifier = Modifier.padding(top = 3.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onClick = { offset ->
                        val artistName = artistAnnotatedText
                            .getStringAnnotations(tag = "artist", start = offset, end = offset)
                            .firstOrNull()
                            ?.item
                            ?: return@ClickableText

                        LibraryObject.setTargetArtistName(artistName)
                        LibraryObject.setArtistSongsSearchOnOpen(false)
                        navController.markNextNavigationFromNowPlaying()
                        onDismiss()
                        onOpenLibraryTarget(OverflowLibraryTarget.Artist)
                    },
                )
            } else if (!song.artists.isNullOrBlank()) {
                Text(
                    text = song.artists,
                    fontSize = 13.5.sp,
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .alpha(0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!song.album.isNullOrBlank()) {
                Text(
                    text = song.album,
                    fontSize = 12.5.sp,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .alpha(0.5f)
                        .clickable(
                            enabled = targetAlbumName != null,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            val albumName = targetAlbumName ?: return@clickable
                            LibraryObject.setTargetAlbumName(albumName)
                            navController.markNextNavigationFromNowPlaying()
                            onDismiss()
                            onOpenLibraryTarget(OverflowLibraryTarget.Album)
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PlayingBar(
    modifier: Modifier,
    navController: NavController,
    onMinimizeNowPlaying: suspend () -> Unit,
    albumUrlLambda: () -> Uri?,
    musicPlayingLambda: () -> YosMediaItem?,
    onAlbumClick: () -> Unit
) = YosWrapper {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.5.dp)
            .padding(top = 22.dp)
            .height(70.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        ShadowImageWithCache(
            dataLambda = albumUrlLambda, contentDescription = null, modifier = modifier
                .size(69.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        onAlbumClick()
                    }), cornerRadius = 5.dp,
            imageQuality = ImageQuality.LOW,
            crossfadeDurationMillis = 150,
            shadowType = ShadowType.Small,
            shadowOverlay = true
        )
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 12.dp, end = 15.dp)
        ) {
            ScrollingSongTitle(
                text = musicPlayingLambda()?.title ?: defaultTitle,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.5.sp,
                modifier = Modifier.fillMaxWidth()
            )
            ScrollingSongTitle(
                text = musicPlayingLambda()?.artistsName
                    ?: defaultArtistsName,
                fontSize = 15.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .overlayEffect()
            )
        }

        YosWrapper {
            ActionButtonsRow(navController, onMinimizeNowPlaying, musicPlayingLambda)
        }
    }

}

@Composable
fun RowScope.AirPlay() {
    val contextCompose = LocalContext.current
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    val connectedDevices =
        remember("AirPlay_connectedDevices") { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    val audioDeviceName = remember("AirPlay_audioDeviceName") { mutableStateOf("") }
    val showName = remember("AirPlay_showName") { mutableStateOf(false) }

    YosWrapper {
        DisposableEffect(Unit) {
            val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED).apply {
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction("yos.music.player.BLUETOOTH_STATUS_REFRESH")
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val action = intent?.action
                    if (action == BluetoothDevice.ACTION_ACL_CONNECTED || action == BluetoothDevice.ACTION_ACL_DISCONNECTED || action == "yos.music.player.BLUETOOTH_STATUS_REFRESH") {
                        if (ActivityCompat.checkSelfPermission(
                                contextCompose,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        connectedDevices.value =
                            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()

                        val thisName =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                connectedDevices.value.firstOrNull { it.bluetoothClass.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO && it.isConnected() }?.alias
                            } else {
                                connectedDevices.value.firstOrNull { it.bluetoothClass.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO && it.isConnected() }?.name
                            }
                        showName.value = thisName != null
                        if (thisName != null) {
                            audioDeviceName.value = thisName.trim()
                        }
                    }
                }
            }
            // Use ContextCompat to handle the Android-14+ requirement that
            // registerReceiver carry an explicit export flag, transparently
            // on older API levels. Mirrors the existing TIRAMISU branch's
            // RECEIVER_EXPORTED choice to preserve behavior.
            ContextCompat.registerReceiver(
                contextCompose,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED,
            )

            if (ActivityCompat.checkSelfPermission(
                    contextCompose,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                connectedDevices.value = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
                val thisName =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        connectedDevices.value.firstOrNull { it.bluetoothClass.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO && it.isConnected() }?.alias
                    } else {
                        connectedDevices.value.firstOrNull { it.bluetoothClass.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO && it.isConnected() }?.name
                    }
                showName.value = thisName != null
                if (thisName != null) {
                    audioDeviceName.value = thisName.trim()
                }
            }

            onDispose {
                runCatching {
                    contextCompose.unregisterReceiver(receiver)
                }
            }
        }
    }

    YosWrapper {
        val context = LocalContext.current

        val systemMediaControlResolver = SystemMediaControlResolver(context)

        Column(
            modifier = Modifier
                .heightIn(min = 53.dp)
                .navigationBarsHeight(48.dp)
                .weight(1f)
                .clickable(
                    onClick = {
                        systemMediaControlResolver.intentSystemMediaDialog()
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.height(36.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(targetState = showName.value, transitionSpec = {
                    (scaleIn(initialScale = 0.3f) + fadeIn()).togetherWith(
                        scaleOut(
                            targetScale = 0.3f
                        ) + fadeOut()
                    )
                }, contentAlignment = Alignment.Center) {
                    if (it) {
                        Icon(
                            painterResource(id = R.drawable.ic_earphone),
                            contentDescription = null,
                            modifier = Modifier
                                .size(27.dp)
                        )
                    } else {
                        Icon(
                            painterResource(id = R.drawable.ic_nowplaying_airplay),
                            contentDescription = null,
                            modifier = Modifier
                                .size(21.5.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(showName.value, enter = scaleIn(initialScale = 0.3f) + fadeIn(), exit = scaleOut(
                targetScale = 0.3f
            ) + fadeOut()) {
                Text(
                    text = audioDeviceName.value,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun BluetoothDevice.isConnected(): Boolean {
    return runCatching {
        val isConnectedMethod =
            BluetoothDevice::class.java.getMethod("isConnected")
        isConnectedMethod.isAccessible = true
        isConnectedMethod.invoke(this) as Boolean
    }.getOrDefault(false)
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerControl(
    isPlayingLambda: () -> Boolean,
    isPlayingOnChanged: (Boolean) -> Unit,
    onPrevious: () -> Unit,
    onStatus: (Boolean) -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onLyrics: () -> Unit,
    onPlaylist: () -> Unit,
    nowPage: () -> String,
    onSlider: () -> Unit,
    onWhile: suspend () -> Unit,
    modifier: Modifier
) {
    val playingDuration = rememberSaveable(key = "PlayerControl_playingDuration") {
        mutableLongStateOf(0L)
    }
    val playingPosition = rememberSaveable(key = "PlayerControl_playingPosition") {
        mutableLongStateOf(0L)
    }
    val context = LocalContext.current
    val playedTime = rememberSaveable(key = "PlayerControl_playedTime") { mutableStateOf("0:00") }
    val remainingTime =
        rememberSaveable(key = "PlayerControl_remainingTime") { mutableStateOf("-0:00") }
    val sliderPosition = remember("PlayerControl_sliderPosition") { mutableFloatStateOf(0f) }
    val isSliding = remember("PlayerControl_isSliding") {
        mutableStateOf(false)
    }
    val progressTouched = remember("PlayerControl_progressTouched") { mutableStateOf(false) }
    // Only a real horizontal drag may move playback; a plain tap must not seek.
    val progressDragging = remember("PlayerControl_progressDragging") { mutableStateOf(false) }
    val progressInteraction = remember { MutableInteractionSource() }
    val progressPressed = progressInteraction.collectIsPressedAsState()
    val progressDragged = progressInteraction.collectIsDraggedAsState()
    val progressActive = progressTouched.value || progressPressed.value || progressDragged.value || isSliding.value
    val progressSwell = rememberControlSwell(progressActive, "Progress")

    // The icon follows a settled play state, so the brief pause reported while a
    // track changes never makes it flicker. A tap updates it immediately.
    val settledIcon = remember("PlayerControl_settledIcon") { mutableStateOf(isPlayingLambda()) }
    LaunchedEffect(Unit) {
        snapshotFlow { isPlayingLambda() }.collectLatest { playing ->
            if (playing) {
                settledIcon.value = true
            } else {
                delay(320L)
                settledIcon.value = false
            }
        }
    }

    // The cover reacts to the scrubber being held.
    LaunchedEffect(progressDragging.value, isSliding.value) {
        scrubbingArtwork.value = progressDragging.value || isSliding.value
    }

    YosWrapper {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp)
                .padding(bottom = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            YosWrapper {
                // 启动作用
                YosWrapper {
                    val lifecycleState =
                        LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()

                    LaunchedEffect(Unit) {
                        var lastPosition = 0L
                        while (true) {
                            //isPlaying.value = /*mediaControl?.isPlaying ?: false*/ FadeExo.targetStatus != 0
                            if (lifecycleState.value.isAtLeast(Lifecycle.State.RESUMED)) {
                                playingDuration.longValue = mediaControl?.duration ?: 0
                                playingPosition.longValue = mediaControl?.currentPosition ?: 0

                                if (!isSliding.value && playingDuration.longValue > 0L) {
                                    val totalSeconds =
                                        playingPosition.longValue.coerceAtLeast(0) / 1000
                                    if (totalSeconds != lastPosition) {
                                        playedTime.value = formatTime(totalSeconds)

                                        sliderPosition.floatValue =
                                            playingPosition.longValue.coerceAtLeast(0).toFloat()

                                        val remainingSeconds =
                                            playingDuration.longValue.coerceAtLeast(0) / 1000 - totalSeconds
                                        remainingTime.value = "-${formatTime(remainingSeconds)}"
                                        lastPosition = totalSeconds
                                    }
                                }

                                onWhile()
                            }

                            delay(1000)
                        }
                    }
                }

                // 进度条
                YosWrapper {
                    //println("重组：控制区域内部 - 进度条")
                    // The position only ticks once a second, so glide the fill between
                    // ticks; jumps backwards (seek, track change) snap instantly.
                    val glidingProgress = remember("PlayerControl_glide") { Animatable(0f) }
                    LaunchedEffect(sliderPosition.floatValue, isSliding.value) {
                        val target = sliderPosition.floatValue
                        if (isSliding.value || target <= glidingProgress.value) {
                            glidingProgress.snapTo(target)
                        } else {
                            glidingProgress.animateTo(
                                targetValue = target,
                                animationSpec = tween(1000, easing = LinearEasing)
                            )
                        }
                    }
                    // Reacts on touch down: swells, brightens and stretches slightly while
                    // held, then springs back on release.
                    Slider(
                        value = glidingProgress.value.coerceIn(
                            0f,
                            playingDuration.longValue.toFloat().coerceAtLeast(0f)
                        ),

                        onValueChange = { newValue ->
                            if (!progressDragging.value) return@Slider
                            isSliding.value = true

                            sliderPosition.floatValue = newValue
                            val newTotalSeconds = newValue.toLong() / 1000
                            playedTime.value = formatTime(newTotalSeconds)

                            val newRemainingSeconds =
                                playingDuration.longValue / 1000 - newTotalSeconds
                            remainingTime.value = "-${formatTime(newRemainingSeconds)}"

                            onSlider()
                        },
                        onValueChangeFinished = {
                            if (isSliding.value) {
                                Vibrator.longClick(context)
                                onSeek(sliderPosition.floatValue)
                            } else {
                                // Tapped without dragging: keep the current position.
                                sliderPosition.floatValue =
                                    playingPosition.longValue.coerceAtLeast(0).toFloat()
                            }
                            isSliding.value = false
                        },
                        valueRange = 0f..playingDuration.longValue.toFloat().coerceAtLeast(0f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0x0DFFFFFF)
                        ),
                        interactionSource = progressInteraction,
                        modifier = Modifier
                            // No overlayEffect here: its offscreen layer cropped the
                            // swollen bar at its resting bounds.
                            .padding(horizontal = 5.dp)
                            .pointerInput(Unit) {
                                val slop = viewConfiguration.touchSlop
                                awaitEachGesture {
                                    val down = awaitFirstDown(
                                        requireUnconsumed = false,
                                        pass = PointerEventPass.Initial
                                    )
                                    progressTouched.value = true
                                    progressDragging.value = false
                                    val startX = down.position.x
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change =
                                            event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break
                                        if (abs(change.position.x - startX) > slop) {
                                            progressDragging.value = true
                                        }
                                    }
                                    progressTouched.value = false
                                    progressDragging.value = false
                                }
                            }
                            .height(ControlTouchHeight),
                        thumb = {
                        },
                        track = {
                            Track(
                                sliderPositions = SliderPositions(
                                    initialActiveRange = 0f..(sliderPosition.floatValue / playingDuration.longValue.coerceAtLeast(1L))
                                ),
                                height = progressSwell.thickness,
                                overhang = progressSwell.overhang,
                                activeAlpha = progressSwell.activeAlpha,
                                inactiveAlpha = progressSwell.inactiveAlpha
                            )

                        }
                    )
                }

                // 控制按钮&进度文本
                YosWrapper {
                    //println("重组：控制区域内部 - 控制按钮&进度文本")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 7.dp)
                            .height(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = playedTime.value,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.3.sp,
                                color = Color.White,
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = -progressSwell.labelOffset.toPx()
                                        transformOrigin = TransformOrigin(0f, 0.5f)
                                        scaleX = progressSwell.labelScale
                                        scaleY = progressSwell.labelScale
                                        this.alpha = progressSwell.labelAlpha
                                    }
                            )
                            Text(
                                text = remainingTime.value,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.3.sp,
                                color = Color.White,
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = progressSwell.labelOffset.toPx()
                                        transformOrigin = TransformOrigin(1f, 0.5f)
                                        scaleX = progressSwell.labelScale
                                        scaleY = progressSwell.labelScale
                                        this.alpha = progressSwell.labelAlpha
                                    }
                            )
                        }

                        MusicQualityIndicator()
                    }


                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(61.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = false),
                                        onClick = {
                                            Vibrator.click(context)
                                            onPrevious()
                                        }),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(id = R.drawable.ic_nowplaying_rewind),
                                    contentDescription = "Previous",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(43.dp))

                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = false),
                                        onClick = {
                                            Vibrator.click(context)
                                            isPlayingOnChanged(!isPlayingLambda())
                                            settledIcon.value = isPlayingLambda()
                                            onStatus(isPlayingLambda())
                                        }),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(targetState = settledIcon.value, transitionSpec = {
                                    (scaleIn(initialScale = 0.3f) + fadeIn()).togetherWith(
                                        scaleOut(
                                            targetScale = 0.3f
                                        ) + fadeOut()
                                    )
                                }) {
                                    if (it) {
                                        Icon(
                                            painterResource(id = R.drawable.ic_nowplaying_pause),
                                            contentDescription = "Pause",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.4.dp)
                                        )
                                    } else {
                                        Icon(
                                            painterResource(id = R.drawable.ic_nowplaying_play),
                                            contentDescription = "Play",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(5.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(43.dp))
                            Box(
                                modifier = Modifier
                                    .size(61.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = false),
                                        onClick = {
                                            Vibrator.click(context)
                                            onNext()
                                        }),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(id = R.drawable.ic_nowplaying_fforward),
                                    contentDescription = "Next",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 音量调节
            YosWrapper {
                if (SettingsLibrary.NowPlayingShowVolumeBar) {
                    VolumeSlider(context = context, onSlider)
                }
            }

            // 底部 歌词&播放列表
            YosWrapper {
                //println("重组：控制区域内部 - 底部栏")
                Row(
                    modifier = Modifier
                        .overlayEffect()
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .alpha(0.4f),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val dp = 32.dp
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .weight(1f)
                            .clickable(
                                onClick = { onLyrics() },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = nowPage() == Lyric,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            }) {
                            if (it) {
                                Icon(
                                    painterResource(id = R.drawable.ic_nowplaying_lyricson),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(dp)
                                )
                            } else {
                                Icon(
                                    painterResource(id = R.drawable.ic_nowplaying_lyrics),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(0.1f))

                    AirPlay()

                    Spacer(modifier = Modifier.weight(0.1f))

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .weight(1f)
                            .clickable(
                                onClick = { onPlaylist() },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = nowPage() == PlayingList,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            }) {
                            if (it) {
                                Icon(
                                    painterResource(id = R.drawable.ic_nowplaying_queueon),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(dp)
                                )
                            } else {
                                Icon(
                                    painterResource(id = R.drawable.ic_nowplaying_queue),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(dp)
                                )
                            }
                        }
                    }
                }
            }

            // 边距填充
            /*YosWrapper {
                Spacer(modifier = Modifier.navigationBarsHeight(5.dp))
            }*/
            // 为显示设备名称，迁移到 AirPlay 底部处理
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeSlider(context: Context, onSlider: () -> Unit) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val sliderPosition =
        remember("VolumeSlider_sliderPosition") { mutableFloatStateOf(currentVolume / maxVolume.toFloat()) }
    val sliding = remember("VolumeSlider_sliding") {
        mutableStateOf(false)
    }
    val volumeTouched = remember("VolumeSlider_volumeTouched") { mutableStateOf(false) }
    val volumeDragging = remember("VolumeSlider_volumeDragging") { mutableStateOf(false) }
    val volumeInteraction = remember { MutableInteractionSource() }
    val volumePressed = volumeInteraction.collectIsPressedAsState()
    val volumeDragged = volumeInteraction.collectIsDraggedAsState()
    val volumeActive = volumeTouched.value || volumePressed.value || volumeDragged.value || sliding.value
    val volumeSwell = rememberControlSwell(volumeActive, "Volume")

    val volumeChangeReceiver = remember("VolumeSlider_volumeChangeReceiver") {
        VolumeChangeReceiver { newVolume ->
            sliderPosition.floatValue = newVolume / maxVolume.toFloat()
        }
    }
    val intentFilter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")

    DisposableEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                volumeChangeReceiver,
                intentFilter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(volumeChangeReceiver, intentFilter)
        }

        onDispose {
            context.unregisterReceiver(volumeChangeReceiver)
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 1.5.dp)
            .padding(horizontal = 8.dp)
            .padding(top = 4.dp, bottom = 2.5.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_nowplaying_volume),
            contentDescription = "Mute",
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    translationX = -volumeSwell.labelOffset.toPx()
                    scaleX = volumeSwell.iconScale
                    scaleY = volumeSwell.iconScale
                    this.alpha = volumeSwell.iconAlpha
                }
        )

        YosWrapper {
            val animatedProgress = if (sliding.value) {
                sliderPosition
            } else {
                animateFloatAsState(
                    targetValue = sliderPosition.floatValue,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    visibilityThreshold = 0.0001f
                )
            }

            // Matches the playback bar: reacts on touch, grows/brightens while held,
            // settles back on release.
            Slider(
                value = (animatedProgress.value * maxVolume),
                onValueChange = { newValue ->
                    if (!volumeDragging.value) return@Slider
                    sliding.value = true
                    sliderPosition.floatValue = newValue / maxVolume
                    val volume = newValue.toInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                    onSlider()
                },
                valueRange = 0f..maxVolume.toFloat(),
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0x0DFFFFFF)
                ),
                interactionSource = volumeInteraction,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.5.dp, end = 10.dp)
                    .height(ControlTouchHeight)
                    .pointerInput(Unit) {
                        val slop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial
                            )
                            volumeTouched.value = true
                            volumeDragging.value = false
                            val startX = down.position.x
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change =
                                    event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                if (abs(change.position.x - startX) > slop) {
                                    volumeDragging.value = true
                                }
                            }
                            volumeTouched.value = false
                            volumeDragging.value = false
                        }
                    }
                    ,

                thumb = {
                },
                track = {
                    Track(
                        sliderPositions = SliderPositions(
                            initialActiveRange = 0f..animatedProgress.value
                        ),
                        height = volumeSwell.thickness,
                        overhang = volumeSwell.overhang,
                        activeAlpha = volumeSwell.activeAlpha,
                        inactiveAlpha = volumeSwell.inactiveAlpha
                    )
                },

                onValueChangeFinished = {
                    Vibrator.longClick(context)
                    sliding.value = false
                }
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_nowplaying_volume_full),
            contentDescription = "Max Volume",
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    translationX = volumeSwell.labelOffset.toPx()
                    this.alpha = volumeSwell.iconAlpha
                }
        )
    }
}

@Stable
private data class ControlSwell(
    val thickness: Dp,
    val overhang: Dp,
    val activeAlpha: Float,
    val inactiveAlpha: Float,
    val labelOffset: Dp,
    val labelAlpha: Float,
    val labelScale: Float,
    val iconAlpha: Float,
    val iconScale: Float
)

private val ControlTouchHeight = 32.dp

/**
 * Apple Music style swell: on touch the bar grows in thickness, extends slightly past
 * both ends and brightens, while the time labels / volume icons brighten, scale up and
 * drift outward with the bar ends. At rest everything returns to its original opacity.
 * A critically damped spring gives the fast start / soft settle (~250 ms) both ways.
 */
@Composable
private fun rememberControlSwell(active: Boolean, label: String): ControlSwell {
    val floatSpec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 700f)
    val dpSpec = spring<Dp>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 700f)
    val thickness by animateDpAsState(
        targetValue = if (active) 17.dp else 7.dp,
        animationSpec = dpSpec,
        label = "${label}Thickness"
    )
    val overhang by animateDpAsState(
        targetValue = if (active) 3.dp else 0.dp,
        animationSpec = dpSpec,
        label = "${label}Overhang"
    )
    val activeAlpha by animateFloatAsState(
        targetValue = if (active) 0.8f else 0.4f,
        animationSpec = floatSpec,
        label = "${label}ActiveAlpha"
    )
    val inactiveAlpha by animateFloatAsState(
        targetValue = if (active) 0.22f else 0.05f,
        animationSpec = floatSpec,
        label = "${label}InactiveAlpha"
    )
    val labelOffset by animateDpAsState(
        targetValue = if (active) 3.dp else 0.dp,
        animationSpec = dpSpec,
        label = "${label}LabelOffset"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (active) 0.6f else 0.25f,
        animationSpec = floatSpec,
        label = "${label}LabelAlpha"
    )
    val labelScale by animateFloatAsState(
        targetValue = if (active) 1.12f else 1f,
        animationSpec = floatSpec,
        label = "${label}LabelScale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (active) 0.7f else 0.28f,
        animationSpec = floatSpec,
        label = "${label}IconAlpha"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (active) 1.12f else 1f,
        animationSpec = floatSpec,
        label = "${label}IconScale"
    )
    return ControlSwell(
        thickness, overhang, activeAlpha, inactiveAlpha,
        labelOffset, labelAlpha, labelScale, iconAlpha, iconScale
    )
}

@Composable
private fun Track(
    sliderPositions: SliderPositions,
    modifier: Modifier = Modifier,
    height: Dp,
    overhang: Dp,
    activeAlpha: Float,
    inactiveAlpha: Float
) = YosWrapper {
    // No graphicsLayer here on purpose: an alpha layer would composite offscreen and
    // crop the swollen bar at its resting bounds. Alpha lives in the colours instead,
    // and the extra width is drawn past the bounds so nothing is clipped.
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val grow = overhang.toPx()
        val sliderLeft = Offset(-grow, center.y)
        val sliderRight = Offset(size.width + grow, center.y)
        val sliderStart = if (isRtl) sliderRight else sliderLeft
        val sliderEnd = if (isRtl) sliderLeft else sliderRight
        val tickSize = 2.0.dp.toPx()
        val trackStrokeWidth = height.toPx()
        drawLine(
            Color.White.copy(alpha = inactiveAlpha),
            sliderStart,
            sliderEnd,
            trackStrokeWidth,
            StrokeCap.Round
        )
        val sliderValueEnd = Offset(
            sliderStart.x +
                    (sliderEnd.x - sliderStart.x) * sliderPositions.activeRange.endInclusive,
            center.y
        )

        val sliderValueStart = Offset(
            sliderStart.x +
                    (sliderEnd.x - sliderStart.x) * sliderPositions.activeRange.start,
            center.y
        )

        drawLine(
            Color.White.copy(alpha = activeAlpha),
            sliderValueStart,
            sliderValueEnd,
            trackStrokeWidth,
            StrokeCap.Round
        )
        sliderPositions.tickFractions.groupBy {
            it > sliderPositions.activeRange.endInclusive ||
                    it < sliderPositions.activeRange.start
        }.forEach { (outsideFraction, list) ->
            drawPoints(
                list.fastMap {
                    Offset(lerp(sliderStart, sliderEnd, it).x, center.y)
                },
                PointMode.Points,
                Color.White.copy(alpha = if (outsideFraction) inactiveAlpha else activeAlpha),
                tickSize,
                StrokeCap.Round
            )
        }
    }
}

/**
 * iOS-style scrolling title: overflowing text glides left after a short pause and
 * loops. The leading fade only appears once the text has actually moved, so a
 * still title is never clipped on its left edge.
 */
@Composable
private fun ScrollingSongTitle(
    text: String,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontWeight: FontWeight,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = fontWeight
    )
    BoxWithConstraints(modifier = modifier) {
        val available = constraints.maxWidth
        val textWidth = remember(text, available, fontSize, fontWeight) {
            measurer.measure(
                text = AnnotatedString(text),
                style = style,
                maxLines = 1,
                softWrap = false
            ).size.width
        }
        val scrolls = available > 0 && textWidth > available

        if (!scrolls) {
            Text(
                text = text,
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = fontWeight,
                softWrap = false
            )
            return@BoxWithConstraints
        }

        val density = LocalDensity.current
        val gapPx = with(density) { 42.dp.toPx() }
        val velocityPx = with(density) { 34.dp.toPx() }
        val offset = remember(text, available) { Animatable(0f) }

        LaunchedEffect(text, available, textWidth) {
            val distance = textWidth + gapPx
            // Wait once so the start of the name is readable, then keep looping.
            offset.snapTo(0f)
            delay(1500)
            while (true) {
                offset.animateTo(
                    targetValue = -distance,
                    animationSpec = tween(
                        durationMillis = ((distance / velocityPx) * 1000f).toInt().coerceAtLeast(1),
                        easing = LinearEasing
                    )
                )
                offset.snapTo(0f)
            }
        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fade = 20.dp.toPx().coerceAtMost(size.width / 4f)
                    val movedAway = offset.value < -1f
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to if (movedAway) Color.Transparent else Color.Black,
                            (fade / size.width) to Color.Black,
                            (1f - fade / size.width) to Color.Black,
                            1f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            Row(modifier = Modifier.graphicsLayer { translationX = offset.value }) {
                Text(
                    text = text,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    fontWeight = fontWeight,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(42.dp))
                Text(
                    text = text,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    fontWeight = fontWeight,
                    softWrap = false
                )
            }
        }
    }
}

fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "$minutes:${if (secs < 10) "0$secs" else "$secs"}"
}

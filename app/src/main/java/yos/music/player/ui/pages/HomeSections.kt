package yos.music.player.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yos.music.player.R
import yos.music.player.code.MediaController
import yos.music.player.data.libraries.MusicLibrary
import yos.music.player.data.libraries.StatsPeriod
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.data.libraries.artistsName
import yos.music.player.data.libraries.defaultArtistsName
import yos.music.player.data.libraries.defaultTitle
import yos.music.player.ui.UI
import yos.music.player.ui.theme.YosRoundedCornerShape
import yos.music.player.ui.toUI

private fun List<YosMediaItem>.pickRandom(count: Int): List<YosMediaItem> {
    if (size <= count) {return shuffled()}
    val selectedIndexes = LinkedHashSet<Int>()
    while (selectedIndexes.size < count) {selectedIndexes.add(indices.random())}
    return selectedIndexes.map { this[it] }
}

@Composable
fun QuickTilesRow(navController: NavController) {
    val musicList = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())
    if (musicList.isEmpty()) {return}

    val statsSnapshot = rememberStatsSnapshot(StatsPeriod.AllTime)
    val mostPlayed = remember(statsSnapshot) {
        statsSnapshot.trackEntries
            .sortedByDescending { it.listenedMs }
            .mapNotNull { it.libraryItem }
            .distinctBy { it.uri }
            .take(50)
    }
    val mostPlayedEnabled = mostPlayed.isNotEmpty()

    val scope = rememberCoroutineScope()

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickTile(
            title = stringResource(id = R.string.home_quick_most_played),
            iconRes = R.drawable.ic_uitabbar_stats,
            enabled = mostPlayedEnabled,
            modifier = Modifier.weight(1f),
            onClick = {
                if (!mostPlayedEnabled) {return@QuickTile}
                navController.toUI(UI.StatsTracks)
            }
        )
        QuickTile(
            title = stringResource(id = R.string.home_quick_shuffle),
            iconRes = R.drawable.ic_shuffle,
            enabled = true,
            modifier = Modifier.weight(1f),
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val shuffled = musicList.shuffled()
                    MediaController.prepare(
                        shuffled.first(),
                        shuffled,
                        shuffleModeEnabled = true
                    )
                }
            }
        )
    }
}

@Composable
private fun QuickTile(
    title: String,
    iconRes: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .clip(YosRoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = YosRoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecommendGrid() {
    val musicList = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())
    if (musicList.isEmpty()) {return}

    val gridSongs = remember("RecommendGrid_songs") {
        mutableListOf<YosMediaItem>().apply { addAll(musicList.pickRandom(4)) }
    }
    if (gridSongs.isEmpty()) {return}

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Warm the memory cache so the tiles appear with their covers on first render
    // instead of flashing from empty to artwork.
    LaunchedEffect(gridSongs) {
        val loader = context.imageLoader
        gridSongs.forEach { music ->
            music.thumb?.let { thumb ->
                loader.enqueue(
                    ImageRequest.Builder(context)
                        .data(thumb)
                        .memoryCacheKey(thumb.toString())
                        .build()
                )
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = stringResource(id = R.string.home_grid_recommend_title),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            gridSongs.chunked(2).forEach { rowSongs ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowSongs.forEach { music ->
                        RecommendGridItem(
                            music = music,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    MediaController.prepare(music, gridSongs)
                                }
                            }
                        )
                    }
                    if (rowSongs.size == 1) {
                        Surface(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.background) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendGridItem(
    music: YosMediaItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = YosRoundedCornerShape(14.dp)
    // Only the artwork is rounded; clipping the whole tile cut into the text's first letters.
    Column(
        modifier
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(music.thumb)
                .memoryCacheKey(music.thumb?.toString())
                // Quick fade from the tile surface; no grey placeholder frame first.
                .crossfade(150)
                .error(R.drawable.placeholder_music_default_artwork)
                .fallback(R.drawable.placeholder_music_default_artwork)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        )
        Text(
            text = music.title ?: defaultTitle,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = music.artistsName ?: defaultArtistsName,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .alpha(0.6f)
                .padding(top = 2.dp)
        )
    }
}

@Composable
fun GenresRow() {
    val musicList = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())
    if (musicList.isEmpty()) {return}

    val genres = remember(musicList) {
        musicList
            .groupBy { it.genre?.trim().orEmpty() }
            .filterKeys { it.isNotEmpty() }
            .entries
            .sortedByDescending { it.value.size }
            .take(8)
    }
    if (genres.isEmpty()) {return}

    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = stringResource(id = R.string.home_genres_title),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(genres, key = { it.key }) { (genreName, genreSongs) ->
                Surface(
                    modifier = Modifier
                        .clip(YosRoundedCornerShape(20.dp))
                        .clickable {
                            scope.launch(Dispatchers.IO) {
                                val shuffled = genreSongs.shuffled()
                                MediaController.prepare(shuffled.first(), shuffled)
                            }
                        },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = YosRoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = genreName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

package yos.music.player.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import yos.music.player.R
import yos.music.player.code.MediaController
import yos.music.player.data.libraries.MusicLibrary
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.ui.UI
import yos.music.player.ui.pages.library.MusicList
import yos.music.player.ui.pages.library.playlists.PlayListSearch
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.ProfileButton
import yos.music.player.ui.widgets.basic.SearchTextField
import yos.music.player.ui.widgets.basic.Title

private const val SEARCH_DEBOUNCE_MS = 150L
private const val SEARCH_MAX_RESULTS = 100
private const val BROWSE_CARD_COUNT = 10

private data class BrowseTile(
    val label: String,
    val songs: List<YosMediaItem>,
    val color: Color,
    val grainSeed: Int
)

/** Increments on every visit to Search so the browse palette is re-rolled. */
private object SearchVisitCounter {
    private var value = 0
    fun next(): Int = ++value
}

@Composable
fun Search(navController: NavController) {
    val songs = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())

    val searchText = remember { mutableStateOf("") }
    val results = remember { mutableStateOf(songs) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    // A fresh set of colours (and grain) every time Search is opened.
    val visitKey = remember { mutableStateOf(SearchVisitCounter.next()) }
    LaunchedEffect(Unit) { visitKey.value = SearchVisitCounter.next() }
    val tiles = remember(songs, visitKey.value) { buildBrowseTiles(songs) }

    // Debounced fuzzy search across the whole library, ranked by relevance.
    LaunchedEffect(searchText.value, songs) {
        val query = searchText.value
        if (query.isBlank()) {
            results.value = songs
            return@LaunchedEffect
        }
        delay(SEARCH_DEBOUNCE_MS)
        results.value = withContext(Dispatchers.Default) {
            PlayListSearch.matchAndRank(songs, query).take(SEARCH_MAX_RESULTS)
        }
    }

    Title(
        title = stringResource(id = R.string.page_search_title),
        rightIconContent = {
            ProfileButton { navController.toUI(UI.Settings.Main) }
        },
        content = {
            item("SearchField") {
                SearchTextField(
                    text = searchText.value,
                    placeholder = stringResource(id = R.string.search_library_placeholder),
                    onValueChange = { searchText.value = it },
                    onSearch = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    onClear = { searchText.value = "" }
                )
            }

            if (searchText.value.isBlank()) {
                items(
                    tiles.chunked(2),
                    key = { row: List<BrowseTile> -> "browse_${row.first().label}" }
                ) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 14.dp)
                    ) {
                        row.forEachIndexed { index, tile ->
                            if (index > 0) Spacer(modifier = Modifier.width(14.dp))
                            BrowseCard(
                                tile = tile,
                                modifier = Modifier.weight(1f)
                            ) {
                                keyboard?.hide()
                                if (tile.songs.isNotEmpty()) {
                                    scope.launch(Dispatchers.IO) {
                                        MediaController.prepare(tile.songs.first(), tile.songs)
                                    }
                                }
                            }
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.width(14.dp))
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                if (results.value.isEmpty()) {
                    item("SearchNoResults") {
                        SearchMessage(stringResource(id = R.string.search_no_results))
                    }
                }

                items(
                    results.value,
                    key = { music: YosMediaItem ->
                        music.uri?.toString() ?: music.hashCode().toString()
                    }
                ) { music ->
                    MusicList(
                        music = music,
                        navController = navController,
                        itemClick = {
                            keyboard?.hide()
                            scope.launch(Dispatchers.IO) {
                                MediaController.prepare(music, results.value)
                            }
                        }
                    )
                }
            }
        }
    )
}

/**
 * Builds the browse grid: one card per genre found in the library, falling back
 * to albums when the files carry no genre tags. Colours are flat, saturated and
 * reshuffled on every visit.
 */
private fun buildBrowseTiles(songs: List<YosMediaItem>): List<BrowseTile> {
    val random = Random(System.nanoTime())
    val hues = listOf(6f, 28f, 48f, 96f, 152f, 188f, 214f, 252f, 288f, 322f).shuffled(random)

    val byGenre = songs
        .filter { !it.genre.isNullOrBlank() }
        .groupBy { it.genre!!.trim() }

    val grouped: List<Pair<String, List<YosMediaItem>>> = if (byGenre.isNotEmpty()) {
        byGenre.entries.sortedByDescending { it.value.size }.map { it.key to it.value }
    } else {
        songs
            .filter { !it.album.isNullOrBlank() }
            .groupBy { it.album!!.trim() }
            .entries.sortedByDescending { it.value.size }
            .map { it.key to it.value }
    }

    return grouped.take(BROWSE_CARD_COUNT).mapIndexed { index, (label, items) ->
        val hue = hues[index % hues.size]
        BrowseTile(
            label = label,
            songs = items,
            color = Color.hsl(hue, 0.82f, 0.42f),
            grainSeed = random.nextInt()
        )
    }
}

/**
 * Flat monochromatic card with a subtle film grain. No text: the colour is the
 * whole design, and it changes on every visit to Search.
 */
@Composable
private fun BrowseCard(
    tile: BrowseTile,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .aspectRatio(1.35f)
            .clip(shape)
            .background(tile.color)
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1.35f)) {
            val random = Random(tile.grainSeed)
            val dots = 700
            repeat(dots) {
                val x = random.nextFloat() * size.width
                val y = random.nextFloat() * size.height
                val light = random.nextBoolean()
                drawRect(
                    color = if (light) Color.White.copy(alpha = 0.05f)
                    else Color.Black.copy(alpha = 0.06f),
                    topLeft = Offset(x, y),
                    size = Size(1.6f, 1.6f)
                )
            }
        }
    }
}

@Composable
private fun SearchMessage(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            modifier = Modifier.alpha(0.6f)
        )
    }
}

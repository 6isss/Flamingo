package yos.music.player.ui.pages

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yos.music.player.R
import yos.music.player.code.MediaController
import yos.music.player.data.libraries.MusicLibrary
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.data.objects.LibraryObject
import yos.music.player.ui.widgets.basic.ImageQuality
import yos.music.player.ui.widgets.basic.ShadowImage
import yos.music.player.data.spotify.SpotiFlacLauncher
import yos.music.player.data.spotify.SpotifyApi
import yos.music.player.data.spotify.SpotifyResult
import yos.music.player.ui.UI
import yos.music.player.ui.pages.library.MusicList
import yos.music.player.ui.pages.library.playlists.PlayListSearch
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.ProfileButton
import yos.music.player.ui.widgets.basic.SearchTextField
import yos.music.player.ui.widgets.basic.Title

private const val SEARCH_DEBOUNCE_MS = 150L
private const val SPOTIFY_DEBOUNCE_MS = 350L
private const val SEARCH_MAX_RESULTS = 100

@Composable
fun Search(navController: NavController) {
    val songs = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())
    val context = LocalContext.current

    val searchText = remember { mutableStateOf("") }
    val results = remember { mutableStateOf(songs) }
    val onlineMode = remember { mutableStateOf(false) }
    val spotifyResults = remember { mutableStateOf(emptyList<SpotifyResult>()) }
    val spotifyLoading = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Debounced fuzzy search across the whole library, ranked by
    // relevance (same engine as the in-playlist search).
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

    // Debounced Spotify catalogue search (metadata only).
    LaunchedEffect(searchText.value, onlineMode.value) {
        val query = searchText.value
        if (!onlineMode.value || query.isBlank()) {
            spotifyResults.value = emptyList()
            spotifyLoading.value = false
            return@LaunchedEffect
        }
        delay(SPOTIFY_DEBOUNCE_MS)
        spotifyLoading.value = true
        spotifyResults.value = SpotifyApi.search(query)
        spotifyLoading.value = false
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
                    placeholder = stringResource(
                        id = if (onlineMode.value) R.string.search_spotify_placeholder
                        else R.string.search_library_placeholder
                    ),
                    onValueChange = { searchText.value = it },
                    onSearch = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    onClear = { searchText.value = "" }
                )
            }

            item("SearchSource") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchSourceTab(
                        label = stringResource(id = R.string.search_source_library),
                        selected = !onlineMode.value,
                        modifier = Modifier.weight(1f)
                    ) { onlineMode.value = false }
                    SearchSourceTab(
                        label = stringResource(id = R.string.search_source_spotify),
                        selected = onlineMode.value,
                        modifier = Modifier.weight(1f)
                    ) { onlineMode.value = true }
                }
            }

            if (onlineMode.value) {
                if (!SpotifyApi.isConfigured) {
                    item("SpotifyNotConfigured") {
                        SearchMessage(stringResource(id = R.string.search_spotify_unavailable))
                    }
                } else if (spotifyLoading.value) {
                    item("SpotifyLoading") {
                        SearchMessage(stringResource(id = R.string.search_spotify_loading))
                    }
                } else if (searchText.value.isNotBlank() && spotifyResults.value.isEmpty()) {
                    item("SpotifyNoResults") {
                        SearchMessage(stringResource(id = R.string.search_no_results))
                    }
                }

                items(
                    spotifyResults.value,
                    key = { result: SpotifyResult -> "${result.type}_${result.id}" }
                ) { result ->
                    SpotifyResultRow(
                        result = result,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(result.externalUrl))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        onDownload = {
                            SpotiFlacLauncher.download(context, result.externalUrl)
                        }
                    )
                }
            } else if (searchText.value.isBlank()) {
                val browseAlbums = remember(songs) {
                    runCatching { MusicLibrary.albums }.getOrDefault(emptyList()).take(10)
                }
                items(
                    browseAlbums.chunked(2),
                    key = { row: List<String> -> "browse_${row.first()}" }
                ) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 14.dp)
                    ) {
                        row.forEachIndexed { index, album ->
                            if (index > 0) Spacer(modifier = Modifier.width(14.dp))
                            BrowseCard(
                                albumName = album,
                                modifier = Modifier.weight(1f)
                            ) {
                                LibraryObject.setTargetAlbumName(album)
                                navController.toUI(UI.AlbumInfo)
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
 * Full-bleed album card for the empty search state: square artwork under a
 * saturated gradient wash whose hue is derived from the album name, so it stays
 * the same between visits.
 */
@Composable
private fun BrowseCard(
    albumName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val songs = runCatching { MusicLibrary.Album[albumName] }.getOrDefault(emptyList())
    val shape = RoundedCornerShape(18.dp)
    val gradient = remember(albumName) {
        val hue = (albumName.hashCode().toLong() and 0xFFFFFF) % 360L
        Brush.linearGradient(
            listOf(
                Color.hsl(hue.toFloat(), 0.85f, 0.55f).copy(alpha = 0.55f),
                Color.hsl(((hue + 55) % 360).toFloat(), 0.9f, 0.35f).copy(alpha = 0.75f)
            )
        )
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        ShadowImage(
            dataLambda = { songs.getOrNull(0)?.thumb },
            contentDescription = albumName,
            modifier = Modifier.fillMaxWidth(),
            shadowAlpha = 0f,
            cornerRadius = 18.dp,
            imageQuality = ImageQuality.HIGH
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(gradient)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
        )
        Text(
            text = albumName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        )
    }
}

@Composable
private fun SearchSourceTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        text = label,
        fontSize = 15.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .alpha(if (selected) 1f else 0.6f)
    )
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

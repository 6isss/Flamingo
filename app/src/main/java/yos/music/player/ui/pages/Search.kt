package yos.music.player.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import yos.music.player.data.libraries.artistsName
import yos.music.player.ui.UI
import yos.music.player.ui.pages.library.playlists.PlayListSearch
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.ProfileButton
import yos.music.player.ui.widgets.basic.SearchTextField
import yos.music.player.ui.widgets.basic.Title

private const val SEARCH_DEBOUNCE_MS = 150L
private const val SEARCH_MAX_RESULTS = 100

@Composable
fun Search(navController: NavController) {
    val songs = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())

    val searchText = remember { mutableStateOf("") }
    val results = remember { mutableStateOf(emptyList<YosMediaItem>()) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    // Debounced fuzzy search across the whole library, ranked by relevance.
    LaunchedEffect(searchText.value, songs) {
        val query = searchText.value
        if (query.isBlank()) {
            results.value = emptyList()
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
                    onFocusChanged = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    onClear = { searchText.value = "" }
                )
                // Generous breathing room between the field and whatever follows.
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (searchText.value.isNotBlank()) {
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
                    SearchResultRow(
                        title = music.title ?: stringResource(id = R.string.page_search_title),
                        artist = music.artistsName.orEmpty(),
                        onClick = {
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

@Composable
private fun SearchResultRow(title: String, artist: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Text(text = title, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (artist.isNotBlank()) {
            Text(
                text = artist,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun SearchMessage(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Text(text = text, fontSize = 15.sp)
    }
}

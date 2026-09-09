package yos.music.player.ui.pages.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yos.music.player.R
import yos.music.player.code.MediaController
import yos.music.player.data.libraries.FavPlayListLibrary
import yos.music.player.data.libraries.MusicLibrary.songs
import yos.music.player.data.libraries.PlayListLibrary.playList
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.data.objects.LibraryObject
import yos.music.player.ui.UI
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.ProfileButton
import yos.music.player.ui.widgets.basic.Title
import yos.music.player.ui.widgets.basic.YosWrapper

/**
 * Flattened library: two pills at the top (Music and Favourites). Music lists
 * every song plus the playlists inline, so there is no extra hop through an
 * Artists/Albums/Playlists menu.
 */
@Composable
fun Library(navController: NavController) {
    val favourites = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize()
    ) {
        Title(
            title = stringResource(id = R.string.page_library_title),
            rightIconContent = {
                ProfileButton { navController.toUI(UI.Settings.Main) }
            }
        ) {
            item("LibraryTabs") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LibraryPill(
                        label = stringResource(id = R.string.page_library_songs),
                        selected = !favourites.value,
                        modifier = Modifier.weight(1f)
                    ) { favourites.value = false }
                    LibraryPill(
                        label = stringResource(id = R.string.page_library_playlists_fav_title),
                        selected = favourites.value,
                        modifier = Modifier.weight(1f)
                    ) { favourites.value = true }
                }
            }

            if (!favourites.value) {
                item("AllSongs") {
                    YosWrapper {
                        val targetTitle = stringResource(id = R.string.page_library_songs)
                        val targetList = songs
                        SmallLabelItem(
                            icon = painterResource(id = R.drawable.ic_library_link_icon_songs),
                            label = targetTitle
                        ) {
                            LibraryObject.setTargetListWithTitle(targetTitle, targetList)
                            navController.toUI(UI.NormalMusic)
                        }
                    }
                    LibraryDivider()
                }

                val ordered = playList.filter { it.isPinned }
                    .sortedBy { it.pinOrder ?: Int.MAX_VALUE } +
                        playList.filter { !it.isPinned }.sortedBy { it.name }

                items(ordered, key = { it.listID }) { list ->
                    SmallLabelItem(
                        icon = painterResource(id = R.drawable.ic_library_link_icon_playlists),
                        label = list.name
                    ) {
                        scope.launch(Dispatchers.IO) {
                            val targetList = list.songDataList.mapNotNull { uri ->
                                songs.find { it.uri == uri }
                            }
                            LibraryObject.setTargetListWithTitle(
                                list.name,
                                targetList,
                                playListId = list.listID,
                            )
                            withContext(Dispatchers.Main) {
                                navController.toUI(UI.NormalMusic)
                            }
                        }
                    }
                    LibraryDivider()
                }
            } else {
                val favs = FavPlayListLibrary.favPlayList
                if (favs.isEmpty()) {
                    item("NoFavourites") {
                        Text(
                            text = stringResource(id = R.string.search_no_results),
                            fontSize = 18.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 24.dp)
                                .alpha(0.6f)
                        )
                    }
                }
                items(
                    favs,
                    key = { music: YosMediaItem ->
                        music.uri?.toString() ?: music.hashCode().toString()
                    }
                ) { music ->
                    MusicList(
                        music = music,
                        navController = navController,
                        itemClick = {
                            scope.launch(Dispatchers.IO) {
                                MediaController.prepare(music, favs)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryPill(
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

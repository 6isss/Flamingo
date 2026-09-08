package yos.music.player.ui.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import yos.music.player.R
import yos.music.player.data.models.ImageViewModel
import yos.music.player.ui.pages.library.Library

/*@Stable
object HomePage {
    const val Home = "主页"
    const val Library = "资料库"
}*/

@Composable
fun HomeNav(
    navController: NavController,
    selectedPage: Int,
    imageViewModel: ImageViewModel,
    nowPageOnChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val home = context.getString(R.string.page_home_title)
    val library = context.getString(R.string.page_library_title)
    val search = context.getString(R.string.page_search_title)

    LaunchedEffect(selectedPage) {
        nowPageOnChanged(
            when (selectedPage) {
                0 -> home
                1 -> library
                2 -> search
                else -> home
            }
        )
    }

    AnimatedContent(
        targetState = selectedPage,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            fadeIn(tween(200, easing = EaseOutCubic)) togetherWith
                fadeOut(tween(120, easing = EaseOutCubic))
        },
        label = "HomeTabTransition"
    ) { page ->
        Column(Modifier.fillMaxSize()) {
            when (page) {
                0 -> Home(navController, imageViewModel)
                1 -> Library(navController)
                2 -> Search(navController)
            }
        }
    }
}

package yos.music.player.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import yos.music.player.R
import yos.music.player.data.models.ImageViewModel
import yos.music.player.ui.pages.library.Library
import yos.music.player.ui.widgets.basic.YosWrapper

/*@Stable
object HomePage {
    const val Home = "主页"
    const val Library = "资料库"
}*/

@Composable
fun HomeNav(
    navController: NavController,
    pagerState: PagerState,
    imageViewModel: ImageViewModel,
    nowPageOnChanged: (String) -> Unit
) =
    YosWrapper {
        val context = LocalContext.current
        val home = context.getString(R.string.page_home_title)
        val library = context.getString(R.string.page_library_title)
        val search = context.getString(R.string.page_search_title)

        //val pagerState = rememberPagerState(pageCount = { 2 })
        /*val nowPageIndex = when (nowPage.value) {
            home -> 0
            library -> 1
            else -> 0
        }

        YosWrapper {
            LaunchedEffect(nowPageIndex) {
                pagerState.animateScrollToPage(nowPageIndex)
            }
        }*/

        YosWrapper {
            LaunchedEffect(pagerState) {
                nowPageOnChanged(
                    when (pagerState.currentPage) {
                    0 -> home
                    1 -> library
                    2 -> search
                    else -> home
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
            key = { page -> page },
            userScrollEnabled = false
        ) { page ->
            // Subtle "bump": the incoming tab settles from a hair smaller
            // back to full size. Read at composition time (never during
            // placement) so it can't fight Compose's lookahead pass.
            val selected = pagerState.currentPage == page
            val pageScale by animateFloatAsState(
                targetValue = if (selected) 1f else 0.97f,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "TabBumpScale"
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .scale(pageScale)
            ) {
                when (page) {
                    0 -> Home(navController, imageViewModel)
                    1 -> Library(navController)
                    2 -> Search(navController)
                }
            }
        }
    }

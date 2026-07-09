package yos.music.player.ui.pages.settings.performance.userinterface

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import yos.music.player.R
import yos.music.player.code.AnimatedArtworkLibrary
import yos.music.player.data.libraries.MusicLibrary
import yos.music.player.data.libraries.SettingsLibrary
import yos.music.player.ui.UI
import yos.music.player.ui.pages.settings.Divider
import yos.music.player.ui.pages.settings.GroupSpacer
import yos.music.player.ui.pages.settings.GroupSpacerMedium
import yos.music.player.ui.pages.settings.LabelItem
import yos.music.player.ui.pages.settings.ListHeader
import yos.music.player.ui.pages.settings.SelectItem
import yos.music.player.ui.pages.settings.SettingBackground
import yos.music.player.ui.pages.settings.SwitchItem
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.RoundColumn
import yos.music.player.ui.widgets.basic.Title

@Composable
fun UserInterfaceSetting(navController: NavController) =
    SettingBackground {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val animatedAlbumCoverCacheDeleteArmed = remember("UserInterfaceSetting_animatedAlbumCoverCacheDeleteArmed") {
            mutableStateOf(false)
        }
        val animatedAlbumCoverCacheSizeBytes = remember("UserInterfaceSetting_animatedAlbumCoverCacheSizeBytes") {
            mutableLongStateOf(0L)
        }

        LaunchedEffect(MusicLibrary.songs)
        {
            animatedAlbumCoverCacheSizeBytes.longValue = AnimatedArtworkLibrary.cachedArtworkFilesSizeBytes(MusicLibrary.songs)
        }

        Title(title = stringResource(id = R.string.settings_performance_ui_title),
            onBack = {
                navController.popBackStack()
            },
            content = {
                item("settings") {
                    Column(Modifier.fillMaxSize()) {
                        // ListHeader(content = stringResource(id = R.string.settings_performance_ui_basic))

                        RoundColumn {
                            SelectItem(
                                title = stringResource(id = R.string.settings_performance_ui_theme),
                                items = listOf(
                                    "Auto",
                                    "Dark",
                                    "Light"
                                ),
                                value = SettingsLibrary.CustomTheme,
                                onValueChange = {
                                    SettingsLibrary.CustomTheme = it
                                }
                            )

                            Divider()

                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_ui_blur_effect_title),
                                // desc = stringResource(id = R.string.settings_performance_ui_blur_effect_desc),
                                onClick = {
                                    SettingsLibrary.BarBlurEffect = !SettingsLibrary.BarBlurEffect
                                },
                                checkedLambda = { SettingsLibrary.BarBlurEffect }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_ui_blur_effect_desc))

                        GroupSpacerMedium()

                        val showCornerSetDialog =
                        remember("UserInterfaceSetting_showCornerSetDialog") {
                            mutableStateOf(false)
                        }

                        RoundColumn {
                            LabelItem(
                                title = stringResource(id = R.string.settings_performance_ui_screen_corner_title),
                                // desc = stringResource(id = R.string.settings_performance_ui_screen_corner_desc),
                                superLink = true
                            ) {
                                showCornerSetDialog.value = true
                            }
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_ui_screen_corner_desc))

                        if (showCornerSetDialog.value) {
                            ScreenCornerSetDialog {
                                showCornerSetDialog.value = false
                            }
                        }

                        GroupSpacerMedium()

                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_ui_nowplaying_show_volume_bar),
                                // desc = stringResource(id = R.string.settings_performance_ui_nowplaying_show_volume_bar_desc),
                                onClick = {
                                    SettingsLibrary.NowPlayingShowVolumeBar =
                                        !SettingsLibrary.NowPlayingShowVolumeBar
                                },
                                checkedLambda = { SettingsLibrary.NowPlayingShowVolumeBar }
                            )
                        }

                        ListHeader(content = stringResource(id = R.string.settings_performance_ui_nowplaying_show_volume_bar_desc))
                        GroupSpacerMedium()

                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_ui_nowplaying_background_effect),
                                // desc = stringResource(id = R.string.settings_performance_ui_nowplaying_background_effect_desc),
                                onClick = {
                                    SettingsLibrary.NowplayingBackgroundEffect =
                                        !SettingsLibrary.NowplayingBackgroundEffect
                                },
                                checkedLambda = { SettingsLibrary.NowplayingBackgroundEffect }
                            )
                        }

                        ListHeader(content = stringResource(id = R.string.settings_performance_ui_nowplaying_background_effect_desc))

                        GroupSpacerMedium()

                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_library_animated_album_covers),
                                onClick = {
                                    SettingsLibrary.AnimatedAlbumCovers =
                                        !SettingsLibrary.AnimatedAlbumCovers
                                },
                                checkedLambda = { SettingsLibrary.AnimatedAlbumCovers }
                            )
                            Divider()
                            LabelItem(
                                title = stringResource(id = R.string.settings_library_animated_album_cover_blacklist),
                            ) {
                                navController.toUI(UI.Settings.AnimatedAlbumCoverBlacklist)
                            }
                            Divider()
                            LabelItem(
                                title = stringResource(
                                    id = R.string.settings_library_animated_album_cover_cache_clear,
                                    formatAnimatedAlbumCoverCacheSize(animatedAlbumCoverCacheSizeBytes.longValue)
                                ),
                                superLink = true,
                            ) {
                                if (!animatedAlbumCoverCacheDeleteArmed.value)
                                {
                                    animatedAlbumCoverCacheDeleteArmed.value = true
                                    Toast.makeText(
                                        context,
                                        R.string.settings_library_animated_album_cover_cache_clear_confirm,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@LabelItem
                                }

                                animatedAlbumCoverCacheDeleteArmed.value = false
                                scope.launch {
                                    val deletedCount = AnimatedArtworkLibrary.deleteCachedArtworkFiles(MusicLibrary.songs)
                                    animatedAlbumCoverCacheSizeBytes.longValue = AnimatedArtworkLibrary.cachedArtworkFilesSizeBytes(MusicLibrary.songs)
                                    Toast.makeText(
                                        context,
                                        context.getString(
                                            R.string.settings_library_animated_album_cover_cache_clear_done,
                                            deletedCount
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        ListHeader(content = stringResource(id = R.string.settings_library_animated_album_covers_desc))

                        GroupSpacer()
                    }
                }
            }
        )
    }

private fun formatAnimatedAlbumCoverCacheSize(sizeBytes: Long): String
{
    if (sizeBytes <= 0L) {return "0Mb"}

    return "${(sizeBytes + 1024L * 1024L - 1L) / (1024L * 1024L)}Mb"
}

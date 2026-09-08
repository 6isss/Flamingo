package yos.music.player.ui.widgets.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable

@Composable
fun YosWrapper(content: @Composable () -> Unit) = content()

package yos.music.player.data.spotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import yos.music.player.R

/**
 * Hands a Spotify link over to the separately installed SpotiFLAC app,
 * which performs the actual high quality download.
 */
object SpotiFlacLauncher {

    private val CANDIDATE_PACKAGES = listOf(
        "com.zarz.spotiflac",
        "com.spotiflac.app",
        "com.spotiflac.mobile"
    )

    private const val RELEASES_URL = "https://github.com/spotiflacapp/SpotiFLAC-Mobile/releases"

    private fun installedPackage(context: Context): String? =
        CANDIDATE_PACKAGES.firstOrNull { packageName ->
            runCatching {
                context.packageManager.getLaunchIntentForPackage(packageName) != null
            }.getOrDefault(false)
        }

    /**
     * Opens [spotifyUrl] inside SpotiFLAC. If the app is not installed the
     * user is sent to its releases page instead.
     */
    fun download(context: Context, spotifyUrl: String) {
        val target = installedPackage(context)

        if (target != null) {
            val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl)).apply {
                setPackage(target)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching { context.startActivity(viewIntent); true }.getOrDefault(false)) return

            // Some builds only accept a shared link rather than a view intent.
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, spotifyUrl)
                setPackage(target)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching { context.startActivity(sendIntent); true }.getOrDefault(false)) return

            // Last resort: just launch the app so the link can be pasted.
            context.packageManager.getLaunchIntentForPackage(target)?.let { launch ->
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(launch) }
            }
            return
        }

        Toast.makeText(context, R.string.search_spotiflac_missing, Toast.LENGTH_LONG).show()
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

package yos.music.player

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import yos.music.player.data.spotify.SpotifyAuth

/**
 * Invisible activity that receives the Spotify redirect (flamingo://spotify-callback),
 * exchanges the one-time code for a session and returns to the app.
 */
class SpotifyAuthActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        val code = intent?.data?.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            backToApp()
            return
        }
        lifecycleScope.launch {
            SpotifyAuth.completeLogin(this@SpotifyAuthActivity, code)
            backToApp()
        }
    }

    private fun backToApp() {
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }
        finish()
    }
}

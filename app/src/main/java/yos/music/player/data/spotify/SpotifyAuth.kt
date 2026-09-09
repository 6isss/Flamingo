package yos.music.player.data.spotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import yos.music.player.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * In-app Spotify sign in using the Authorization Code + PKCE flow, so no client
 * secret and no local.properties entry are needed. The session (access token,
 * refresh token and the account name) is kept in the app's private storage and
 * refreshed automatically.
 */
object SpotifyAuth {

    /** Public client id. PKCE needs no secret, so shipping this is safe. */
    private const val DEFAULT_CLIENT_ID = "50023866301e43bc9bde8770c1cc690d"

    const val REDIRECT_URI = "flamingo://spotify-callback"

    private const val AUTHORIZE_URL = "https://accounts.spotify.com/authorize"
    private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
    private const val SCOPES =
        "user-read-private user-read-email playlist-read-private user-library-read"

    private const val PREFS = "spotify_auth"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_EXPIRES = "expires_at"
    private const val KEY_VERIFIER = "code_verifier"
    private const val KEY_NAME = "display_name"

    val clientId: String
        get() = BuildConfig.SPOTIFY_CLIENT_ID.takeIf { it.isNotBlank() } ?: DEFAULT_CLIENT_ID

    /** Observable so Settings and Search recompose right after signing in or out. */
    val accountName = mutableStateOf<String?>(null)
    val signedIn = mutableStateOf(false)

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = prefs() ?: return
        signedIn.value = !prefs.getString(KEY_REFRESH, null).isNullOrBlank() ||
                !prefs.getString(KEY_ACCESS, null).isNullOrBlank()
        accountName.value = prefs.getString(KEY_NAME, null)
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Opens the Spotify login page in the browser. */
    fun startLogin(context: Context) {
        appContext = context.applicationContext
        val verifier = randomString(64)
        prefs()?.edit()?.putString(KEY_VERIFIER, verifier)?.apply()

        val challenge = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )

        val url = "$AUTHORIZE_URL?client_id=$clientId" +
                "&response_type=code" +
                "&redirect_uri=${URLEncoder.encode(REDIRECT_URI, "UTF-8")}" +
                "&code_challenge_method=S256" +
                "&code_challenge=$challenge" +
                "&scope=${URLEncoder.encode(SCOPES, "UTF-8")}"

        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Exchanges the code returned on the redirect for a session. */
    suspend fun completeLogin(context: Context, code: String): Boolean =
        withContext(Dispatchers.IO) {
            appContext = context.applicationContext
            val verifier = prefs()?.getString(KEY_VERIFIER, null) ?: return@withContext false
            val body = "grant_type=authorization_code" +
                    "&code=${URLEncoder.encode(code, "UTF-8")}" +
                    "&redirect_uri=${URLEncoder.encode(REDIRECT_URI, "UTF-8")}" +
                    "&client_id=$clientId" +
                    "&code_verifier=$verifier"
            val json = postToken(body) ?: return@withContext false
            store(json)
            fetchProfile()
            true
        }

    fun signOut() {
        prefs()?.edit()?.clear()?.apply()
        signedIn.value = false
        accountName.value = null
    }

    /** A valid user access token, refreshing it when needed. Null when signed out. */
    fun accessToken(): String? {
        val prefs = prefs() ?: return null
        val access = prefs.getString(KEY_ACCESS, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES, 0L)
        if (!access.isNullOrBlank() && System.currentTimeMillis() < expiresAt) return access

        val refresh = prefs.getString(KEY_REFRESH, null) ?: return null
        val json = postToken(
            "grant_type=refresh_token&refresh_token=${URLEncoder.encode(refresh, "UTF-8")}" +
                    "&client_id=$clientId"
        ) ?: return null
        store(json)
        return prefs.getString(KEY_ACCESS, null)
    }

    /** Drops the cached access token so the next read refreshes it. */
    fun invalidateAccessToken() {
        prefs()?.edit()?.remove(KEY_ACCESS)?.putLong(KEY_EXPIRES, 0L)?.apply()
    }

    private fun store(json: JSONObject) {
        val prefs = prefs() ?: return
        val access = json.optString("access_token").takeIf { it.isNotBlank() } ?: return
        val expiresIn = json.optLong("expires_in", 3600L)
        val editor = prefs.edit()
            .putString(KEY_ACCESS, access)
            .putLong(KEY_EXPIRES, System.currentTimeMillis() + (expiresIn - 60L) * 1000L)
        json.optString("refresh_token").takeIf { it.isNotBlank() }?.let {
            editor.putString(KEY_REFRESH, it)
        }
        editor.apply()
        signedIn.value = true
    }

    private fun fetchProfile() {
        val bearer = prefs()?.getString(KEY_ACCESS, null) ?: return
        runCatching {
            val connection = (URL("https://api.spotify.com/v1/me").openConnection()
                    as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Authorization", "Bearer $bearer")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val name = JSONObject(body).optString("display_name").takeIf { it.isNotBlank() }
                ?: JSONObject(body).optString("id").takeIf { it.isNotBlank() }
            if (name != null) {
                prefs()?.edit()?.putString(KEY_NAME, name)?.apply()
                accountName.value = name
            }
        }
    }

    private fun postToken(body: String): JSONObject? = runCatching {
        val connection = (URL(TOKEN_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        connection.outputStream.use { it.write(body.toByteArray()) }
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        JSONObject(response)
    }.getOrNull()

    private fun randomString(length: Int): String {
        val allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        val random = SecureRandom()
        return buildString(length) {
            repeat(length) { append(allowed[random.nextInt(allowed.length)]) }
        }
    }
}

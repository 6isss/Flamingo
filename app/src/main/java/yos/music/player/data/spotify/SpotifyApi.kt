package yos.music.player.data.spotify

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import yos.music.player.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * One entry of a Spotify catalogue search (metadata only, no audio).
 */
data class SpotifyResult(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val externalUrl: String
)

/**
 * Minimal Spotify Web API client using the client-credentials flow.
 * Only public catalogue metadata (tracks, albums, playlists) is requested.
 */
object SpotifyApi {

    private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
    private const val SEARCH_URL = "https://api.spotify.com/v1/search"

    @Volatile
    private var token: String? = null

    @Volatile
    private var tokenExpiresAt = 0L

    /** Signing in inside Settings is enough; build-time credentials stay optional. */
    val isConfigured: Boolean
        get() = SpotifyAuth.signedIn.value ||
                (BuildConfig.SPOTIFY_CLIENT_ID.isNotBlank() &&
                        BuildConfig.SPOTIFY_CLIENT_SECRET.isNotBlank())

    private val hasClientCredentials: Boolean
        get() = BuildConfig.SPOTIFY_CLIENT_ID.isNotBlank() &&
                BuildConfig.SPOTIFY_CLIENT_SECRET.isNotBlank()

    /** The bearer token plus whether it belongs to a signed-in user. */
    private fun accessToken(forceRefresh: Boolean = false): Pair<String, Boolean>? {
        if (forceRefresh) SpotifyAuth.invalidateAccessToken()
        SpotifyAuth.accessToken()?.let { return it to true }

        if (forceRefresh) {
            token = null
            tokenExpiresAt = 0L
        }
        val cached = token
        if (cached != null && System.currentTimeMillis() < tokenExpiresAt) return cached to false
        if (!hasClientCredentials) return null

        val credentials = Base64.encodeToString(
            "${BuildConfig.SPOTIFY_CLIENT_ID}:${BuildConfig.SPOTIFY_CLIENT_SECRET}".toByteArray(),
            Base64.NO_WRAP
        )

        return runCatching {
            val connection = (URL(TOKEN_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Authorization", "Basic $credentials")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            connection.outputStream.use { it.write("grant_type=client_credentials".toByteArray()) }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(body)
            val value = json.optString("access_token").takeIf { it.isNotBlank() }
            val expiresIn = json.optLong("expires_in", 3600L)
            if (value != null) {
                token = value
                tokenExpiresAt = System.currentTimeMillis() + (expiresIn - 60L) * 1000L
            }
            value?.let { it to false }
        }.getOrNull()
    }

    suspend fun search(query: String, limit: Int = 20): List<SpotifyResult> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            requestSearch(query, limit, forceRefresh = false)
                ?: requestSearch(query, limit, forceRefresh = true)
                ?: emptyList()
        }

    /** Returns null when the token was rejected, so the caller can retry once. */
    private fun requestSearch(query: String, limit: Int, forceRefresh: Boolean): List<SpotifyResult>? {
        val (bearer, isUserToken) = accessToken(forceRefresh) ?: return emptyList()
        val market = if (isUserToken) "from_token" else "US"
        val url = "$SEARCH_URL?q=${URLEncoder.encode(query, "UTF-8")}" +
                "&type=track,album,playlist&limit=$limit&market=$market"

        return runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $bearer")
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                connection.disconnect()
                Log.w("SpotifyApi", "Search failed with HTTP $code: $error")
                if (code == 401 || code == 403) {
                    token = null
                    tokenExpiresAt = 0L
                    return if (forceRefresh) emptyList() else null
                }
                return emptyList()
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            parse(JSONObject(body))
        }.getOrElse {
            Log.w("SpotifyApi", "Search failed", it)
            emptyList()
        }
    }

    private fun parse(root: JSONObject): List<SpotifyResult> {
        val results = mutableListOf<SpotifyResult>()

        root.optJSONObject("tracks")?.optJSONArray("items")?.forEachObject { item ->
            val album = item.optJSONObject("album")
            results += SpotifyResult(
                id = item.optString("id"),
                type = "track",
                title = item.optString("name"),
                subtitle = artistsOf(item.optJSONArray("artists")),
                imageUrl = imageOf(album?.optJSONArray("images")),
                externalUrl = externalUrlOf(item, "track")
            )
        }

        root.optJSONObject("albums")?.optJSONArray("items")?.forEachObject { item ->
            results += SpotifyResult(
                id = item.optString("id"),
                type = "album",
                title = item.optString("name"),
                subtitle = artistsOf(item.optJSONArray("artists")),
                imageUrl = imageOf(item.optJSONArray("images")),
                externalUrl = externalUrlOf(item, "album")
            )
        }

        root.optJSONObject("playlists")?.optJSONArray("items")?.forEachObject { item ->
            results += SpotifyResult(
                id = item.optString("id"),
                type = "playlist",
                title = item.optString("name"),
                subtitle = item.optJSONObject("owner")?.optString("display_name").orEmpty(),
                imageUrl = imageOf(item.optJSONArray("images")),
                externalUrl = externalUrlOf(item, "playlist")
            )
        }

        return results.filter { it.id.isNotBlank() && it.title.isNotBlank() }
    }

    private fun artistsOf(array: JSONArray?): String {
        if (array == null) return ""
        val names = mutableListOf<String>()
        array.forEachObject { names += it.optString("name") }
        return names.filter { it.isNotBlank() }.joinToString(", ")
    }

    private fun imageOf(array: JSONArray?): String? {
        if (array == null || array.length() == 0) return null
        return array.optJSONObject(0)?.optString("url")?.takeIf { it.isNotBlank() }
    }

    private fun externalUrlOf(item: JSONObject, type: String): String {
        val external = item.optJSONObject("external_urls")?.optString("spotify")
        return if (!external.isNullOrBlank()) external
        else "https://open.spotify.com/$type/${item.optString("id")}"
    }

    private inline fun JSONArray.forEachObject(action: (JSONObject) -> Unit) {
        for (index in 0 until length()) {
            optJSONObject(index)?.let(action)
        }
    }
}

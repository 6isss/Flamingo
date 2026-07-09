package yos.music.player.code

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import yos.music.player.data.libraries.SettingsLibrary
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.data.libraries.artistsName
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer

object AnimatedArtworkLibrary
{
    private const val ArtworkSearchEndpoint = "https://artwork.m8tec.top/api/v1/artwork/search"
    private const val NetworkTimeoutMilliseconds = 15000
    private const val DefaultSampleBufferBytes = 1024 * 1024
    private const val MaximumExpectedStartOffsetMicroseconds = 500_000L
    private val bandwidthRegex = Regex("BANDWIDTH=(\\d+)")
    private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|]")
    private val hlsMapUriRegex = Regex("URI=\"([^\"]+)\"")
    private val resolutionRegex = Regex("RESOLUTION=(\\d+)x(\\d+)")

    private data class StreamVariant(
        val url: String,
        val isAvc: Boolean,
        val pixelCount: Int,
        val bandwidth: Long
    )

    suspend fun resolveArtworkFile(music: YosMediaItem): File? = withContext(Dispatchers.IO)
    {
        if (!SettingsLibrary.AnimatedAlbumCovers) {return@withContext null}

        val localArtworkFile = localArtworkFile(music) ?: return@withContext null
        if (localArtworkFile.exists())
        {
            return@withContext if (localArtworkFile.isFile && localArtworkFile.length() > 0L && normalizeCachedMp4File(localArtworkFile))
            {
                localArtworkFile
            }
            else
            {
                null
            }
        }

        val albumName = music.album?.trim()?.takeIf { it.isNotEmpty() } ?: return@withContext null
        if (SettingsLibrary.isAnimatedAlbumCoverBlacklisted(albumName)) {return@withContext null}

        val searchUrl = buildSearchUrl(music, albumName) ?: return@withContext null
        val hlsUrl = fetchArtworkHlsUrl(searchUrl) ?: return@withContext null
        val mp4Url = resolveMp4Url(hlsUrl) ?: return@withContext null

        if (downloadFile(mp4Url, localArtworkFile)) {localArtworkFile} else null
    }

    suspend fun deleteCachedArtworkFiles(songs: List<YosMediaItem>): Int = withContext(Dispatchers.IO)
    {
        cachedArtworkFiles(songs).count { it.delete() }
    }

    suspend fun cachedArtworkFilesSizeBytes(songs: List<YosMediaItem>): Long = withContext(Dispatchers.IO)
    {
        cachedArtworkFiles(songs).sumOf { it.length() }
    }

    private fun localArtworkFile(music: YosMediaItem): File?
    {
        val albumName = music.album?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val songPath = music.uri?.path ?: return null
        val songDirectory = File(songPath).parentFile ?: return null
        if (!songDirectory.isDirectory) {return null}

        return File(songDirectory, animatedArtworkFileName(albumName))
    }

    private fun cachedArtworkFiles(songs: List<YosMediaItem>): List<File>
    {
        return songs
            .mapNotNull { localArtworkFile(it) }
            .distinctBy { it.absolutePath }
            .filter { it.isFile }
    }

    internal fun animatedArtworkFileName(albumName: String): String
    {
        val safeAlbumName = albumName
            .trim()
            .replace(invalidFileNameCharacters, "_")
            .ifEmpty { "animated_artwork" }

        return "$safeAlbumName.mp4"
    }

    private fun buildSearchUrl(music: YosMediaItem, albumName: String): String?
    {
        val artistName = music.albumArtists?.trim()?.takeIf { it.isNotEmpty() }
            ?: music.artistsName?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null

        val queryParameters = mutableListOf(
            "artist=${encodeUrlParameter(artistName)}",
            "album=${encodeUrlParameter(albumName)}"
        )
        music.title?.trim()?.takeIf { it.isNotEmpty() }?.let {
            queryParameters += "title=${encodeUrlParameter(it)}"
        }

        return "$ArtworkSearchEndpoint?${queryParameters.joinToString("&")}"
    }

    private fun encodeUrlParameter(value: String): String
    {
        return URLEncoder.encode(value, "UTF-8")
    }

    private fun fetchArtworkHlsUrl(searchUrl: String): String?
    {
        return runCatching {
            val responseText = readUrlText(searchUrl)
            JSONObject(responseText).optString("url").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun resolveMp4Url(hlsUrl: String): String?
    {
        return runCatching {
            val masterPlaylistText = readUrlText(hlsUrl)
            val mediaPlaylistUrl = pickBestStreamUrl(masterPlaylistText, hlsUrl)

            if (mediaPlaylistUrl == null)
            {
                return@runCatching extractMappedMp4Url(masterPlaylistText, hlsUrl)
            }

            extractMappedMp4Url(readUrlText(mediaPlaylistUrl), mediaPlaylistUrl)
        }.getOrNull()
    }

    internal fun pickBestStreamUrl(masterPlaylistText: String, masterPlaylistUrl: String): String?
    {
        val playlistLines = masterPlaylistText.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val streamVariants = mutableListOf<StreamVariant>()

        playlistLines.forEachIndexed { index, line ->
            if (!line.startsWith("#EXT-X-STREAM-INF")) {return@forEachIndexed}

            val streamPath = playlistLines.getOrNull(index + 1)?.takeIf { !it.startsWith("#") } ?: return@forEachIndexed
            val resolution = resolutionRegex.find(line)?.groupValues
            val pixelCount = if (resolution != null) {resolution[1].toInt() * resolution[2].toInt()} else {0}
            val bandwidth = bandwidthRegex.find(line)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L

            streamVariants += StreamVariant(
                url = absoluteUrl(masterPlaylistUrl, streamPath),
                isAvc = line.contains("avc1", ignoreCase = true),
                pixelCount = pixelCount,
                bandwidth = bandwidth
            )
        }

        return streamVariants
            .filter { it.isAvc }
            .ifEmpty { streamVariants }
            .maxWithOrNull(compareBy<StreamVariant> { it.pixelCount }.thenBy { it.bandwidth })
            ?.url
    }

    internal fun extractMappedMp4Url(mediaPlaylistText: String, mediaPlaylistUrl: String): String?
    {
        mediaPlaylistText.lineSequence().map { it.trim() }.forEach { line ->
            if (line.startsWith("#EXT-X-MAP"))
            {
                val mapUri = hlsMapUriRegex.find(line)?.groupValues?.getOrNull(1) ?: return@forEach
                if (mapUri.endsWith(".mp4", ignoreCase = true)) {return absoluteUrl(mediaPlaylistUrl, mapUri)}
            }
        }

        mediaPlaylistText.lineSequence().map { it.trim() }.forEach { line ->
            if (!line.startsWith("#") && line.endsWith(".mp4", ignoreCase = true))
            {
                return absoluteUrl(mediaPlaylistUrl, line)
            }
        }

        return null
    }

    private fun absoluteUrl(baseUrl: String, path: String): String
    {
        return URL(URL(baseUrl), path).toString()
    }

    private fun readUrlText(url: String): String
    {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = NetworkTimeoutMilliseconds
        connection.readTimeout = NetworkTimeoutMilliseconds
        connection.requestMethod = "GET"

        try
        {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {throw IllegalStateException("HTTP $responseCode")}

            return connection.inputStream.bufferedReader().use { it.readText() }
        }
        finally
        {
            connection.disconnect()
        }
    }

    private fun downloadFile(sourceUrl: String, destinationFile: File): Boolean
    {
        if (destinationFile.exists()) {return false}

        val temporaryFile = File(destinationFile.parentFile, ".${destinationFile.name}.tmp")
        val normalizedTemporaryFile = File(destinationFile.parentFile, ".${destinationFile.name}.normalized.tmp")
        var completed = false
        temporaryFile.delete()
        normalizedTemporaryFile.delete()

        val connection = URL(sourceUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = NetworkTimeoutMilliseconds
        connection.readTimeout = NetworkTimeoutMilliseconds
        connection.requestMethod = "GET"

        try
        {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {return false}

            connection.inputStream.use { inputStream ->
                temporaryFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (temporaryFile.length() == 0L) {return false}
            if (!normalizeMp4File(temporaryFile, normalizedTemporaryFile)) {return false}
            if (destinationFile.exists()) {return false}

            completed = normalizedTemporaryFile.renameTo(destinationFile)
            return completed
        }
        finally
        {
            connection.disconnect()
            temporaryFile.delete()
            if (!completed) {normalizedTemporaryFile.delete()}
        }
    }

    private fun normalizeCachedMp4File(artworkFile: File): Boolean
    {
        val firstVideoSampleTimeUs = firstVideoSampleTimeUs(artworkFile) ?: return false
        if (firstVideoSampleTimeUs <= MaximumExpectedStartOffsetMicroseconds) {return true}

        val normalizedTemporaryFile = File(artworkFile.parentFile, ".${artworkFile.name}.normalized.tmp")
        val backupFile = File(artworkFile.parentFile, ".${artworkFile.name}.backup.tmp")
        normalizedTemporaryFile.delete()
        backupFile.delete()

        if (!normalizeMp4File(artworkFile, normalizedTemporaryFile)) {return true}
        if (!artworkFile.renameTo(backupFile))
        {
            normalizedTemporaryFile.delete()
            return true
        }

        if (!normalizedTemporaryFile.renameTo(artworkFile))
        {
            normalizedTemporaryFile.delete()
            backupFile.renameTo(artworkFile)
            return false
        }

        backupFile.delete()
        return true
    }

    private fun firstVideoSampleTimeUs(artworkFile: File): Long?
    {
        val extractor = MediaExtractor()

        try
        {
            extractor.setDataSource(artworkFile.absolutePath)

            for (trackIndex in 0 until extractor.trackCount)
            {
                val format = extractor.getTrackFormat(trackIndex)
                val mimeType = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mimeType.startsWith("video/")) {continue}

                extractor.selectTrack(trackIndex)
                return extractor.sampleTime.coerceAtLeast(0L)
            }

            return null
        }
        catch (_: Exception)
        {
            return null
        }
        finally
        {
            extractor.release()
        }
    }

    private fun normalizeMp4File(sourceFile: File, destinationFile: File): Boolean
    {
        destinationFile.delete()

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var wroteSample = false
        var stopSucceeded = true

        try
        {
            extractor.setDataSource(sourceFile.absolutePath)
            muxer = MediaMuxer(destinationFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val muxerTrackIndexes = addSupportedTracks(extractor, muxer)
            if (muxerTrackIndexes.none { it >= 0 }) {return false}

            val trackStartTimesUs = LongArray(extractor.trackCount) { Long.MIN_VALUE }
            val sampleBuffer = ByteBuffer.allocate(maxSampleInputSize(extractor))
            val bufferInfo = MediaCodec.BufferInfo()

            muxer.start()
            muxerStarted = true

            while (true)
            {
                val sampleTrackIndex = extractor.sampleTrackIndex
                if (sampleTrackIndex < 0) {break}

                val muxerTrackIndex = muxerTrackIndexes[sampleTrackIndex]
                if (muxerTrackIndex < 0)
                {
                    extractor.advance()
                    continue
                }

                sampleBuffer.clear()
                val sampleSize = extractor.readSampleData(sampleBuffer, 0)
                if (sampleSize < 0) {break}

                val sampleTimeUs = extractor.sampleTime.coerceAtLeast(0L)
                if (trackStartTimesUs[sampleTrackIndex] == Long.MIN_VALUE)
                {
                    trackStartTimesUs[sampleTrackIndex] = sampleTimeUs
                }

                bufferInfo.set(
                    0,
                    sampleSize,
                    (sampleTimeUs - trackStartTimesUs[sampleTrackIndex]).coerceAtLeast(0L),
                    extractor.sampleFlags
                )
                muxer.writeSampleData(muxerTrackIndex, sampleBuffer, bufferInfo)
                wroteSample = true

                extractor.advance()
            }
        }
        catch (_: Exception)
        {
            wroteSample = false
        }
        finally
        {
            extractor.release()
            if (muxerStarted)
            {
                stopSucceeded = runCatching { muxer?.stop() }.isSuccess
            }
            muxer?.release()
        }

        if (!wroteSample || !stopSucceeded || destinationFile.length() == 0L)
        {
            destinationFile.delete()
            return false
        }

        return true
    }

    private fun addSupportedTracks(extractor: MediaExtractor, muxer: MediaMuxer): IntArray
    {
        val muxerTrackIndexes = IntArray(extractor.trackCount) { -1 }

        for (trackIndex in 0 until extractor.trackCount)
        {
            val format = extractor.getTrackFormat(trackIndex)
            val mimeType = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (!mimeType.startsWith("video/") && !mimeType.startsWith("audio/")) {continue}

            muxerTrackIndexes[trackIndex] = muxer.addTrack(format)
            extractor.selectTrack(trackIndex)
        }

        return muxerTrackIndexes
    }

    private fun maxSampleInputSize(extractor: MediaExtractor): Int
    {
        var maxSampleInputSize = DefaultSampleBufferBytes

        for (trackIndex in 0 until extractor.trackCount)
        {
            val format = extractor.getTrackFormat(trackIndex)
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE))
            {
                maxSampleInputSize = maxOf(maxSampleInputSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
            }
        }

        return maxSampleInputSize
    }
}

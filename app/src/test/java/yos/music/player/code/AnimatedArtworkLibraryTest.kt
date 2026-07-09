package yos.music.player.code

import org.junit.Assert.assertEquals
import org.junit.Test

class AnimatedArtworkLibraryTest
{
    @Test
    fun pickBestStreamUrl_prefersHighestResolutionAvcStream()
    {
        val masterPlaylist = """
            #EXTM3U
            #EXT-X-STREAM-INF:CODECS="hvc1",RESOLUTION=2160x2160,BANDWIDTH=20000000
            hvc-2160.m3u8
            #EXT-X-STREAM-INF:CODECS="avc1",RESOLUTION=486x486,BANDWIDTH=1000000
            avc-486.m3u8
            #EXT-X-STREAM-INF:CODECS="avc1",RESOLUTION=1080x1080,BANDWIDTH=6000000
            avc-1080.m3u8
        """.trimIndent()

        val streamUrl = AnimatedArtworkLibrary.pickBestStreamUrl(
            masterPlaylist,
            "https://example.com/artwork/master.m3u8"
        )

        assertEquals("https://example.com/artwork/avc-1080.m3u8", streamUrl)
    }

    @Test
    fun extractMappedMp4Url_resolvesRelativeMapUri()
    {
        val mediaPlaylist = """
            #EXTM3U
            #EXT-X-MAP:URI="cover-.mp4",BYTERANGE="897@0"
            #EXTINF:3.5,
            cover-.mp4
        """.trimIndent()

        val mp4Url = AnimatedArtworkLibrary.extractMappedMp4Url(
            mediaPlaylist,
            "https://example.com/artwork/video.m3u8"
        )

        assertEquals("https://example.com/artwork/cover-.mp4", mp4Url)
    }

    @Test
    fun animatedArtworkFileName_keepsAlbumNameAndMp4Extension()
    {
        assertEquals("A_B Test.mp4", AnimatedArtworkLibrary.animatedArtworkFileName("A/B Test"))
    }
}

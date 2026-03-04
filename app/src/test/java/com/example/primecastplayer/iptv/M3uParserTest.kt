package com.example.primecastplayer.iptv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    @Test
    fun parse_extractsChannelMetadataAndUrl() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="cnn" tvg-logo="https://example.com/cnn.png" group-title="News",CNN International
            https://example.com/live/cnn.m3u8
        """.trimIndent()

        val result = M3uParser.parse(playlist)

        assertEquals(1, result.size)
        val channel = result.first()
        assertEquals("CNN International", channel.name)
        assertEquals("https://example.com/live/cnn.m3u8", channel.streamUrl)
        assertEquals("News", channel.groupTitle)
        assertEquals(IptvCategory.LIVE_TV, channel.category)
    }

    @Test
    fun parse_infersMovieAndSeriesCategories() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 group-title="Movie",Sintel Movie
            https://example.com/vod/sintel.m3u8
            #EXTINF:-1 group-title="Drama",My Series Episode 1
            https://example.com/series/episode1.m3u8
        """.trimIndent()

        val result = M3uParser.parse(playlist)

        assertEquals(2, result.size)
        assertEquals(IptvCategory.MOVIES, result[0].category)
        assertEquals(IptvCategory.SERIES, result[1].category)
    }

    @Test
    fun parse_ignoresStreamsWithoutMetadata() {
        val playlist = """
            #EXTM3U
            https://example.com/no-metadata.m3u8
        """.trimIndent()

        val result = M3uParser.parse(playlist)

        assertTrue(result.isEmpty())
    }
}

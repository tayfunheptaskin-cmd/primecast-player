package com.example.primecastplayer.iptv

object SamplePlaylistRepository : IptvRepository {
    private val channels: List<IptvChannel> by lazy {
        M3uParser.parse(samplePlaylist)
    }

    override fun channelsFor(category: IptvCategory): List<IptvChannel> {
        return channels.filter { it.category == category }
    }

    // Public test streams for prototype/testing use.
    private val samplePlaylist = """
        #EXTM3U
        #EXTINF:-1 tvg-id="trt1" tvg-logo="https://example.com/logo/trt1.png" group-title="News",TRT Haber
        https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8
        #EXTINF:-1 tvg-id="beinmovie" tvg-logo="https://example.com/logo/movie.png" group-title="Movie",Sintel Movie HD
        https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8
        #EXTINF:-1 tvg-id="naturedoc" tvg-logo="https://example.com/logo/series.png" group-title="Series",Planet Earth Episode 1
        https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8
        #EXTINF:-1 tvg-id="foxsports" tvg-logo="https://example.com/logo/sport.png" group-title="Sports",Fox Sports Live
        https://moiptvhls-i.akamaihd.net/hls/live/652316/vtv1/master.m3u8
    """.trimIndent()
}

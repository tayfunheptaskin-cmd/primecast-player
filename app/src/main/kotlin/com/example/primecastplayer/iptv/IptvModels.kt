package com.example.primecastplayer.iptv

enum class IptvCategory(val displayName: String) {
    LIVE_TV("Live TV"),
    MOVIES("Movies"),
    SERIES("Series")
}

data class IptvChannel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val groupTitle: String,
    val category: IptvCategory,
    val logoUrl: String? = null,
    val tvgId: String? = null
)

interface IptvRepository {
    fun cachedPlaylistUrl(): String?
    suspend fun loadPlaylist(
        url: String,
        forceRefresh: Boolean = false
    ): PlaylistLoadResult
}

enum class PlaylistSource {
    NETWORK,
    CACHE,
    SAMPLE
}

data class PlaylistLoadResult(
    val channels: List<IptvChannel>,
    val source: PlaylistSource,
    val usedUrl: String,
    val message: String? = null
)

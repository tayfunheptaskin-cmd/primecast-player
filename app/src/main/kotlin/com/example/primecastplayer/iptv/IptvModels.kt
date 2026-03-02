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
    fun channelsFor(category: IptvCategory): List<IptvChannel>
}

package com.example.primecastplayer.iptv

object M3uParser {
    private val attributesRegex = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(content: String): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        var pendingMetadata: ChannelMetadata? = null

        content.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEach { line ->
                when {
                    line.startsWith("#EXTINF", ignoreCase = true) -> {
                        pendingMetadata = parseMetadata(line)
                    }

                    line.startsWith("#") -> Unit

                    else -> {
                        val metadata = pendingMetadata ?: return@forEach
                        channels += IptvChannel(
                            id = "${metadata.name}-${line}".hashCode().toString(),
                            name = metadata.name,
                            streamUrl = line,
                            groupTitle = metadata.groupTitle,
                            category = inferCategory(metadata.groupTitle, metadata.name),
                            logoUrl = metadata.logoUrl,
                            tvgId = metadata.tvgId
                        )
                        pendingMetadata = null
                    }
                }
            }

        return channels
    }

    private fun parseMetadata(extInfLine: String): ChannelMetadata {
        val attributes = attributesRegex.findAll(extInfLine).associate {
            it.groupValues[1].lowercase() to it.groupValues[2].trim()
        }

        val channelName = extInfLine.substringAfterLast(",").trim().ifEmpty { "Unknown Channel" }

        return ChannelMetadata(
            name = channelName,
            groupTitle = attributes["group-title"].orEmpty(),
            logoUrl = attributes["tvg-logo"],
            tvgId = attributes["tvg-id"]
        )
    }

    private fun inferCategory(groupTitle: String, name: String): IptvCategory {
        val candidateText = "$groupTitle $name".lowercase()
        return when {
            movieKeywords.any(candidateText::contains) -> IptvCategory.MOVIES
            seriesKeywords.any(candidateText::contains) -> IptvCategory.SERIES
            else -> IptvCategory.LIVE_TV
        }
    }

    private val movieKeywords = listOf("movie", "film", "sinema", "vod")
    private val seriesKeywords = listOf("series", "dizi", "show", "episode")
}

private data class ChannelMetadata(
    val name: String,
    val groupTitle: String,
    val logoUrl: String?,
    val tvgId: String?
)

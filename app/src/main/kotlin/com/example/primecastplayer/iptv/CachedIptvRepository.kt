package com.example.primecastplayer.iptv

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CachedIptvRepository(
    private val appContext: Context
) : IptvRepository {

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val cacheFile = File(appContext.filesDir, CACHE_FILE_NAME)

    override fun cachedPlaylistUrl(): String? = prefs.getString(KEY_LAST_URL, null)

    override suspend fun loadPlaylist(
        url: String,
        forceRefresh: Boolean
    ): PlaylistLoadResult = withContext(Dispatchers.IO) {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isBlank()) {
            return@withContext loadFromCacheOrFallback(
                fallbackMessage = "Playlist URL bos. Ornek playlist gosteriliyor."
            )
        }

        val useCacheFirst = !forceRefresh && normalizedUrl == cachedPlaylistUrl()
        if (useCacheFirst) {
            val cachedChannels = readCachedChannels()
            if (cachedChannels.isNotEmpty()) {
                return@withContext PlaylistLoadResult(
                    channels = cachedChannels,
                    source = PlaylistSource.CACHE,
                    usedUrl = normalizedUrl,
                    message = "Playlist cache'ten yuklendi."
                )
            }
        }

        try {
            val content = downloadPlaylist(normalizedUrl)
            val parsedChannels = M3uParser.parse(content)
            if (parsedChannels.isEmpty()) {
                throw IOException("Playlist bos veya desteklenmeyen formatta.")
            }
            writeCache(content, normalizedUrl)
            PlaylistLoadResult(
                channels = parsedChannels,
                source = PlaylistSource.NETWORK,
                usedUrl = normalizedUrl,
                message = "Playlist agdan guncellendi."
            )
        } catch (error: Exception) {
            loadFromCacheOrFallback(
                fallbackMessage = "Aga erisim olmadi (${error.message}). Ornek playlist gosteriliyor.",
                cacheMessage = "Ag hatasi alindi, cache playlist kullanildi (${error.message})."
            )
        }
    }

    private fun loadFromCacheOrFallback(
        fallbackMessage: String,
        cacheMessage: String = "Playlist cache'ten yuklendi."
    ): PlaylistLoadResult {
        val cachedChannels = readCachedChannels()
        if (cachedChannels.isNotEmpty()) {
            return PlaylistLoadResult(
                channels = cachedChannels,
                source = PlaylistSource.CACHE,
                usedUrl = cachedPlaylistUrl() ?: SamplePlaylistRepository.sourceLabel,
                message = cacheMessage
            )
        }

        return PlaylistLoadResult(
            channels = SamplePlaylistRepository.channels,
            source = PlaylistSource.SAMPLE,
            usedUrl = SamplePlaylistRepository.sourceLabel,
            message = fallbackMessage
        )
    }

    private fun writeCache(content: String, url: String) {
        cacheFile.writeText(content)
        prefs.edit().putString(KEY_LAST_URL, url).apply()
    }

    private fun readCachedChannels(): List<IptvChannel> {
        if (!cacheFile.exists()) {
            return emptyList()
        }
        val cachedContent = cacheFile.readText()
        if (cachedContent.isBlank()) {
            return emptyList()
        }
        return M3uParser.parse(cachedContent)
    }

    private fun downloadPlaylist(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 12_000
        connection.readTimeout = 18_000
        connection.setRequestProperty("User-Agent", "PrimeCastPlayer/1.0")
        connection.instanceFollowRedirects = true

        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("HTTP $code")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_PLAYLIST_URL = "https://iptv-org.github.io/iptv/categories/news.m3u"
        private const val PREFS_NAME = "iptv_repository"
        private const val KEY_LAST_URL = "last_playlist_url"
        private const val CACHE_FILE_NAME = "playlist_cache.m3u"
    }
}

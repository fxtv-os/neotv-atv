package com.neoos.neotv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resolves channel logos from the community-maintained iptv-org logo
 * database (https://iptv-org.github.io/api/logos.json) using the channel's
 * tvg-id, for m3u lists that don't ship their own tvg-logo attribute
 * (e.g. many ISP-exported lists like Vodafone only carry tvg-id).
 *
 * This is a read-only lookup against public logo metadata at runtime — it
 * does NOT change which streams/playlists the app uses (those still come
 * exclusively from the bundled assets/m3u files, see ChannelRepository).
 * If the device is offline or the lookup fails, channels simply fall back
 * to the placeholder icon as before.
 */
object TvgLogoResolver {

    private const val LOGOS_URL = "https://iptv-org.github.io/api/logos.json"
    private var cache: Map<String, String>? = null
    private var loadAttempted = false

    /** tvgId may be a bare channel id ("ZDF.de") or "channel@feed" ("DasErste.de@HD"). */
    suspend fun resolve(tvgId: String?): String? {
        if (tvgId.isNullOrBlank()) return null
        val map = load() ?: return null

        map[tvgId]?.let { return it }
        val baseId = tvgId.substringBefore('@')
        return map[baseId]
    }

    private suspend fun load(): Map<String, String>? {
        cache?.let { return it }
        if (loadAttempted) return cache
        loadAttempted = true

        return withContext(Dispatchers.IO) {
            try {
                val connection = (URL(LOGOS_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                }
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val arr = JSONArray(text)
                val result = LinkedHashMap<String, String>()

                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val channel = o.optString("channel").takeIf { it.isNotBlank() } ?: continue
                    val feed = o.optString("feed").takeIf { it.isNotBlank() }
                    val url = o.optString("url").takeIf { it.isNotBlank() } ?: continue
                    val inUse = o.optBoolean("in_use", false)

                    val key = if (feed != null) "$channel@$feed" else channel
                    if (inUse || !result.containsKey(key)) {
                        result[key] = url
                    }
                    if (!result.containsKey(channel)) {
                        result[channel] = url
                    }
                }
                cache = result
                result
            } catch (e: Exception) {
                null
            }
        }
    }
}

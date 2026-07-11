package com.neoos.neotv.data

import android.content.Context
import com.neoos.neotv.model.Channel

/**
 * Loads every *.m3u file that ships inside assets/m3u/ and merges them into
 * one channel catalogue. Because these files are bundled at build time, the
 * user can never point the app at an external playlist from the UI (per spec).
 *
 * To add another bundled list: drop a new .m3u file into
 * app/src/main/assets/m3u/ and rebuild the app. To hand-curate your own
 * "M3U die ich selber zusammenbaue", edit/replace assets/m3u/custom.m3u.
 */
object ChannelRepository {

    private const val ASSET_DIR = "m3u"
    private var cache: List<Channel>? = null

    fun getAllChannels(context: Context): List<Channel> {
        cache?.let { return it }
        val am = context.assets
        val files = am.list(ASSET_DIR)?.filter { it.endsWith(".m3u", ignoreCase = true) || it.endsWith(".m3u8", ignoreCase = true) } ?: emptyList()

        val all = mutableListOf<Channel>()
        for (file in files) {
            runCatching {
                am.open("$ASSET_DIR/$file").use { input ->
                    all.addAll(M3uParser.parse(input, file))
                }
            }
        }
        cache = all
        return all
    }

    fun getGroups(context: Context): List<String> =
        getAllChannels(context).map { it.group }.distinct()

    fun getByGroup(context: Context, group: String): List<Channel> =
        getAllChannels(context).filter { it.group == group }

    fun search(context: Context, query: String): List<Channel> {
        if (query.isBlank()) return emptyList()
        val q = query.trim()
        return getAllChannels(context).filter { it.name.contains(q, ignoreCase = true) }
    }
}

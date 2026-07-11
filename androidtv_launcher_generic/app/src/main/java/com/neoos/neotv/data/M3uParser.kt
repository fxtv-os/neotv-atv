package com.neoos.neotv.data

import com.neoos.neotv.model.Channel
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Minimal, dependency-free M3U/M3U8 (Extended) parser.
 * Supports the tags actually used across IPTV lists:
 *   #EXTINF:-1 tvg-logo="..." group-title="...",Channel Name
 *   http://stream.url/playlist.m3u8
 */
object M3uParser {

    private val extInfRegex = Regex("""#EXTINF:-?\d+\s*(.*?),(.*)""")
    private val attrRegex = Regex("""(\S+?)="([^"]*)"""")

    fun parse(stream: InputStream, sourceFile: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            var pendingName: String? = null
            var pendingLogo: String? = null
            var pendingGroup: String = "Allgemein"
            var index = 0

            reader.forEachLine { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#EXTM3U")) return@forEachLine

                if (line.startsWith("#EXTINF")) {
                    val match = extInfRegex.find(line)
                    if (match != null) {
                        val attrsPart = match.groupValues[1]
                        pendingName = match.groupValues[2].trim()
                        pendingLogo = attrRegex.findAll(attrsPart)
                            .firstOrNull { it.groupValues[1].equals("tvg-logo", ignoreCase = true) }
                            ?.groupValues?.get(2)
                        pendingGroup = attrRegex.findAll(attrsPart)
                            .firstOrNull { it.groupValues[1].equals("group-title", ignoreCase = true) }
                            ?.groupValues?.get(2)?.takeIf { it.isNotBlank() } ?: "Allgemein"
                    }
                } else if (!line.startsWith("#")) {
                    // This is a stream URL line
                    val name = pendingName ?: "Sender ${index + 1}"
                    channels.add(
                        Channel(
                            id = "$sourceFile:$index",
                            name = name,
                            streamUrl = line,
                            logoUrl = pendingLogo,
                            group = pendingGroup,
                            sourceFile = sourceFile
                        )
                    )
                    index++
                    pendingName = null
                    pendingLogo = null
                    pendingGroup = "Allgemein"
                }
            }
        }
        return channels
    }
}

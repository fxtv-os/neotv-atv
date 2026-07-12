package com.neoos.neotv.model

/**
 * A single Live TV channel, parsed from a bundled m3u file.
 * These come ONLY from files in assets/m3u (by design, see repo README) —
 * there is no UI to add or change the m3u source at runtime.
 */
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val group: String,
    val sourceFile: String,
    val tvgId: String? = null
)
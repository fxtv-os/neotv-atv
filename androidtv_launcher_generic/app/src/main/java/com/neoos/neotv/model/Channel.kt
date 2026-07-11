package com.neoos.neotv.model

/**
 * A single Live TV channel, parsed from a bundled .m3u file.
 * These come ONLY from assets/m3u/*.m3u — there is no UI to add/change
 * the m3u source at runtime (by design, see repo README).
 */
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val group: String,
    val sourceFile: String
)
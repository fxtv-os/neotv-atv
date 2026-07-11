package com.neoos.neotv.model

/** A whole streaming app tile (e.g. Disney+, Netflix). */
data class AppTile(
    val label: String,
    val packageName: String,
    val isInstalled: Boolean
)

/**
 * A specific "channel" that lives inside another app (e.g. a Disney+ show/live feed),
 * launched via a deep link Uri into that app. Configured in assets/apps_channels.json.
 */
data class AppChannel(
    val label: String,
    val packageName: String,
    val deepLinkUri: String?,
    val logoUrl: String?,
    val isInstalled: Boolean
)

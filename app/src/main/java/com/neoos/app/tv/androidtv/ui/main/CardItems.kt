package com.neoos.neotv.ui.main

import com.neoos.neotv.R
import com.neoos.neotv.model.AppChannel
import com.neoos.neotv.model.AppTile
import com.neoos.neotv.model.Channel

data class ChannelCardItem(val channel: Channel) : CardItem {
    override val cardTitle = channel.name
    override val cardSubtitle = channel.group
    override val cardImageUrl = channel.logoUrl
    override val cardIconRes = R.drawable.ic_placeholder
}

data class AppCardItem(val app: AppTile) : CardItem {
    override val cardTitle = app.label
    override val cardSubtitle: String? = null
    override val cardImageUrl: String? = null
    override val cardIconRes = R.drawable.ic_placeholder
}

data class AppChannelCardItem(val appChannel: AppChannel) : CardItem {
    override val cardTitle = appChannel.label
    override val cardSubtitle: String? = null
    override val cardImageUrl = appChannel.logoUrl
    override val cardIconRes = R.drawable.ic_placeholder
}

/** Simple action tile, e.g. "Suche starten", "Google Assistant", "Einstellungen öffnen". */
data class ActionCardItem(
    val actionId: String,
    override val cardTitle: String,
    val iconRes: Int
) : CardItem {
    override val cardSubtitle: String? = null
    override val cardImageUrl: String? = null
    override val cardIconRes = iconRes
}

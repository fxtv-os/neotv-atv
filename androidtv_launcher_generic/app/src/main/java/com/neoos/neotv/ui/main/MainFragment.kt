package com.neoos.neotv.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.core.content.ContextCompat
import com.neoos.neotv.R
import com.neoos.neotv.data.AppsRepository
import com.neoos.neotv.data.ChannelRepository
import com.neoos.neotv.model.Channel
import com.neoos.neotv.ui.playback.PlaybackActivity
import com.neoos.neotv.ui.search.SearchActivity
import com.neoos.neotv.ui.settings.SettingsActivity

private const val ACTION_START_SEARCH = "start_search"
private const val ACTION_GOOGLE_ASSISTANT = "google_assistant"
private const val ACTION_OPEN_SETTINGS = "open_settings"

class MainFragment : BrowseSupportFragment() {

    private lateinit var rowsAdapter: ArrayObjectAdapter

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setup()
        buildRows()
        setupClickListener()
    }

    private fun setup() {
        title = getString(R.string.app_name)
        headersState = BrowseSupportFragment.HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = ContextCompat.getColor(requireContext(), R.color.brand_blue)
        searchAffordanceColor = ContextCompat.getColor(requireContext(), R.color.brand_blue)

        // Tapping the built-in search affordance icon also opens our SearchActivity.
        setOnSearchClickedListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }
    }

    private fun buildRows() {
        val ctx = requireContext()
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = CardPresenter()

        // --- Row: Suche ---
        val searchRowAdapter = ArrayObjectAdapter(cardPresenter)
        searchRowAdapter.add(ActionCardItem(ACTION_START_SEARCH, getString(R.string.card_search_start), R.drawable.ic_search))
        searchRowAdapter.add(ActionCardItem(ACTION_GOOGLE_ASSISTANT, getString(R.string.card_google_assistant), R.drawable.ic_assistant))
        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_search)), searchRowAdapter))

        // --- Rows: Live TV (one row per m3u group) ---
        val groups = ChannelRepository.getGroups(ctx)
        for (group in groups) {
            val groupAdapter = ArrayObjectAdapter(cardPresenter)
            ChannelRepository.getByGroup(ctx, group).forEach { groupAdapter.add(ChannelCardItem(it)) }
            val header = if (groups.size == 1) getString(R.string.row_live_tv) else "${getString(R.string.row_live_tv)} · $group"
            rowsAdapter.add(ListRow(HeaderItem(header), groupAdapter))
        }

        // --- Row: Apps ---
        val appsAdapter = ArrayObjectAdapter(cardPresenter)
        AppsRepository.getApps(ctx).forEach { appsAdapter.add(AppCardItem(it)) }
        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_apps)), appsAdapter))

        // --- Row: Channels aus Apps ---
        val appChannelsAdapter = ArrayObjectAdapter(cardPresenter)
        AppsRepository.getAppChannels(ctx).forEach { appChannelsAdapter.add(AppChannelCardItem(it)) }
        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_app_channels)), appChannelsAdapter))

        // --- Row: Einstellungen ---
        val settingsAdapter = ArrayObjectAdapter(cardPresenter)
        settingsAdapter.add(ActionCardItem(ACTION_OPEN_SETTINGS, getString(R.string.card_settings_open), R.drawable.ic_settings))
        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_settings)), settingsAdapter))

        adapter = rowsAdapter
    }

    private fun setupClickListener() {
        onItemViewClickedListener = OnItemViewClickedListener { _: Presenter.ViewHolder?, item: Any?, _: RowPresenter.ViewHolder?, _: Row? ->
            when (item) {
                is ChannelCardItem -> openChannel(item.channel)
                is AppCardItem -> AppsRepository.launch(requireContext(), item.app.packageName)
                is AppChannelCardItem -> AppsRepository.launch(requireContext(), item.appChannel.packageName, item.appChannel.deepLinkUri)
                is ActionCardItem -> handleAction(item.actionId)
            }
        }
    }

    private fun handleAction(actionId: String) {
        when (actionId) {
            ACTION_START_SEARCH -> startActivity(Intent(requireContext(), SearchActivity::class.java))
            ACTION_GOOGLE_ASSISTANT -> launchGoogleAssistant()
            ACTION_OPEN_SETTINGS -> startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
    }

    private fun launchGoogleAssistant() {
        val intent = Intent(Intent.ACTION_VOICE_COMMAND)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        }
    }

    private fun openChannel(channel: Channel) {
        val intent = Intent(requireContext(), PlaybackActivity::class.java).apply {
            putExtra(PlaybackActivity.EXTRA_STREAM_URL, channel.streamUrl)
            putExtra(PlaybackActivity.EXTRA_TITLE, channel.name)
        }
        startActivity(intent)
    }
}

package com.neoos.neotv.ui.search

import android.content.Intent
import android.os.Bundle
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.lifecycleScope
import com.neoos.neotv.R
import com.neoos.neotv.data.AppsRepository
import com.neoos.neotv.data.ChannelRepository
import com.neoos.neotv.model.Channel
import com.neoos.neotv.ui.main.AppCardItem
import com.neoos.neotv.ui.main.CardPresenter
import com.neoos.neotv.ui.main.ChannelCardItem
import com.neoos.neotv.ui.playback.PlaybackActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private var searchJob: Job? = null

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSearchResultProvider(this)
        setOnItemViewClickedListener(OnItemViewClickedListener { _: Presenter.ViewHolder?, item: Any?, _: RowPresenter.ViewHolder?, _: Row? ->
            when (item) {
                is ChannelCardItem -> openChannel(item.channel)
                is AppCardItem -> AppsRepository.launch(requireContext(), item.app.packageName)
            }
        })
    }

    override fun getResultsAdapter(): ObjectAdapter = rowsAdapter

    override fun onQueryTextChange(newQuery: String): Boolean {
        runSearch(newQuery)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        runSearch(query)
        return true
    }

    private fun runSearch(query: String) {
        searchJob?.cancel()
        rowsAdapter.clear()
        if (query.isBlank()) return

        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            val ctx = requireContext()
            val presenter = CardPresenter()

            val channels = ChannelRepository.search(ctx, query)
            if (channels.isNotEmpty()) {
                val adapter = ArrayObjectAdapter(presenter)
                channels.forEach { adapter.add(ChannelCardItem(it)) }
                rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_live_tv)), adapter))
            }

            val apps = AppsRepository.getApps(ctx).filter { it.label.contains(query, ignoreCase = true) }
            if (apps.isNotEmpty()) {
                val adapter = ArrayObjectAdapter(presenter)
                apps.forEach { adapter.add(AppCardItem(it)) }
                rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_apps)), adapter))
            }
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

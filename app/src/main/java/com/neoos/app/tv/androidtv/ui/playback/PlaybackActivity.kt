package com.neoos.neotv.ui.playback

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.neoos.neotv.R
import com.neoos.neotv.util.BaseActivity

/**
 * Full-screen live channel player. Uses Media3 ExoPlayer which natively
 * supports HLS (.m3u8) playback.
 */
class PlaybackActivity : BaseActivity() {

    companion object {
        const val EXTRA_STREAM_URL = "extra_stream_url"
        const val EXTRA_TITLE = "extra_title"
    }

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback)
        playerView = findViewById(R.id.player_view)
        
        // Use Leanback/TV optimized UI for the player
        playerView.controllerShowTimeoutMs = 3000
        playerView.controllerHideOnTouch = true

        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL)
        if (streamUrl.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.playback_error), Toast.LENGTH_LONG).show()
            finish()
            return
        }
        initPlayer(streamUrl)
    }

    private fun initPlayer(streamUrl: String) {
        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        playerView.player = exoPlayer

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(this@PlaybackActivity, getString(R.string.playback_error), Toast.LENGTH_LONG).show()
            }
        })

        val mediaItem = MediaItem.fromUri(streamUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Show subtitle selection on D-Pad Center or Menu key
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_MENU) {
            if (!playerView.isControllerFullyVisible) {
                playerView.showController()
                return true
            }
        }
        
        // Custom subtitle selection trigger (using CAPTIONS or long-press handled here)
        // Note: KEYCODE_SUBTITLE might not be available on all API levels, using 175 literal or checking if defined
        if (keyCode == KeyEvent.KEYCODE_CAPTIONS || keyCode == 175) {
            showSubtitleSelection()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun showSubtitleSelection() {
        val p = player ?: return
        val trackGroups = p.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        
        if (trackGroups.isEmpty()) {
            Toast.makeText(this, getString(R.string.subtitles_none), Toast.LENGTH_SHORT).show()
            return
        }

        val options = mutableListOf<String>()
        options.add(getString(R.string.subtitles_off))
        
        val trackInfo = mutableListOf<Pair<Int, Int>>() // GroupIndex, TrackIndex
        
        trackGroups.forEachIndexed { groupIdx, group ->
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val label = format.language ?: "Track ${i + 1}"
                options.add(label)
                trackInfo.add(groupIdx to i)
            }
        }

        AlertDialog.Builder(this, androidx.leanback.R.style.Theme_Leanback_Browse)
            .setTitle(getString(R.string.subtitles_selection))
            .setItems(options.toTypedArray()) { _, which ->
                if (which == 0) {
                    // Disable subtitles
                    p.trackSelectionParameters = p.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                } else {
                    // Enable selected subtitle
                    val (gIdx, tIdx) = trackInfo[which - 1]
                    p.trackSelectionParameters = p.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(TrackSelectionOverride(trackGroups[gIdx].mediaTrackGroup, tIdx))
                        .build()
                }
            }
            .show()
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}

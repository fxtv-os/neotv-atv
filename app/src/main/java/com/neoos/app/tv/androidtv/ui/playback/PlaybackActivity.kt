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
 * Full-screen live channel player.
 * Includes Timeline and track selection (Audio/Subtitles).
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
        
        // Configuration for TV usage
        playerView.controllerShowTimeoutMs = 5000

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

    @OptIn(UnstableApi::class)
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MENU -> {
                if (!playerView.isControllerFullyVisible) {
                    playerView.showController()
                    return true
                }
            }
            // Audio selection via D-Pad Up
            KeyEvent.KEYCODE_DPAD_UP -> {
                showTrackSelection(C.TRACK_TYPE_AUDIO, "Audio-Sprache wählen")
                return true
            }
            // Subtitle selection via D-Pad Down
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                showTrackSelection(C.TRACK_TYPE_TEXT, getString(R.string.subtitles_selection))
                return true
            }
            // Captions button (if available)
            KeyEvent.KEYCODE_CAPTIONS -> {
                showTrackSelection(C.TRACK_TYPE_TEXT, getString(R.string.subtitles_selection))
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showTrackSelection(trackType: Int, title: String) {
        val p = player ?: return
        val tracks = p.currentTracks
        val groups = tracks.groups.filter { it.type == trackType }
        
        if (groups.isEmpty()) {
            Toast.makeText(this, "Keine Optionen verfügbar", Toast.LENGTH_SHORT).show()
            return
        }

        val options = mutableListOf<String>()
        if (trackType == C.TRACK_TYPE_TEXT) {
            options.add(getString(R.string.subtitles_off))
        }
        
        val trackInfo = mutableListOf<Pair<Int, Int>>() // GroupIndex, TrackIndex
        
        groups.forEachIndexed { groupIdx, group ->
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val label = format.language?.uppercase() ?: "Track ${i + 1}"
                options.add(label)
                trackInfo.add(groupIdx to i)
            }
        }

        AlertDialog.Builder(this, androidx.leanback.R.style.Theme_Leanback_Browse)
            .setTitle(title)
            .setItems(options.toTypedArray()) { _, which ->
                val params = p.trackSelectionParameters.buildUpon()
                
                if (trackType == C.TRACK_TYPE_TEXT && which == 0) {
                    params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                } else {
                    val offset = if (trackType == C.TRACK_TYPE_TEXT) 1 else 0
                    val (gIdx, tIdx) = trackInfo[which - offset]
                    params.setTrackTypeDisabled(trackType, false)
                        .setOverrideForType(TrackSelectionOverride(groups[gIdx].mediaTrackGroup, tIdx))
                }
                p.trackSelectionParameters = params.build()
            }
            .show()
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}

package com.neoos.neotv.ui.playback

import android.os.Bundle
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.neoos.neotv.R
import com.neoos.neotv.util.BaseActivity

/**
 * Full-screen live channel player. Uses Media3 ExoPlayer which natively
 * supports HLS (.m3u8) playback without extra configuration.
 */
class PlaybackActivity : BaseActivity() {

    companion object {
        const val EXTRA_STREAM_URL = "extra_stream_url"
        const val EXTRA_TITLE = "extra_title"
    }

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback)
        playerView = findViewById(R.id.player_view)

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

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}

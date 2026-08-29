package com.memorylane.app.ui

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.memorylane.app.databinding.ActivityMediaViewerBinding

class MediaViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaViewerBinding
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uriString = intent.getStringExtra(EXTRA_URI)
        val isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)

        if (uriString == null) {
            finish()
            return
        }
        val uri = Uri.parse(uriString)

        if (isVideo) {
            binding.imageFull.visibility = android.view.View.GONE
            binding.playerView.visibility = android.view.View.VISIBLE

            val exoPlayer = ExoPlayer.Builder(this).build()
            binding.playerView.player = exoPlayer
            exoPlayer.setMediaItem(ExoMediaItem.fromUri(uri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            player = exoPlayer
        } else {
            binding.playerView.visibility = android.view.View.GONE
            binding.imageFull.visibility = android.view.View.VISIBLE
            Glide.with(this).load(uri).into(binding.imageFull)
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_IS_VIDEO = "extra_is_video"
    }
}

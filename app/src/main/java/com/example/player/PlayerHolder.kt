package com.example.player

import androidx.media3.exoplayer.ExoPlayer

/** Process-wide hand-off between the UI controller and the foreground media service. */
object PlayerHolder {
    @Volatile var player: ExoPlayer? = null
    @Volatile var title: String = "Youseif Player"
    @Volatile var isLive: Boolean = true

    fun attach(player: ExoPlayer, title: String = this.title, isLive: Boolean = this.isLive) {
        this.player = player
        this.title = title.ifBlank { this.title }
        this.isLive = isLive
    }

    fun clear(player: ExoPlayer? = null) {
        if (player == null || this.player === player) this.player = null
    }
}

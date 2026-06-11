package com.englishpractice.app.audio

import android.media.MediaPlayer
import java.io.File

class AudioPlayer {
    private var player: MediaPlayer? = null
    var onCompletion: (() -> Unit)? = null

    fun play(filePath: String) {
        stop()
        if (!File(filePath).exists()) return
        player = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            setOnCompletionListener { onCompletion?.invoke() }
            start()
        }
    }

    fun stop() {
        player?.apply {
            if (isPlaying) stop()
            release()
        }
        player = null
    }

    fun isPlaying(): Boolean = player?.isPlaying == true

    fun release() {
        player?.release()
        player = null
    }
}

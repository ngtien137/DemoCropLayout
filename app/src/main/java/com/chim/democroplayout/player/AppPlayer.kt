package com.chim.democroplayout.player

import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import com.chim.democroplayout.App
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.io.File
import java.lang.Exception

class AppPlayer {

    var playerView: PlayerView? = null
    var duration = 0L
    var currentProgress = 0L
    private var media: SimpleExoPlayer? = null
    var state = State.NOT_READY
    var listener: IAppPlayerListener? = null
    var isIniting = false

    var minCut = 0L
        private set
    var maxCut = 0L
        private set

    private var thread: Thread? = null
    private var runnable = Runnable {
        media?.let {
            try {
                while (true) {
                    if (state == State.PLAYING) {
                        var progress = it.currentPosition
                        var fixProgress = false
                        var isEnd = false
                        if (minCut != 0L || maxCut != duration) {
                            if (progress < minCut) {
                                progress = minCut
                                fixProgress = true
                            } else if (progress >= maxCut) {
                                if (media?.repeatMode == SimpleExoPlayer.REPEAT_MODE_ALL) {
                                    progress = minCut
                                    fixProgress = true
                                } else {
                                    progress = maxCut
                                    fixProgress = true
                                    isEnd = true
                                }
                            }
                        }
                        if (fixProgress)
                            seek(progress, true)
                        currentProgress = progress
                        hander.sendEmptyMessage(0)
                        if (isEnd) {
                            stop()
                            listener?.onVideoEnd()
                        }
                        Thread.sleep(100)
                    }
                }
            } catch (e: Exception) {
            }

        }
    }
    private var hander = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            listener?.onProgressChange(currentProgress)
        }
    }

    fun init(path: String) {
        state = State.NOT_READY
        isIniting = true
        listener?.onLoadStart()
        currentProgress = 0
        val uri = FileProvider.getUriForFile(
            App.self(),
            "${com.chim.democroplayout.BuildConfig.APPLICATION_ID}.provider",
            File(path)
        )
        media = SimpleExoPlayer.Builder(App.self(), DefaultRenderersFactory(App.self()))
            .build()//,trackSelector,loadControl)
        val dataSourceFactory = DefaultDataSourceFactory(
            App.self(),
            Util.getUserAgent(App.self(), "yourApplicationName")
        )
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)

        media?.addListener(object : Player.EventListener {

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                if (playbackState == ExoPlayer.STATE_READY) {
                    if (media != null && isIniting) {
                        isIniting = false
                        if (state == State.NOT_READY) {
                            state = State.PLAYING
                            val duration = media?.duration ?: 0
                            this@AppPlayer.duration = duration
                            if (maxCut == 0L)
                                maxCut = duration
                            listener?.onLoadComplete()
                        }
                    }
                } else if (playbackState == ExoPlayer.STATE_ENDED) {
                    stop()
                    listener?.onVideoEnd()
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                media = null
            }
        })
        media?.volume = 1f
        media?.repeatMode = SimpleExoPlayer.REPEAT_MODE_ALL
        playerView?.player = media
        media?.prepare(mediaSource)
        media?.playWhenReady = true
    }

    fun play() {
        thread?.interrupt()
        thread = null
        state = State.PLAYING
        media?.playWhenReady = true
        thread = Thread(runnable)
        thread?.start()
    }

    fun stop() {
        media?.let {
            seekToMin(false)
            state = State.STOP
            it.playWhenReady = false
            media?.stop()
        }
        thread?.interrupt()
        thread = null
    }

    fun release() {
        stop()
        media?.let {
            it.stop()
            it.release()
            media = null
        }
    }

    fun pause() {
        media?.let {
            state = State.PAUSE
            currentProgress = it.currentPosition
            media?.playWhenReady = false
        }

    }

    fun seek(progress: Long, isPlaying: Boolean = true) {
        currentProgress = progress
        media?.seekTo(progress)
        media?.playWhenReady = isPlaying
        if (isPlaying) {
            state = State.PLAYING
        } else {
            if (state == State.PLAYING) {
                state = State.PAUSE
            }
        }

    }

    fun seekToMin(isPlaying: Boolean = true) {
        seek(minCut, isPlaying)
    }

    fun setRange(minValue: Long, maxValue: Long) {
        this.minCut = minValue
        this.maxCut = maxValue
    }

    fun isPlaying() = state == State.PLAYING
    fun changeRepeat(): Int {
        var mode = SimpleExoPlayer.REPEAT_MODE_OFF
        if (media != null) {
            mode = if (media?.repeatMode == SimpleExoPlayer.REPEAT_MODE_ALL) {
                SimpleExoPlayer.REPEAT_MODE_OFF
            } else {
                SimpleExoPlayer.REPEAT_MODE_ALL
            }
        }
        media?.repeatMode = mode
        return mode
    }

    fun setRepeatMode(repeatMode: Int) {
        media?.repeatMode = repeatMode
    }

    enum class State {
        READY, PLAYING, STOP, PAUSE, NOT_READY
    }

    interface IAppPlayerListener {
        fun onLoadComplete() {}
        fun onLoadStart() {}
        fun onVideoEnd() {}
        fun onProgressChange(progress: Long) {}
    }
}
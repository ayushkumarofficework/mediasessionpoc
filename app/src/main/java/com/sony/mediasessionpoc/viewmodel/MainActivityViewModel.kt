package com.sony.mediasessionpoc.viewmodel


import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.sony.mediasessionpoc.playback.DemoUtil.MIME_TYPE_HLS
import com.sony.mediasessionpoc.playback.mediasession.SonyMediaSessionService
import com.sony.mediasessionpoc.state.PlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivityViewModel : ViewModel() {

    private lateinit var sonyControllerFuture: ListenableFuture<MediaController>
    private var sonyMediaController: MediaController? = null

    private val _playbackState : MutableState<PlaybackState> = mutableStateOf(PlaybackState())
    val playbackState : State<PlaybackState> = _playbackState

    fun createMediaController(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            sonyControllerFuture = MediaController.Builder(
                context,
                SessionToken(context, ComponentName(context, SonyMediaSessionService::class.java)),
            ).buildAsync()
            trySettingControllerToMediaSession()
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun trySettingControllerToMediaSession() {
        try {
            sonyMediaController =
                withContext(Dispatchers.IO) {
                    sonyControllerFuture.get()
                }
        } catch (t: Throwable) {
            Log.w("SonyMediaSession", "Failed to connect to MediaController", t)
            return
        }
        sonyMediaController?.addListener(object  : Listener {
            override fun onTracksChanged(tracks: Tracks) {
                super.onTracksChanged(tracks)
            }

            override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {
                super.onAudioAttributesChanged(audioAttributes)
            }

            override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
                Log.w("SonyMediaSession", "onAvailableCommandsChanged")
                super.onAvailableCommandsChanged(availableCommands)

            }


        })
        withContext(Dispatchers.Main) {
            _playbackState.value = _playbackState.value.copy(mediaControllerCreated = true, mediaController = sonyMediaController)
            val mediaItem = MediaItem.Builder()
                .setUri(
                    "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"
                )
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("HLS (adaptive): Apple 4x3 basic stream (TS/h264/aac)")
                        .setArtist("Arijit Singh")
                        .build()
                )
                .setMimeType(MIME_TYPE_HLS)
                .build()
            sonyMediaController?.setMediaItem(mediaItem)
            sonyMediaController?.play()
            try {
                val sessionCommand = SessionCommand("CHANGE_TRACK",Bundle.EMPTY)
                sonyMediaController?.sendCustomCommand(sessionCommand, Bundle.EMPTY)
                Log.w("SonyMediaSession", "sendCustomCommand "+sessionCommand.customAction+" "+sessionCommand.commandCode)
            } catch (e : Exception) {
                Log.w("SonyMediaSession", "sendCustomCommand ",e)
            }

        }
    }

    fun pausePlayback() {
        sonyMediaController?.pause()
    }
}

package com.sony.mediasessionpoc.viewmodel


import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionUriBuilder
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.common.util.concurrent.ListenableFuture
import com.sony.mediasessionpoc.playback.DemoUtil.MIME_TYPE_HLS
import com.sony.mediasessionpoc.playback.mediasession.SonyMediaSessionService
import com.sony.mediasessionpoc.state.PlaybackState
import com.sony.mediasessionpoc.utilities.MediaType
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
            ).setListener(object : MediaController.Listener {
                override fun onCustomCommand(
                    controller: MediaController,
                    command: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    Log.e("SonyMediaSession","onCustomCommand mediaController "+command.customAction)
                    if("CAST_PLAYER_READY".equals(command.customAction)) {

                        controller.trackSelectionParameters = controller.trackSelectionParameters.buildUpon().clearVideoSizeConstraints().setMaxVideoSize(
                            Int.MAX_VALUE, 540).build()
                    }
                    return super.onCustomCommand(controller, command, args)
                }

                override fun onAvailableSessionCommandsChanged(
                    controller: MediaController,
                    commands: SessionCommands
                ) {
                    Log.e("SonyMediaSession","onAvailableSessionCommandsChanged mediaController")
                }

                override fun onDisconnected(controller: MediaController) {
                    Log.e("SonyMediaSession","onDisconnected mediaController")
                }
            }).buildAsync()
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

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                Log.w("SonyMediaSession", "onPlayWhenReadyChanged ")
            }

            override fun onRenderedFirstFrame() {
                super.onRenderedFirstFrame()
                Log.w("SonyMediaSession", "onRenderedFirstFrame ")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                Log.w("SonyMediaSession", "onPlaybackStateChanged "+playbackState)
//                if(playbackState == 3) {
//                    sonyMediaController?.trackSelectionParameters?.let {
//                        sonyMediaController?.trackSelectionParameters = it.buildUpon().clearVideoSizeConstraints().setMaxVideoSize(
//                            Int.MAX_VALUE,240)?.build()!!
//                    }
//
//                }
            }

            override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
                Log.w("SonyMediaSession", "onTrackSelectionParametersChanged "+parameters.toString())
                super.onTrackSelectionParametersChanged(parameters)
            }


        })
        withContext(Dispatchers.Main) {
            _playbackState.value = _playbackState.value.copy(mediaControllerCreated = true, mediaController = sonyMediaController)
            onMediaTypeChange(_playbackState.value.selectedMediaType)
            try {

                val sessionCommand = SessionCommand("CHANGE_TRACK",Bundle.EMPTY)
                Log.w("SonyMediaSession", "sendCustomCommand "+sessionCommand.customAction+" "+sessionCommand.commandCode)
                sonyMediaController?.sendCustomCommand(sessionCommand, Bundle.EMPTY)
            } catch (e : Exception) {
                Log.w("SonyMediaSession", "sendCustomCommand ",e)
            }

        }
    }

    fun pausePlayback() {
        sonyMediaController?.pause()
    }

    fun selectMediaType(mediaType: MediaType) {
        if(_playbackState.value.selectedMediaType.mediaName != mediaType.mediaName) {
            _playbackState.value = _playbackState.value.copy(selectedMediaType = mediaType)
            onMediaTypeChange(mediaType)
        }
    }

    private fun onMediaTypeChange(mediaType: MediaType) {
        setMediaItemToMediaControllerAndPlay(getMediaItemFOrMediaType(mediaType))
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun getMediaItemFOrMediaType(mediaType: MediaType) : MediaItem {
        return when(mediaType) {
            is MediaType.VodWithoutAds -> {
                MediaItem.Builder()
                    .setUri(
                        "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"
                    )
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("Without Ads HLS (adaptive): Apple 4x3 basic stream (TS/h264/aac)")
                            .setArtist("Arijit Singh")
                            .build()
                    )
                    .setMimeType(MIME_TYPE_HLS)
                    .build()
            }
            is MediaType.LiveWithDaiAds -> {
                MediaItem.Builder()
                    .setUri(ImaServerSideAdInsertionUriBuilder().setAssetKey("sN_IYUG8STe1ZzhIIE_ksA").setFormat(
                        C.CONTENT_TYPE_HLS)
                        .build()
//                        "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"
                    )
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("HLS (adaptive): Apple 4x3 basic stream (TS/h264/aac)")
                            .setArtist("Arijit Singh")
                            .build()
                    )
                    .setMimeType(MIME_TYPE_HLS)
                    .build()
            }
            is MediaType.VodWithAds -> {
                MediaItem.Builder()
                    .setUri(
                        "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"
                    )
                    .setAdsConfiguration(MediaItem.AdsConfiguration.Builder(Uri.parse("https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpremidpostpod&cmsid=496&vid=short_onecue&correlator=")).build())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("With Ads HLS (adaptive): Apple 4x3 basic stream (TS/h264/aac)")
                            .setArtist("Arijit Singh")
                            .build()
                    )
                    .setMimeType(MIME_TYPE_HLS)
                    .build()
            }
        }
    }

    private fun setMediaItemToMediaControllerAndPlay(mediaItem: MediaItem) {
        sonyMediaController?.setMediaItem(mediaItem)
        sonyMediaController?.play()
    }
}

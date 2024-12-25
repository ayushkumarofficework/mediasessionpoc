package com.sony.mediasessionpoc.playback.mediasession

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.android.gms.cast.framework.CastContext
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.sony.mediasessionpoc.playback.playerkeeper.SonyPlayerProvider.getExoPlayer
import com.sony.mediasessionpoc.playback.playerkeeper.SonyPlayerProvider.releasePlayer

class SonyMediaSessionService : MediaSessionService() {

    var mediaSession : MediaSession? = null
    lateinit var exoPlayer : ExoPlayer

    @UnstableApi
    lateinit var castPlayer : CastPlayer

    lateinit var sonyMediaSessionCallback : MediaSession.Callback

    lateinit var mediaControllerInfo : ControllerInfo

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        exoPlayer = getExoPlayer(this)
        castPlayer = CastPlayer(CastContext.getSharedInstance(this, MoreExecutors.directExecutor()).result)
        castPlayer.addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() {
                Log.e("SonyMediaSession","CastPlayer onRenderedFirstFrame")
                super.onRenderedFirstFrame()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.e("SonyMediaSession","CastPlayer onPlaybackStateChanged "+playbackState)
                super.onPlaybackStateChanged(playbackState)
                if(playbackState == 3) {
                    castPlayer.trackSelectionParameters = castPlayer.trackSelectionParameters.buildUpon().clearVideoSizeConstraints().setMaxVideoSize(
                        Int.MAX_VALUE, 540).build()
//                    mediaSession?.sendCustomCommand(mediaControllerInfo, SessionCommand("CAST_PLAYER_READY", Bundle.EMPTY), Bundle.EMPTY)
                }
            }

            override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
                super.onTrackSelectionParametersChanged(parameters)
                Log.e("SonyMediaSession","CastPlayer onTrackSelectionParametersChanged "+parameters.toBundle())
            }

        })
        setCastSessionAvailabilityListener()
        createMediaSessionCallback()
        mediaSession = MediaSession.Builder(this, exoPlayer).setCallback(sonyMediaSessionCallback).build()
    }

    @OptIn(UnstableApi::class)
    private fun setCastSessionAvailabilityListener() {
        castPlayer?.setSessionAvailabilityListener(@OptIn(UnstableApi::class)
        object : SonyCaseSessionAvailablityListener {
            override fun onCastSessionAvailable() {
                Log.e("SonyMediaSession","onCastSessionAvailable")
                if(::exoPlayer.isInitialized) {
                    val mediaItem = exoPlayer.currentMediaItem
                    mediaItem?.let {
                        releasePlayer()
                        mediaSession?.player = castPlayer
                        castPlayer.setMediaItem(mediaItem)
                        castPlayer.playWhenReady = true
                        castPlayer.play()
                    }

                }

            }

            override fun onCastSessionUnavailable() {
                Log.e("SonyMediaSession","onCastSessionUnavailable")
            }

        })
    }


    private fun createMediaSessionCallback() {
        sonyMediaSessionCallback = object : SonyMediaSessionCallback() {
            @UnstableApi
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                Log.e("SonyMediaSession","onConnect ")
                this@SonyMediaSessionService.mediaControllerInfo = controller
                val connectionResult = MediaSession.ConnectionResult.AcceptedResultBuilder(session).setAvailablePlayerCommands(Player.Commands.Builder().addAllCommands().build()).setAvailableSessionCommands(SessionCommands.Builder().add(SessionCommand("CHANGE_TRACK", Bundle.EMPTY)).build())
                return connectionResult.build()
            }

            override fun onDisconnected(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ) {
                Log.e("SonyMediaSession","onDisconnected ")
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                Log.e("SonyMediaSession","onCustomCommand in mediasession "+customCommand.customAction)
                return super.onCustomCommand(session, controller, customCommand, args)
            }

            override fun onPostConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ) {
                Log.e("SonyMediaSession","onPostConnect ")
                session.sendCustomCommand(controller, SessionCommand("CHANGE_TRACK_1", Bundle.EMPTY), Bundle.EMPTY)
                super.onPostConnect(session, controller)
            }


        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if(::exoPlayer.isInitialized == true && exoPlayer.playWhenReady) {
            exoPlayer.pause()
        }
        stopSelf()
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        releasePlayer()
        castPlayer.setSessionAvailabilityListener(null)
        castPlayer.release()
    }


}

open class SonyMediaSessionCallback : MediaSession.Callback {

}

@UnstableApi
open interface SonyCaseSessionAvailablityListener : SessionAvailabilityListener {

}
package com.sony.mediasessionpoc.playback.mediasession

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.android.gms.cast.framework.CastContext
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class SonyMediaSessionService : MediaSessionService() {

    var mediaSession : MediaSession? = null
    lateinit var exoPlayer : ExoPlayer

    @UnstableApi
    lateinit var castPlayer : CastPlayer

    lateinit var sonyMediaSessionCallback : MediaSession.Callback

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
        castPlayer = CastPlayer(CastContext.getSharedInstance(this, MoreExecutors.directExecutor()).result)
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
                        exoPlayer.release()
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
                Log.e("SonyMediaSession","onCustomCommand "+customCommand.customAction)
                return super.onCustomCommand(session, controller, customCommand, args)
            }

            override fun onPostConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ) {
                Log.e("SonyMediaSession","onPostConnect ")
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
        castPlayer.setSessionAvailabilityListener(null)
        castPlayer.release()
    }


}

open class SonyMediaSessionCallback : MediaSession.Callback {

}

@UnstableApi
open interface SonyCaseSessionAvailablityListener : SessionAvailabilityListener {

}
package com.sony.mediasessionpoc.playback.playerkeeper

import android.content.Context
import android.widget.FrameLayout
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionMediaSource

import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

import androidx.media3.ui.PlayerView
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdErrorEvent.AdErrorListener
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener
import com.google.ads.interactivemedia.v3.api.AdsLoader
import com.sony.mediasessionpoc.R
import com.sony.mediasessionpoc.playback.playerkeeper.SonyPlayerProvider.getPlayerView


object SonyPlayerProvider {

    var sonyPlayerContainer : SonyPlayerContainer? = null

    var playerView : PlayerView? = null

    @UnstableApi
    fun getExoPlayer(context: Context) : ExoPlayer {
        if(sonyPlayerContainer == null) {
            sonyPlayerContainer = SonyPlayerContainer()
        }
        if(sonyPlayerContainer?.isPlayerReleased == true) {
            sonyPlayerContainer?.createExoPlayer(context = context)
        }
        return sonyPlayerContainer?.exoPlayer!!
    }

    fun getPlayerView(context: Context) : PlayerView {
        if(playerView == null) {
            playerView = PlayerView(context)
        } else {
            playerView?.adViewGroup?.removeAllViews()
        }
        return playerView!!
    }

    fun releasePlayer() {
        sonyPlayerContainer?.releasePlayer()
    }
}

class SonyPlayerContainer  {
    var exoPlayer : ExoPlayer? = null
    var isPlayerReleased : Boolean = true
    var serverSideAdsLoader : ImaServerSideAdInsertionMediaSource.AdsLoader? = null
    var clientSideAdsLoader : ImaAdsLoader? = null

    @UnstableApi
    fun createExoPlayer(context: Context) {
        val mediaSourceFactory  = DefaultMediaSourceFactory(context)


        serverSideAdsLoader =
            ImaServerSideAdInsertionMediaSource.AdsLoader.Builder(context) { getPlayerView(context) }
                .setAdEventListener(object : AdEventListener {
                    override fun onAdEvent(adEvent: AdEvent) {
                        Log.e("SonyMediaSession","Server Side Ad onAdEvent "+adEvent.type.name)
                    }
                }).setAdErrorListener(object : AdErrorListener {
                    override fun onAdError(adErrorEvent: AdErrorEvent) {
                        Log.e("SonyMediaSession","Server Side Ad onAdError "+adErrorEvent.error.message, adErrorEvent.error.cause)
                    }
                })
                .build()

        val serverSideAdsMediaSourceFactory =
            ImaServerSideAdInsertionMediaSource.Factory(serverSideAdsLoader!!, mediaSourceFactory)
        mediaSourceFactory.setServerSideAdInsertionMediaSourceFactory(serverSideAdsMediaSourceFactory)

        clientSideAdsLoader = ImaAdsLoader.Builder(context)
            .setAdEventListener(object : AdEventListener {
                override fun onAdEvent(adEvent: AdEvent) {
                    Log.e("SonyMediaSession","Client Side Ad onAdEvent "+adEvent.type.name)
                }
            }).setAdErrorListener(object : AdErrorListener {
                override fun onAdError(adErrorEvent: AdErrorEvent) {
                    Log.e("SonyMediaSession","Client Side Ad onAdError "+adErrorEvent.error.message, adErrorEvent.error.cause)
                }
            })

            .build()

        mediaSourceFactory.setLocalAdInsertionComponents(
            { clientSideAdsLoader },
            { getPlayerView(context) })


        exoPlayer = ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).build()
        serverSideAdsLoader?.setPlayer(exoPlayer!!)
        clientSideAdsLoader?.setPlayer(exoPlayer!!)
        isPlayerReleased = false

    }

    fun releasePlayer() {
        exoPlayer?.release()
        isPlayerReleased = true
        serverSideAdsLoader?.release()
        clientSideAdsLoader?.release()
    }

}
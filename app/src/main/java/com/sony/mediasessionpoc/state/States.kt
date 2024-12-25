package com.sony.mediasessionpoc.state

import androidx.media3.session.MediaController
import com.sony.mediasessionpoc.utilities.MediaType
import com.sony.mediasessionpoc.utilities.getListOfAvailableMediaTypes


data class PlaybackState(val mediaControllerCreated : Boolean = false,
                         val mediaController: MediaController? = null,
                         val selectedMediaType: MediaType = MediaType.VodWithoutAds(),
                        val availableMediaTypes : List<MediaType> = getListOfAvailableMediaTypes())
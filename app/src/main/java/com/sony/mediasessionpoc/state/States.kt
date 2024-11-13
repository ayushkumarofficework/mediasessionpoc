package com.sony.mediasessionpoc.state

import androidx.media3.session.MediaController


data class PlaybackState(val mediaControllerCreated : Boolean = false
                         ,val mediaController: MediaController? = null)
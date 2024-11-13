package com.sony.mediasessionpoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.media3.ui.PlayerView
import com.sony.mediasessionpoc.state.PlaybackState
import com.sony.mediasessionpoc.ui.theme.MediaSessionPocTheme
import com.sony.mediasessionpoc.viewmodel.MainActivityViewModel

class MainActivity : ComponentActivity() {

    lateinit var mainActivityViewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivityViewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]
        mainActivityViewModel.createMediaController(this)
        setContent {
            MediaSessionPocTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SonyPlayer(state = mainActivityViewModel.playbackState, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mainActivityViewModel?.pausePlayback()
    }
}


@Composable
fun SonyPlayer(state: State<PlaybackState>, modifier: Modifier) {

    val mediaController = remember(state.value.mediaController) {
        state.value.mediaController
    }
    mediaController?.let {
        AndroidView(factory = {
            val playerView = PlayerView(it)
//            playerView.useController = false
            playerView.player = mediaController
            playerView
        })
    }
}

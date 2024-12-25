package com.sony.mediasessionpoc

import android.os.Bundle
import android.provider.MediaStore.Audio.Media
import android.view.ContextThemeWrapper
import android.view.Menu
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.media3.ui.PlayerView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.sony.mediasessionpoc.playback.playerkeeper.SonyPlayerProvider.getPlayerView
import com.sony.mediasessionpoc.state.PlaybackState
import com.sony.mediasessionpoc.ui.theme.MediaSessionPocTheme
import com.sony.mediasessionpoc.utilities.MediaType
import com.sony.mediasessionpoc.viewmodel.MainActivityViewModel

class MainActivity : AppCompatActivity() {

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
                    Column(modifier = Modifier.fillMaxSize()) {
                        SonyPlayer(state = mainActivityViewModel.playbackState, modifier = Modifier.fillMaxSize())
                        CastingButton(modifier = Modifier)
                        AvailableMediaTypes(mainActivityViewModel.playbackState.value.availableMediaTypes, mainActivityViewModel.playbackState.value.selectedMediaType, modifier = Modifier, onMediaTypeSelection = this@MainActivity::onMediaTypeSelection)
                    }

                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mainActivityViewModel?.pausePlayback()
    }

    private fun onMediaTypeSelection(mediaType: MediaType) {
        mainActivityViewModel.selectMediaType(mediaType = mediaType)
    }

}

@Composable
fun AvailableMediaTypes(availableMediaTypes : List<MediaType>, selectedMediaType : MediaType,onMediaTypeSelection : (MediaType) -> Unit, modifier: Modifier) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items = availableMediaTypes, key = {item : MediaType -> item.mediaName}) {
            AvailableMediaType(mediaType = it, mediaName = it.mediaName, isSelected = it.mediaName.equals(selectedMediaType.mediaName), modifier = modifier, onMediaTypeSelection = onMediaTypeSelection)
        }
    }
}


@Composable
fun AvailableMediaType(mediaType: MediaType, mediaName : String, isSelected : Boolean, modifier: Modifier, onMediaTypeSelection : (MediaType) -> Unit) {
    Box(modifier = modifier.background(color = if(isSelected) Color.Green else Color.White).clickable {
        onMediaTypeSelection(mediaType)
    }.padding(10.dp)) {
        Text(text = mediaName, color = if(isSelected) Color.Red else Color.Black)
    }
}

@Composable
fun CastingButton(modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val button = MediaRouteButton(ctx)
            button.setBackgroundColor(ctx.getColor(R.color.black))
            CastButtonFactory.setUpMediaRouteButton(ctx, button)
            button
        },
        update = {
            it.performClick()
        }
    )
}


@Composable
fun SonyPlayer(state: State<PlaybackState>, modifier: Modifier) {

    val mediaController = remember(state.value.mediaController) {
        state.value.mediaController
    }
    mediaController?.let {
        AndroidView(factory = {
            val playerView = getPlayerView(it)
//            playerView.useController = false
            playerView.player = mediaController
            playerView
        })
    }
}

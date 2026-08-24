package it.girotuttatorino.gtt.ui.intro

import android.content.ContentResolver
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.view.View
import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.viewinterop.AndroidView
import it.girotuttatorino.gtt.R

private const val INTRO_TARGET_DURATION_MILLIS = 3_000
private const val INTRO_FADE_IN_MILLIS = 180
private const val INTRO_FADE_OUT_MILLIS = 220

@Composable
fun AppIntroScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val backgroundColor = colorResource(R.color.intro_background)
    val currentOnFinished = rememberUpdatedState(onFinished)
    var firstFrameRendered by remember { mutableStateOf(false) }
    var playbackCompleted by remember { mutableStateOf(false) }
    val introOverlayAlpha = remember { Animatable(1f) }
    val videoUri = remember(context) {
        Uri.parse(
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://" +
                "${context.packageName}/${R.raw.intro}",
        )
    }
    val videoView = remember(context, videoUri) {
        VideoView(context).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
            setVideoURI(videoUri)
            setOnPreparedListener { player ->
                player.isLooping = false
                player.setVolume(0f, 0f)
                val sourceDurationMillis = player.duration.coerceAtLeast(1)
                val playbackSpeed = sourceDurationMillis.toFloat() / INTRO_TARGET_DURATION_MILLIS
                player.playbackParams = player.playbackParams
                    .setSpeed(playbackSpeed)
                    .setPitch(1f)
                start()
            }
            setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    firstFrameRendered = true
                }
                false
            }
            setOnCompletionListener { playbackCompleted = true }
            setOnErrorListener { _, _, _ ->
                playbackCompleted = true
                true
            }
        }
    }

    LaunchedEffect(firstFrameRendered) {
        if (firstFrameRendered) {
            introOverlayAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(INTRO_FADE_IN_MILLIS),
            )
        }
    }

    LaunchedEffect(playbackCompleted) {
        if (playbackCompleted) {
            introOverlayAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(INTRO_FADE_OUT_MILLIS),
            )
            currentOnFinished.value()
        }
    }

    DisposableEffect(videoView) {
        onDispose(videoView::stopPlayback)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .testTag("app_intro"),
    ) {
        AndroidView(
            factory = { videoView },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(introOverlayAlpha.value)
                .background(backgroundColor),
        )
    }
}

package it.girotuttatorino.gtt.ui.tickets

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.girotuttatorino.gtt.R
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val QR_REFRESH_MILLIS = 15_000L
private val ArtworkShape = RoundedCornerShape(12.dp)

/** The same artwork footprint exposes a white QR reverse only for a valid ticket. */
@Composable
internal fun TicketFlipArtwork(
    imageResource: Int,
    ticketName: String,
    ticketId: String,
    validated: Boolean,
    refreshKey: Any?,
    onQrPayloadRequested: () -> ByteArray?,
    modifier: Modifier = Modifier,
) {
    var showBack by remember(ticketId, validated) { mutableStateOf(false) }
    var qrBitmap by remember(ticketId, validated) { mutableStateOf<Bitmap?>(null) }
    val latestRequest = rememberUpdatedState(onQrPayloadRequested)

    LaunchedEffect(ticketId, validated) {
        if (validated) {
            qrBitmap = withContext(Dispatchers.Default) {
                latestRequest.value()?.let(TicketQrBitmap::create)
            }
        }
    }
    LaunchedEffect(ticketId, validated, showBack, refreshKey) {
        if (!validated || !showBack) return@LaunchedEffect
        while (true) {
            val refreshed = withContext(Dispatchers.Default) {
                latestRequest.value()?.let(TicketQrBitmap::create)
            }
            qrBitmap = refreshed
            if (refreshed == null) {
                showBack = false
                break
            }
            delay(QR_REFRESH_MILLIS)
        }
    }

    fun flip() {
        if (!validated) return
        if (!showBack && qrBitmap == null) {
            qrBitmap = latestRequest.value()?.let(TicketQrBitmap::create)
            if (qrBitmap == null) return
        }
        showBack = !showBack
    }

    val rotation by animateFloatAsState(
        targetValue = if (showBack) 180f else 0f,
        animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing),
        label = "ticketQrFlip",
    )
    // Complete each face at 0° so Android's 3D lighting cannot tint the white reverse.
    val faceRotation = if (rotation < 90f) rotation else rotation - 180f
    val flipAction = if (showBack) {
        stringResource(R.string.ticket_qr_flip_front)
    } else {
        stringResource(R.string.ticket_qr_flip_back)
    }
    val flipModifier = if (validated) {
        Modifier
            .pointerInput(ticketId, showBack, qrBitmap) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onHorizontalDrag = { change, distance ->
                        drag += distance
                        change.consume()
                    },
                    onDragEnd = { if (abs(drag) >= 48.dp.toPx()) flip() },
                )
            }
            .clickable(onClickLabel = flipAction, onClick = ::flip)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(300f / 183f)
            .clip(ArtworkShape)
            .testTag(if (showBack) "ticket_qr_reverse" else "ticket_artwork_front")
            .then(flipModifier)
            .graphicsLayer {
                rotationY = faceRotation
                cameraDistance = 16f * density
            },
        contentAlignment = Alignment.Center,
    ) {
        if (rotation < 90f) {
            Image(
                painter = painterResource(imageResource),
                contentDescription = ticketName,
                modifier = Modifier.matchParentSize().testTag("ticket_artwork_image"),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White)
                    .testTag("ticket_reverse_surface"),
                contentAlignment = Alignment.Center,
            ) {
                val bitmap = qrBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.ticket_qr_description),
                        modifier = Modifier.fillMaxHeight(0.94f).aspectRatio(1f)
                            .testTag("ticket_qr_image"),
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None,
                    )
                }
            }
        }
    }
}

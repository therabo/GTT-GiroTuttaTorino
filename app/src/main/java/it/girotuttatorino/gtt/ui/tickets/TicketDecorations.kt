package it.girotuttatorino.gtt.ui.tickets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.girotuttatorino.gtt.R
import it.girotuttatorino.gtt.ui.theme.GttBlue
import it.girotuttatorino.gtt.ui.theme.GttCyan
import it.girotuttatorino.gtt.ui.theme.GttDarkBlue
import it.girotuttatorino.gtt.ui.theme.GttMagenta
import it.girotuttatorino.gtt.ui.theme.GttOrange
import it.girotuttatorino.gtt.ui.theme.GttYellow

private val SplashGradient = listOf(GttDarkBlue, GttBlue, GttCyan)

@Composable
internal fun SplashTopBar() {
    val statusBarHeight = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp + statusBarHeight),
    ) {
        val enlargedText = LocalDensity.current.fontScale >= 1.3f
        val narrowBar = maxWidth < 360.dp
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - 16.dp.toPx())
                cubicTo(
                    size.width * 0.83f,
                    size.height + 3.dp.toPx(),
                    size.width * 0.64f,
                    size.height - 27.dp.toPx(),
                    size.width * 0.44f,
                    size.height - 11.dp.toPx(),
                )
                cubicTo(
                    size.width * 0.27f,
                    size.height + 2.dp.toPx(),
                    size.width * 0.12f,
                    size.height - 23.dp.toPx(),
                    0f,
                    size.height - 8.dp.toPx(),
                )
                close()
            }
            drawPath(
                path = barPath,
                brush = Brush.linearGradient(
                    colors = SplashGradient,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
            )

            drawCircle(
                color = GttOrange,
                radius = 8.dp.toPx(),
                center = Offset(size.width * 0.79f, size.height - 14.dp.toPx()),
            )
            drawCircle(
                color = GttMagenta,
                radius = 4.dp.toPx(),
                center = Offset(size.width * 0.84f, size.height - 4.dp.toPx()),
            )
            drawCircle(
                color = GttYellow,
                radius = 2.5.dp.toPx(),
                center = Offset(size.width * 0.75f, size.height - 2.dp.toPx()),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = 3.5.dp.toPx(),
                center = Offset(size.width * 0.06f, size.height - 20.dp.toPx()),
            )
            drawCircle(
                color = GttYellow,
                radius = 2.dp.toPx(),
                center = Offset(size.width * 0.22f, size.height - 14.dp.toPx()),
            )
            drawCircle(
                color = GttMagenta,
                radius = 3.dp.toPx(),
                center = Offset(size.width * 0.92f, size.height - 29.dp.toPx()),
            )
            drawCircle(
                color = GttOrange,
                radius = 4.5.dp.toPx(),
                center = Offset(size.width * 0.14f, size.height - 34.dp.toPx()),
            )
            drawCircle(
                color = GttMagenta,
                radius = 2.25.dp.toPx(),
                center = Offset(size.width * 0.18f, size.height - 23.dp.toPx()),
            )
            drawCircle(
                color = GttYellow,
                radius = 1.6.dp.toPx(),
                center = Offset(size.width * 0.11f, size.height - 13.dp.toPx()),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.84f),
                radius = 1.8.dp.toPx(),
                center = Offset(size.width * 0.88f, size.height - 18.dp.toPx()),
            )
            drawCircle(
                color = GttOrange,
                radius = 1.7.dp.toPx(),
                center = Offset(size.width * 0.95f, size.height - 12.dp.toPx()),
            )
        }

        Text(
            text = stringResource(R.string.tickets_title),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    start = 12.dp,
                    top = statusBarHeight + 25.dp,
                    end = 12.dp,
                ),
            color = Color.White,
            fontSize = when {
                enlargedText -> 24.sp
                narrowBar -> 27.sp
                else -> 32.sp
            },
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            maxLines = if (enlargedText) 2 else 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ContentSplashes(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(
            color = GttCyan.copy(alpha = 0.07f),
            radius = 82.dp.toPx(),
            center = Offset(size.width + 12.dp.toPx(), size.height * 0.20f),
        )
        drawCircle(
            color = GttMagenta.copy(alpha = 0.07f),
            radius = 9.dp.toPx(),
            center = Offset(size.width - 28.dp.toPx(), size.height * 0.39f),
        )
        drawCircle(
            color = GttOrange.copy(alpha = 0.08f),
            radius = 5.dp.toPx(),
            center = Offset(size.width - 59.dp.toPx(), size.height * 0.42f),
        )
        drawCircle(
            color = GttCyan.copy(alpha = 0.08f),
            radius = 7.dp.toPx(),
            center = Offset(25.dp.toPx(), size.height * 0.57f),
        )
        drawCircle(
            color = GttOrange.copy(alpha = 0.10f),
            radius = 3.dp.toPx(),
            center = Offset(49.dp.toPx(), size.height * 0.54f),
        )
        drawCircle(
            color = GttMagenta.copy(alpha = 0.08f),
            radius = 4.dp.toPx(),
            center = Offset(16.dp.toPx(), size.height * 0.51f),
        )
        drawCircle(
            color = GttCyan.copy(alpha = 0.10f),
            radius = 2.5.dp.toPx(),
            center = Offset(65.dp.toPx(), size.height * 0.59f),
        )
        drawCircle(
            color = GttMagenta.copy(alpha = 0.07f),
            radius = 6.dp.toPx(),
            center = Offset(size.width - 23.dp.toPx(), size.height * 0.64f),
        )
        drawCircle(
            color = GttOrange.copy(alpha = 0.09f),
            radius = 3.5.dp.toPx(),
            center = Offset(size.width - 54.dp.toPx(), size.height * 0.68f),
        )
        drawCircle(
            color = GttCyan.copy(alpha = 0.08f),
            radius = 2.dp.toPx(),
            center = Offset(size.width - 76.dp.toPx(), size.height * 0.62f),
        )
    }
}

@Composable
internal fun SplashFooter() {
    val navigationBarHeight = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp + navigationBarHeight),
    ) {
        val footerPath = Path().apply {
            moveTo(0f, 12.dp.toPx())
            cubicTo(
                size.width * 0.18f,
                -2.dp.toPx(),
                size.width * 0.34f,
                22.dp.toPx(),
                size.width * 0.52f,
                8.dp.toPx(),
            )
            cubicTo(
                size.width * 0.69f,
                -3.dp.toPx(),
                size.width * 0.84f,
                19.dp.toPx(),
                size.width,
                5.dp.toPx(),
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = footerPath,
            brush = Brush.linearGradient(
                colors = listOf(GttDarkBlue, GttBlue, GttMagenta, GttOrange),
                start = Offset.Zero,
                end = Offset(size.width, 0f),
            ),
        )
        drawCircle(
            color = GttCyan,
            radius = 4.dp.toPx(),
            center = Offset(size.width * 0.18f, 5.dp.toPx()),
        )
        drawCircle(
            color = GttOrange,
            radius = 2.5.dp.toPx(),
            center = Offset(size.width * 0.72f, 3.dp.toPx()),
        )
        drawCircle(
            color = GttYellow,
            radius = 2.dp.toPx(),
            center = Offset(size.width * 0.10f, 11.dp.toPx()),
        )
        drawCircle(
            color = GttMagenta,
            radius = 3.5.dp.toPx(),
            center = Offset(size.width * 0.88f, 9.dp.toPx()),
        )
        drawCircle(
            color = GttCyan,
            radius = 2.dp.toPx(),
            center = Offset(size.width * 0.26f, 14.dp.toPx()),
        )
        drawCircle(
            color = GttYellow,
            radius = 1.5.dp.toPx(),
            center = Offset(size.width * 0.31f, 7.dp.toPx()),
        )
        drawCircle(
            color = GttOrange,
            radius = 2.dp.toPx(),
            center = Offset(size.width * 0.66f, 12.dp.toPx()),
        )
        drawCircle(
            color = GttMagenta,
            radius = 1.5.dp.toPx(),
            center = Offset(size.width * 0.94f, 15.dp.toPx()),
        )
    }
}

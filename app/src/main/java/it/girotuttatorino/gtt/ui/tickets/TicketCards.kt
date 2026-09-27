package it.girotuttatorino.gtt.ui.tickets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.girotuttatorino.gtt.R
import it.girotuttatorino.gtt.ui.theme.GttBlue
import it.girotuttatorino.gtt.ui.theme.GttCyan
import it.girotuttatorino.gtt.ui.theme.GttGreen
import it.girotuttatorino.gtt.ui.theme.GttInk
import it.girotuttatorino.gtt.ui.theme.GttMagenta

internal val TicketShape = RoundedCornerShape(12.dp)

internal enum class TicketBadgeState {
    Available,
    Unavailable,
    Validated,
}

internal fun ticketBadgeState(available: Boolean, validated: Boolean): TicketBadgeState = when {
    !available -> TicketBadgeState.Unavailable
    validated -> TicketBadgeState.Validated
    else -> TicketBadgeState.Available
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TicketCard(
    ticket: TicketItem,
    available: Boolean,
    validated: Boolean,
    remainingValiditySeconds: Long?,
    onLongClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val longPressLabel = stringResource(R.string.hold_for_ticket_details)
    val ticketName = stringResource(ticket.nameResource)
    val shape = TicketShape
    val pressInteractionSource = remember { MutableInteractionSource() }
    val isPressed by pressInteractionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (available && isPressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 700f),
        label = "ticketPressScale",
    )
    val interactionModifier = if (available) {
        Modifier.combinedClickable(
            interactionSource = pressInteractionSource,
            indication = null,
            onClick = {},
            onLongClickLabel = longPressLabel,
            onLongClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            },
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .testTag("ticket_card_${ticket.id}")
            .then(interactionModifier),
    ) {
        Surface(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = if (available) 7.dp else 3.dp,
                    shape = shape,
                )
                .clip(shape),
            shape = shape,
            color = Color.White,
        ) {}

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.6.dp),
        ) {
            val usesCompactLayout = maxWidth < 420.dp
            val stacked = TicketLayoutPolicy.stackListCard(
                contentWidthDp = maxWidth.value,
                fontScale = LocalDensity.current.fontScale,
            )
            if (stacked) {
                val artworkWidth = maxWidth.coerceAtMost(300.dp)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TicketArtwork(
                        imageResource = ticket.imageResource,
                        contentDescription = ticketName,
                        available = available,
                        modifier = Modifier.width(artworkWidth),
                    )
                    TicketActions(
                        ticketName = ticketName,
                        available = available,
                        validated = validated,
                        remainingValiditySeconds = remainingValiditySeconds,
                        durationResource = ticket.durationResource,
                        showHoldHint = false,
                        compact = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        if (usesCompactLayout) 12.dp else 18.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val artworkLayoutModifier = if (usesCompactLayout) {
                        Modifier.weight(0.90f)
                    } else {
                        Modifier.width(244.dp)
                    }
                    TicketArtwork(
                        imageResource = ticket.imageResource,
                        contentDescription = ticketName,
                        available = available,
                        modifier = artworkLayoutModifier,
                    )
                    TicketActions(
                        ticketName = ticketName,
                        available = available,
                        validated = validated,
                        remainingValiditySeconds = remainingValiditySeconds,
                        durationResource = ticket.durationResource,
                        showHoldHint = available && !usesCompactLayout,
                        compact = usesCompactLayout,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketArtwork(
    imageResource: Int,
    contentDescription: String,
    available: Boolean,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(imageResource),
        contentDescription = contentDescription,
        modifier = modifier
            .aspectRatio(300f / 183f)
            .alpha(if (available) 1f else 0.62f)
            .clip(TicketShape)
            .testTag("ticket_list_artwork"),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun TicketActions(
    ticketName: String,
    available: Boolean,
    validated: Boolean,
    remainingValiditySeconds: Long?,
    durationResource: Int,
    showHoldHint: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TicketSummary(
            ticketName = ticketName,
            available = available,
            validated = validated,
            remainingValiditySeconds = remainingValiditySeconds,
            durationResource = durationResource,
            compact = compact,
        )
        if (showHoldHint) {
            Text(
                text = stringResource(R.string.hold_for_ticket_details),
                modifier = Modifier.padding(top = 10.dp),
                color = GttBlue.copy(alpha = 0.62f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
internal fun TicketSummary(
    ticketName: String,
    available: Boolean,
    validated: Boolean,
    remainingValiditySeconds: Long?,
    durationResource: Int,
    compact: Boolean = false,
    uniformExpandedBadgeSize: Boolean = false,
    trailingBadgeContent: (@Composable () -> Unit)? = null,
) {
    val badgeState = ticketBadgeState(available = available, validated = validated)

    Column {
        if (trailingBadgeContent == null) {
            TicketStatusBadge(
                badgeState = badgeState,
                available = available,
                compact = compact,
                uniformExpandedSize = uniformExpandedBadgeSize,
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (TicketLayoutPolicy.stackExpandedControls(
                        contentWidthDp = maxWidth.value,
                        fontScale = LocalDensity.current.fontScale,
                    )
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TicketStatusBadge(
                            badgeState = badgeState,
                            available = available,
                            compact = compact,
                            uniformExpandedSize = true,
                        )
                        trailingBadgeContent()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TicketStatusBadge(
                            badgeState = badgeState,
                            available = available,
                            compact = compact,
                            uniformExpandedSize = true,
                        )
                        trailingBadgeContent()
                    }
                }
            }
        }
        Text(
            text = ticketName,
            modifier = Modifier.padding(top = if (compact) 6.dp else 10.dp),
            color = if (available) GttInk else GttInk.copy(alpha = 0.68f),
            fontSize = if (compact) 17.sp else 23.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = if (compact) 20.sp else 28.sp,
            maxLines = 2,
        )
        if (available) {
            Text(
                text = ticketValidityText(
                    validated,
                    remainingValiditySeconds,
                    durationResource,
                ),
                color = GttBlue.copy(alpha = 0.72f),
                fontSize = if (compact) 12.sp else 14.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun TicketStatusBadge(
    badgeState: TicketBadgeState,
    available: Boolean,
    compact: Boolean,
    uniformExpandedSize: Boolean,
    modifier: Modifier = Modifier,
) {
    val badgeContentColor = when (badgeState) {
        TicketBadgeState.Available -> GttBlue
        TicketBadgeState.Unavailable -> GttMagenta
        TicketBadgeState.Validated -> GttGreen
    }
    val badgeContainerColor = when (badgeState) {
        TicketBadgeState.Available -> GttCyan.copy(alpha = 0.13f)
        TicketBadgeState.Unavailable -> GttMagenta.copy(alpha = 0.10f)
        TicketBadgeState.Validated -> GttGreen.copy(alpha = 0.14f)
    }
    val badgeTextResource = when (badgeState) {
        TicketBadgeState.Available -> R.string.ticket_available
        TicketBadgeState.Unavailable -> R.string.currently_unavailable
        TicketBadgeState.Validated -> R.string.ticket_validated
    }

    Surface(
        modifier = modifier,
        shape = TicketShape,
        color = badgeContainerColor,
    ) {
        if (uniformExpandedSize) {
            UniformExpandedControlContent(
                text = stringResource(badgeTextResource).uppercase(),
                compact = compact,
                color = badgeContentColor,
                raiseText = badgeState == TicketBadgeState.Validated,
            )
        } else {
            Text(
                text = stringResource(badgeTextResource).uppercase(),
                modifier = Modifier
                    .padding(
                        horizontal = when {
                            compact && !available -> 6.dp
                            compact -> 8.dp
                            else -> 10.dp
                        },
                        vertical = if (compact) 4.dp else 5.dp,
                    )
                    .offset(y = if (badgeState == TicketBadgeState.Validated) (-1).dp else 0.dp),
                color = badgeContentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = if (compact) 0.2.sp else 0.5.sp,
                lineHeight = if (compact) 11.sp else 13.sp,
                maxLines = 2,
            )
        }
    }
}

@Composable
internal fun UniformExpandedControlContent(
    text: String,
    compact: Boolean,
    color: Color,
    raiseText: Boolean = false,
) {
    val horizontalPadding = if (compact) 8.dp else 10.dp
    val verticalPadding = if (compact) 4.dp else 5.dp
    val fontSize = 11.sp

    Box(contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.ticket_available).uppercase(),
            modifier = Modifier
                .padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding,
                )
                .clearAndSetSemantics {},
            color = Color.Transparent,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
        )
        Text(
            text = text,
            modifier = Modifier.offset(y = if (raiseText) (-1).dp else 0.dp),
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ticketValidityText(
    validated: Boolean,
    remainingValiditySeconds: Long?,
    durationResource: Int,
): String {
    if (!validated || remainingValiditySeconds == null) {
        return stringResource(durationResource)
    }
    val minutes = remainingValiditySeconds / 60L
    val seconds = remainingValiditySeconds % 60L
    return stringResource(R.string.ticket_countdown, minutes, seconds)
}

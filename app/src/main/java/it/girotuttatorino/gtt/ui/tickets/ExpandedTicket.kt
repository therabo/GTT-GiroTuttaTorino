package it.girotuttatorino.gtt.ui.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.girotuttatorino.gtt.R
import it.girotuttatorino.gtt.nfc.NfcValidationState
import it.girotuttatorino.gtt.ui.theme.GttBlue
import it.girotuttatorino.gtt.ui.theme.GttCyan
import it.girotuttatorino.gtt.ui.theme.GttDarkBlue
import it.girotuttatorino.gtt.ui.theme.GttInk
import it.girotuttatorino.gtt.ui.theme.GttMagenta
import it.girotuttatorino.gtt.ui.theme.GttOrange
import java.text.DateFormat
import java.util.Date

@Composable
internal fun ExpandedTicketCard(
    ticketId: String,
    ticketName: String,
    ticketImageResource: Int,
    durationResource: Int,
    areaResource: Int,
    tripsResource: Int,
    showMetroAccess: Boolean,
    validated: Boolean,
    remainingValiditySeconds: Long?,
    ridesToGo: Int?,
    metroAccessToGo: Int?,
    nfcValidationState: NfcValidationState,
    onResetValidatedTicket: () -> Unit,
    onQrPayloadRequested: () -> ByteArray?,
    modifier: Modifier = Modifier,
) {
    val shape = TicketShape

    Surface(
        modifier = modifier
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = GttCyan.copy(alpha = 0.35f),
                spotColor = GttDarkBlue.copy(alpha = 0.55f),
            )
            .border(
                width = 1.dp,
                color = GttCyan.copy(alpha = 0.30f),
                shape = shape,
            )
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        shape = shape,
        color = Color.Transparent,
    ) {
        BoxWithConstraints(
            modifier = Modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFF0F9FF),
                        Color(0xFFFFFBF2),
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            ),
        ) {
            val usesCompactOverlay = TicketLayoutPolicy.compactOverlay(
                widthDp = maxWidth.value,
                heightDp = maxHeight.value,
                fontScale = LocalDensity.current.fontScale,
            )
            ExpandedTicketContent(
                ticketId = ticketId,
                ticketName = ticketName,
                ticketImageResource = ticketImageResource,
                durationResource = durationResource,
                areaResource = areaResource,
                tripsResource = tripsResource,
                showMetroAccess = showMetroAccess,
                validated = validated,
                remainingValiditySeconds = remainingValiditySeconds,
                ridesToGo = ridesToGo,
                metroAccessToGo = metroAccessToGo,
                nfcValidationState = nfcValidationState,
                onResetValidatedTicket = onResetValidatedTicket,
                onQrPayloadRequested = onQrPayloadRequested,
                compact = usesCompactOverlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (usesCompactOverlay) 17.64.dp else 28.dp),
            )
        }
    }
}

@Composable
private fun ExpandedTicketContent(
    ticketId: String,
    ticketName: String,
    ticketImageResource: Int,
    durationResource: Int,
    areaResource: Int,
    tripsResource: Int,
    showMetroAccess: Boolean,
    validated: Boolean,
    remainingValiditySeconds: Long?,
    ridesToGo: Int?,
    metroAccessToGo: Int?,
    nfcValidationState: NfcValidationState,
    onResetValidatedTicket: () -> Unit,
    onQrPayloadRequested: () -> ByteArray?,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    var showResetConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(validated) {
        if (!validated) showResetConfirmation = false
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmation = false
                        onResetValidatedTicket()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.reset_ticket_confirm),
                        color = GttBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text(
                        text = stringResource(R.string.reset_ticket_cancel),
                        color = GttInk.copy(alpha = 0.70f),
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.reset_ticket_title),
                    color = GttInk,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.reset_ticket_message),
                    color = GttInk.copy(alpha = 0.76f),
                )
            },
            shape = TicketShape,
            containerColor = Color.White,
            modifier = Modifier.testTag("reset_ticket_confirmation"),
        )
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        TicketFlipArtwork(
            imageResource = ticketImageResource,
            ticketName = ticketName,
            ticketId = ticketId,
            validated = validated,
            refreshKey = nfcValidationState,
            onQrPayloadRequested = onQrPayloadRequested,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(if (compact) 10.dp else 16.dp))
        TicketSummary(
            ticketName = ticketName,
            available = true,
            validated = validated,
            remainingValiditySeconds = remainingValiditySeconds,
            durationResource = durationResource,
            compact = compact,
            uniformExpandedBadgeSize = true,
            trailingBadgeContent = if (validated) {
                {
                    ResetValidatedTicketButton(
                        compact = compact,
                        onClick = { showResetConfirmation = true },
                    )
                }
            } else {
                null
            },
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = if (compact) 8.dp else 13.dp),
            color = GttBlue.copy(alpha = 0.12f),
        )
        TicketDetailRow(
            label = stringResource(R.string.ticket_validity_label),
            value = ticketValidityText(
                validated,
                remainingValiditySeconds,
                durationResource,
            ),
            compact = compact,
        )
        TicketDetailRow(
            label = stringResource(R.string.ticket_area_label),
            value = stringResource(areaResource),
            compact = compact,
        )
        TicketDetailRow(
            label = stringResource(R.string.ticket_trips_label),
            value = if (validated && ridesToGo != null) {
                stringResource(R.string.ticket_rides_remaining_value, ridesToGo)
            } else {
                stringResource(tripsResource)
            },
            compact = compact,
        )
        if (validated && showMetroAccess) {
            TicketDetailRow(
                label = stringResource(R.string.ticket_metro_access_label),
                value = stringResource(
                    R.string.ticket_metro_access_value,
                    metroAccessToGo ?: 0,
                ),
                compact = compact,
            )
        }
        NfcValidationBanner(
            state = nfcValidationState,
            compact = compact,
            modifier = Modifier.padding(top = if (compact) 8.dp else 12.dp),
        )
        Text(
            text = stringResource(R.string.tap_outside_to_close_ticket),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = if (compact) 6.dp else 10.dp),
            color = GttBlue.copy(alpha = 0.62f),
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ResetValidatedTicketButton(
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = GttBlue.copy(alpha = 0.18f),
                shape = TicketControlShape,
            )
            .clip(TicketControlShape)
            .testTag("reset_validated_ticket")
            .clickable(
                onClickLabel = stringResource(R.string.reset_validated_ticket),
                onClick = onClick,
            ),
        shape = TicketControlShape,
        color = GttCyan.copy(alpha = 0.13f),
        contentColor = GttBlue,
    ) {
        UniformExpandedControlContent(
            text = stringResource(R.string.regenerate_ticket),
            compact = compact,
            color = GttBlue,
        )
    }
}

@Composable
private fun NfcValidationBanner(
    state: NfcValidationState,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    if (state == NfcValidationState.Inactive) return

    val title: String
    val message: String
    val accent: Color
    when (state) {
        NfcValidationState.Ready -> {
            title = stringResource(R.string.nfc_ready_title)
            message = stringResource(R.string.nfc_ready_message)
            accent = GttBlue
        }
        is NfcValidationState.Validated -> {
            title = stringResource(R.string.nfc_validated_title)
            val validationTime = DateFormat.getTimeInstance(DateFormat.SHORT)
                .format(Date(state.timestampMillis))
            message = stringResource(R.string.nfc_validated_message, validationTime)
            accent = GttCyan
        }
        NfcValidationState.Disabled -> {
            title = stringResource(R.string.nfc_disabled_title)
            message = stringResource(R.string.nfc_disabled_message)
            accent = GttOrange
        }
        NfcValidationState.Unsupported -> {
            title = stringResource(R.string.nfc_unsupported_title)
            message = stringResource(R.string.nfc_unsupported_message)
            accent = GttInk.copy(alpha = 0.70f)
        }
        NfcValidationState.Error -> {
            title = stringResource(R.string.nfc_error_title)
            message = stringResource(R.string.nfc_error_message)
            accent = GttMagenta
        }
        NfcValidationState.Inactive -> return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("nfc_validation_status"),
        shape = TicketShape,
        color = accent.copy(alpha = 0.10f),
        contentColor = GttInk,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 8.dp else 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 36.dp else 42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "NFC",
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = accent,
                    fontSize = if (compact) 13.sp else 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = message,
                    modifier = Modifier.padding(top = 2.dp),
                    color = GttInk.copy(alpha = 0.76f),
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = if (compact) 14.sp else 16.sp,
                )
            }
        }
    }
}

@Composable
private fun TicketDetailRow(
    label: String,
    value: String,
    compact: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 2.dp else 5.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(if (compact) 70.dp else 76.dp),
            color = GttBlue.copy(alpha = 0.72f),
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = GttInk,
            fontSize = if (compact) 13.sp else 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

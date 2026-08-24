package it.girotuttatorino.gtt.nfc.core

/** Structural model for an AEPV V-Token. */
internal object AepVToken {
    const val MAX_LENGTH = 4096
    const val STATE_ISSUED = 1
    const val STATE_VALIDATED = 2

    private const val TOKEN_UID_OFFSET = 4
    private const val TOKEN_UID_SIZE = 8
    private const val DEVICE_UID_OFFSET = 23
    private const val OBJECT_UID_OFFSET = 31
    private const val OBJECT_TYPE_OFFSET = 39
    private const val OBJECT_FORMAT_OFFSET = 40
    private const val PAYLOAD_SIZE_OFFSET = 49
    private const val VALIDATION_SIZE_OFFSET = 53
    private const val TRAILER_SIZE_OFFSET = 55
    private const val PAYLOAD_OFFSET = 57
    private const val VALIDATION_MINIMUM_SIZE = 40
    private const val TRANSIT_OBJECT_TYPE = 2
    private const val TRANSIT_OBJECT_FORMAT = 1
    private const val CONTRACT_PROVIDER_RELATIVE_OFFSET = 21
    private const val CONTRACT_TARIFF_RELATIVE_OFFSET = 22
    private const val MINIMUM_PRODUCT_PAYLOAD_SIZE = 24
    private const val CITY_METRO_USED_RELATIVE_OFFSET = 12
    private const val CITY_METRO_USED_MASK = 0x80
    private const val FLAG_VALIDATED = 0x02
    private const val FLAG_MINUTES_TO_GO_VALID = 0x04
    private const val FLAG_RIDES_TO_GO_VALID = 0x08
    private const val VALIDATION_RIDE_ID_OFFSET = 22
    private val magic = byteArrayOf('A'.code.toByte(), 'E'.code.toByte(), 'P'.code.toByte(), 'V'.code.toByte())

    data class ValidationInfo(
        val flags: Int,
        val firstValidationMinutesSince2016: Long?,
        val lastValidationMinutesSince2016: Long?,
        val minutesToGo: Int?,
        val ridesToGo: Int?,
        val rideId: Long,
    )

    data class ProductIdentity(
        val providerId: Int,
        val tariffId: Int,
    )

    fun basicShape(ticket: ByteArray?): Boolean {
        if (ticket == null || ticket.size !in 57..MAX_LENGTH) return false
        if (!ticket.copyOfRange(0, magic.size).contentEquals(magic)) return false
        val payloadSize = bigUnsignedInt(ticket, PAYLOAD_SIZE_OFFSET)
        val validationSize = bigUnsignedShort(ticket, VALIDATION_SIZE_OFFSET)
        val trailerSize = bigUnsignedShort(ticket, TRAILER_SIZE_OFFSET)
        val expectedSize = PAYLOAD_OFFSET.toLong() + payloadSize + validationSize + trailerSize
        if (payloadSize !in 1..MAX_LENGTH.toLong() || expectedSize != ticket.size.toLong()) {
            return false
        }
        val validationOffset = PAYLOAD_OFFSET + payloadSize.toInt()
        return validationSize >= VALIDATION_MINIMUM_SIZE &&
            ByteCodec.unsigned(ticket[validationOffset]) == 1
    }

    fun isToken(ticket: ByteArray?, expectedDeviceUid: ByteArray?): Boolean =
        basicShape(ticket) && expectedDeviceUid?.size == 8 &&
            ByteCodec.constantTimeEquals(deviceUid(ticket), expectedDeviceUid)

    fun isIssuedToken(ticket: ByteArray?, expectedDeviceUid: ByteArray?): Boolean =
        isToken(ticket, expectedDeviceUid) &&
            !isValidated(ticket)

    fun isValidated(ticket: ByteArray?): Boolean =
        basicShape(ticket) &&
            (ByteCodec.unsigned(
                requireNotNull(ticket)[validationOffset(ticket) + 1],
            ) and FLAG_VALIDATED) != 0

    fun state(ticket: ByteArray?): Int = when {
        !basicShape(ticket) -> -1
        isValidated(ticket) -> STATE_VALIDATED
        else -> STATE_ISSUED
    }

    fun validationInfo(ticket: ByteArray?): ValidationInfo? {
        if (!basicShape(ticket)) return null
        val value = requireNotNull(ticket)
        val offset = validationOffset(value)
        if (value.size < offset + VALIDATION_MINIMUM_SIZE) return null
        val flags = ByteCodec.unsigned(value[offset + 1])
        val firstValidation = bigUnsignedInt(value, offset + 2).takeIf { it != 0L }
        val lastValidation = bigUnsignedInt(value, offset + 6).takeIf { it != 0L }
        val rideId = bigUnsignedInt(value, offset + VALIDATION_RIDE_ID_OFFSET)
        return ValidationInfo(
            flags = flags,
            firstValidationMinutesSince2016 = firstValidation,
            lastValidationMinutesSince2016 = lastValidation,
            minutesToGo = bigUnsignedShort(value, offset + 34)
                .takeIf { flags and FLAG_MINUTES_TO_GO_VALID != 0 },
            ridesToGo = ByteCodec.unsigned(value[offset + 36])
                .takeIf { flags and FLAG_RIDES_TO_GO_VALID != 0 },
            rideId = rideId,
        )
    }

    /**
     * Decodes the remaining metro admission from the City 100-minute physical-document
     * payload. TO Move maps page 3, byte 0, bit 7 to "ticket already used on the metro";
     * page 3 begins 12 bytes after the payload start. The bit is therefore inverted to
     * obtain the remaining-admissions value exposed by the UI.
     */
    fun cityMetroRidesToGo(ticket: ByteArray?): Int? {
        if (!basicShape(ticket)) return null
        val value = requireNotNull(ticket)
        if (payloadSize(value) <= CITY_METRO_USED_RELATIVE_OFFSET) return null
        val metroAlreadyUsed = ByteCodec.unsigned(
            value[PAYLOAD_OFFSET + CITY_METRO_USED_RELATIVE_OFFSET],
        ) and CITY_METRO_USED_MASK != 0
        return if (metroAlreadyUsed) 0 else 1
    }

    fun deviceUid(ticket: ByteArray?): ByteArray? =
        ticket?.takeIf { it.size >= DEVICE_UID_OFFSET + 8 }
            ?.copyOfRange(DEVICE_UID_OFFSET, DEVICE_UID_OFFSET + 8)

    fun tokenUid(ticket: ByteArray?): ByteArray? =
        ticket?.takeIf { it.size >= TOKEN_UID_OFFSET + TOKEN_UID_SIZE }
            ?.copyOfRange(TOKEN_UID_OFFSET, TOKEN_UID_OFFSET + TOKEN_UID_SIZE)

    fun objectUid(ticket: ByteArray?): ByteArray? =
        ticket?.takeIf { it.size >= OBJECT_UID_OFFSET + 8 }
            ?.copyOfRange(OBJECT_UID_OFFSET, OBJECT_UID_OFFSET + 8)

    /**
     * Reads the compact contract discriminator observed in AEP physical-document
     * payload format 1. Commercial names and descriptions are resolved separately
     * by [TicketProduct], just as TO Move resolves them through GetInfoCard/catalog data.
     */
    fun productIdentity(ticket: ByteArray?): ProductIdentity? {
        if (!basicShape(ticket)) return null
        val value = requireNotNull(ticket)
        if (ByteCodec.unsigned(value[OBJECT_TYPE_OFFSET]) != TRANSIT_OBJECT_TYPE ||
            ByteCodec.unsigned(value[OBJECT_FORMAT_OFFSET]) != TRANSIT_OBJECT_FORMAT
        ) {
            return null
        }
        val payloadSize = payloadSize(value)
        if (payloadSize < MINIMUM_PRODUCT_PAYLOAD_SIZE) return null
        val providerId = ByteCodec.unsigned(
            value[PAYLOAD_OFFSET + CONTRACT_PROVIDER_RELATIVE_OFFSET],
        )
        val tariffId = bigUnsignedShort(
            value,
            PAYLOAD_OFFSET + CONTRACT_TARIFF_RELATIVE_OFFSET,
        )
        if (providerId <= 0 || tariffId <= 0) return null
        return ProductIdentity(providerId = providerId, tariffId = tariffId)
    }

    fun validValidatedTransition(before: ByteArray, after: ByteArray, deviceUid: ByteArray): Boolean {
        if (!isToken(before, deviceUid) || !isToken(after, deviceUid)) return false
        if (!isValidated(after)) return false

        if (!sameRange(before, after, TOKEN_UID_OFFSET, TOKEN_UID_OFFSET + TOKEN_UID_SIZE + 2)) {
            return false
        }
        if (!sameRange(before, after, 16, 18)) return false
        if (!sameRange(before, after, 20, 23)) return false
        if (!sameRange(before, after, DEVICE_UID_OFFSET, 41)) return false
        if (!sameRange(before, after, 45, TRAILER_SIZE_OFFSET)) return false
        if (productIdentity(before) != productIdentity(after)) return false

        val validationOffset = validationOffset(after)
        return (validationOffset + 2 until validationOffset + 10).any { after[it] != 0.toByte() }
    }

    fun validationOffset(ticket: ByteArray): Int =
        PAYLOAD_OFFSET + payloadSize(ticket)

    private fun payloadSize(ticket: ByteArray): Int =
        bigUnsignedInt(ticket, PAYLOAD_SIZE_OFFSET).toInt()

    private fun bigUnsignedShort(value: ByteArray, offset: Int): Int =
        (ByteCodec.unsigned(value[offset]) shl 8) or ByteCodec.unsigned(value[offset + 1])

    private fun bigUnsignedInt(value: ByteArray, offset: Int): Long =
        (ByteCodec.unsigned(value[offset]).toLong() shl 24) or
            (ByteCodec.unsigned(value[offset + 1]).toLong() shl 16) or
            (ByteCodec.unsigned(value[offset + 2]).toLong() shl 8) or
            ByteCodec.unsigned(value[offset + 3]).toLong()

    private fun sameRange(left: ByteArray, right: ByteArray, start: Int, end: Int): Boolean =
        left.size >= end && right.size >= end &&
            left.copyOfRange(start, end).contentEquals(right.copyOfRange(start, end))
}

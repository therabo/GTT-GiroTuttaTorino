package it.girotuttatorino.gtt.nfc.core

/** Format-4 verification data for a currently validated AEP V-Token. */
internal object TicketQrPayload {
    private const val HEADER_SIZE = 57
    private const val PAYLOAD_SIZE_OFFSET = 49
    private val header = byteArrayOf('V'.code.toByte(), 'T'.code.toByte(),
        'S'.code.toByte(), 'Q'.code.toByte(), 1, 0, 0, 0, 0, 0, 0, 0)

    fun fromValidatedToken(token: ByteArray?, generatedAtEpochSeconds: Long): ByteArray? {
        if (!AepVToken.isValidated(token) ||
            generatedAtEpochSeconds !in 1..0xFFFF_FFFFL
        ) return null
        val value = requireNotNull(token)
        val payloadSize = ByteCodec.readInt(value, PAYLOAD_SIZE_OFFSET)
        if (payloadSize !in 1..0xFFFF ||
            HEADER_SIZE + payloadSize > value.size
        ) return null

        val timestamp = ByteArray(4).also {
            ByteCodec.putInt(it, 0, generatedAtEpochSeconds.toInt())
        }
        return ByteCodec.concat(
            header,
            timestamp,
            value.copyOfRange(4, 12), // V-Token UID
            value.copyOfRange(20, 22), // system type
            byteArrayOf(0, value[22]), // 16-bit system subtype
            value.copyOfRange(23, 31), // device UID
            value.copyOfRange(31, 39), // object UID
            byteArrayOf(value[39]), // object type
            byteArrayOf(value[51], value[52]), // 16-bit payload length
            value.copyOfRange(HEADER_SIZE, HEADER_SIZE + payloadSize),
            ByteArray(6),
        )
    }
}

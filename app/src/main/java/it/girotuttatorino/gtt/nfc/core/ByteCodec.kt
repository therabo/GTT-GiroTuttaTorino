package it.girotuttatorino.gtt.nfc.core

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

internal object ByteCodec {
    fun unsigned(value: Byte): Int = value.toInt() and 0xFF

    fun unsignedShort(high: Byte, low: Byte): Int =
        (unsigned(high) shl 8) or unsigned(low)

    fun readLong(value: ByteArray, offset: Int): Long {
        var result = 0L
        repeat(Long.SIZE_BYTES) { index ->
            result = (result shl 8) or unsigned(value[offset + index]).toLong()
        }
        return result
    }

    fun readInt(value: ByteArray, offset: Int): Int =
        (unsigned(value[offset]) shl 24) or
            (unsigned(value[offset + 1]) shl 16) or
            (unsigned(value[offset + 2]) shl 8) or
            unsigned(value[offset + 3])

    fun putLong(target: ByteArray, offset: Int, input: Long) {
        var value = input
        for (index in Long.SIZE_BYTES - 1 downTo 0) {
            target[offset + index] = value.toByte()
            value = value ushr 8
        }
    }

    fun putInt(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value ushr 24).toByte()
        target[offset + 1] = (value ushr 16).toByte()
        target[offset + 2] = (value ushr 8).toByte()
        target[offset + 3] = value.toByte()
    }

    fun fromHex(value: String): ByteArray {
        val compact = value.filterNot(Char::isWhitespace)
        require(compact.length % 2 == 0) { "Hex value must contain an even number of chars" }
        return ByteArray(compact.length / 2) { index ->
            compact.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    fun toHex(value: ByteArray?): String = value?.joinToString(separator = "") {
        String.format(Locale.US, "%02X", unsigned(it))
    }.orEmpty()

    fun concat(vararg values: ByteArray): ByteArray {
        val output = ByteArray(values.sumOf(ByteArray::size))
        var offset = 0
        values.forEach { value ->
            value.copyInto(output, destinationOffset = offset)
            offset += value.size
        }
        return output
    }

    fun sha256(value: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value)

    fun shortHash(value: ByteArray): String = toHex(sha256(value).copyOf(8))

    fun utf8(value: String): ByteArray = value.toByteArray(StandardCharsets.UTF_8)

    fun constantTimeEquals(left: ByteArray?, right: ByteArray?): Boolean =
        left != null && right != null && MessageDigest.isEqual(left, right)
}

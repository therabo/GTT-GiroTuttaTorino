package it.girotuttatorino.gtt.nfc.core

import it.girotuttatorino.gtt.BuildConfig
import java.io.ByteArrayOutputStream

internal object HceProtocol {
    const val PRIVATE_AID_HEX = "F04754544150502E4843452E5631"
    const val GTT_AID_HEX = BuildConfig.GTT_HCE_AID
    const val PAGE_SIZE = 96
    const val MAX_FRAGMENT_SIZE = 248

    val SW_OK = ByteCodec.fromHex("9000")
    val SW_WRONG_LENGTH = ByteCodec.fromHex("6700")
    val SW_SECURITY = ByteCodec.fromHex("6982")
    val SW_CONDITIONS = ByteCodec.fromHex("6985")
    val SW_NOT_FOUND = ByteCodec.fromHex("6A82")
    val SW_WRONG_DATA = ByteCodec.fromHex("6A80")
    val SW_PARAMETERS = ByteCodec.fromHex("6A86")
    val SW_INS = ByteCodec.fromHex("6D00")
    val SW_CLA = ByteCodec.fromHex("6E00")

    data class Command(
        val cla: Int,
        val instruction: Int,
        val parameter1: Int,
        val parameter2: Int,
        val data: ByteArray,
    ) {
        val offset: Int get() = (parameter1 shl 8) or parameter2
    }

    fun parse(apdu: ByteArray?): Command? {
        if (apdu == null || apdu.size < 5) return null
        val dataLength = ByteCodec.unsigned(apdu[4])
        val dataEnd = 5 + dataLength
        if (apdu.size != dataEnd && apdu.size != dataEnd + 1) return null
        return Command(
            cla = ByteCodec.unsigned(apdu[0]),
            instruction = ByteCodec.unsigned(apdu[1]),
            parameter1 = ByteCodec.unsigned(apdu[2]),
            parameter2 = ByteCodec.unsigned(apdu[3]),
            data = apdu.copyOfRange(5, dataEnd),
        )
    }

    fun select(aidHex: String = PRIVATE_AID_HEX): ByteArray =
        command(0x00, 0xA4, 0x04, 0x00, ByteCodec.fromHex(aidHex), true)

    fun profile(): ByteArray = command(0x80, 0xA5, 0x10, 0x00, ByteCodec.fromHex("0103000000"), true)

    fun read(
        offset: Int,
        length: Int,
        correlation: ByteArray,
        operationMode: Int = 1,
        readMode: Int = 1,
    ): ByteArray {
        require(correlation.size == 4)
        val data = byteArrayOf(operationMode.toByte(), readMode.toByte(), length.toByte()) + correlation
        return command(0x80, 0xA6, offset ushr 8, offset, data, true)
    }

    fun write(
        offset: Int,
        complete: Boolean,
        correlation: ByteArray,
        fragment: ByteArray,
        writeMode: Int = 1,
    ): ByteArray {
        require(correlation.size == 4)
        val data = byteArrayOf(
            (if (complete) 1 else 0).toByte(),
            writeMode.toByte(),
            fragment.size.toByte(),
        ) + correlation + fragment
        return command(0x80, 0xA7, offset ushr 8, offset, data, false)
    }

    fun fci(aidHex: String, deviceUid: ByteArray, tokenLength: Int): ByteArray {
        require(deviceUid.size == 8)
        require(tokenLength in 1..AepVToken.MAX_LENGTH)
        val aid = ByteCodec.fromHex(aidHex)
        val payload = ByteArrayOutputStream().apply {
            write(0x84)
            write(aid.size)
            write(aid)
            write(0xA5)
            write(0x16)
            write(0xBF)
            write(0x0C)
            write(0x13)
            write(0xC7)
            write(0x08)
            write(deviceUid)
            write(0x53)
            write(0x07)
            write(0x01)
            write(0x01)
            write(tokenLength ushr 8)
            write(tokenLength)
            write(0x10)
            write(0x01)
            write(0x29)
        }.toByteArray()
        return ByteCodec.concat(byteArrayOf(0x6F, payload.size.toByte()), payload, SW_OK)
    }

    fun readResponse(correlation: ByteArray, fragment: ByteArray): ByteArray =
        ByteCodec.concat(
            byteArrayOf(0x80.toByte(), (fragment.size + 5).toByte(), fragment.size.toByte()),
            correlation,
            fragment,
            SW_OK,
        )

    fun writeAcknowledgement(correlation: ByteArray): ByteArray =
        ByteCodec.concat(ByteCodec.fromHex("8004"), correlation, SW_OK)

    fun isSuccessful(response: ByteArray?): Boolean =
        response != null && response.size >= 2 &&
            response[response.lastIndex - 1] == 0x90.toByte() &&
            response[response.lastIndex] == 0.toByte()

    private fun command(
        cla: Int,
        instruction: Int,
        parameter1: Int,
        parameter2: Int,
        data: ByteArray,
        includeExpectedLength: Boolean,
    ): ByteArray {
        require(data.size <= 255)
        return ByteArray(5 + data.size + if (includeExpectedLength) 1 else 0).also { output ->
            output[0] = cla.toByte()
            output[1] = instruction.toByte()
            output[2] = parameter1.toByte()
            output[3] = parameter2.toByte()
            output[4] = data.size.toByte()
            data.copyInto(output, destinationOffset = 5)
        }
    }
}

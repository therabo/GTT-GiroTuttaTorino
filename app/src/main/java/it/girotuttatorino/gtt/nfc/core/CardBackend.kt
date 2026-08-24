package it.girotuttatorino.gtt.nfc.core

internal interface CardBackend {
    fun isValidationSessionActive(): Boolean
    fun deviceUid(): ByteArray
    fun loadTicket(): ByteArray?
    fun compareAndCommit(expected: ByteArray, replacement: ByteArray): Boolean
    fun onCommitted()
    fun trace(event: String)
}

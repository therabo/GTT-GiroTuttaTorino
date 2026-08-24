package it.girotuttatorino.gtt.nfc

internal sealed interface NfcValidationState {
    data object Inactive : NfcValidationState
    data object Unsupported : NfcValidationState
    data object Disabled : NfcValidationState
    data object Ready : NfcValidationState
    data class Validated(val timestampMillis: Long) : NfcValidationState
    data object Error : NfcValidationState
}

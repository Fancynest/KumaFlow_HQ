package com.bearbones.kumaflow.nfc

import kotlinx.coroutines.flow.MutableSharedFlow

object NfcTriggerManager {
    val showNfcTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
}

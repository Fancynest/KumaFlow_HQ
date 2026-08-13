package com.bearbones.kumaflow.nfc

data class CardInfo(
    val cardType: String,
    val cardNumber: String,
    val balance: Long?
)

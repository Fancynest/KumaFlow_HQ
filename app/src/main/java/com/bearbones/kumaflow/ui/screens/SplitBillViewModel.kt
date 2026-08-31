package com.bearbones.kumaflow.ui.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import kotlin.math.ceil

enum class SplitMode {
    SAMA_RATA,
    TAHU_DIRI
}

data class SubItem(
    val id: String = UUID.randomUUID().toString(),
    val amountStr: String = ""
)

data class CustomSplitItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val amounts: List<SubItem> = listOf(SubItem())
) {
    val totalPrice: Long
        get() = amounts.sumOf {
            val clean = it.amountStr.replace("[^0-9]".toRegex(), "")
            clean.toLongOrNull() ?: 0L
        }
}

data class SplitBillState(
    val totalBillStr: String = "",
    val numberOfPeople: String = "1",
    val mode: SplitMode = SplitMode.SAMA_RATA,
    val taxPercentage: String = "0",
    val customItems: List<CustomSplitItem> = listOf(
        CustomSplitItem(name = "Saya")
    )
) {
    val totalBill: Long
        get() {
            val manual = totalBillStr.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
            return if (manual > 0L) {
                manual
            } else if (mode == SplitMode.TAHU_DIRI) {
                customItems.sumOf { it.totalPrice }
            } else {
                0L
            }
        }
}

data class PersonShare(
    val id: String,
    val name: String,
    val subtotal: Long,
    val taxAmount: Long,
    val finalAmount: Long
)

data class TahuDiriResult(
    val itemizedShares: List<PersonShare>,
    val subtotalAll: Long,
    val totalTax: Long,
    val grandTotal: Long,
    val remainingPerPerson: Long = 0L,
    val remainingPeopleCount: Int = 0
)

class SplitBillViewModel : ViewModel() {
    private val _state = MutableStateFlow(SplitBillState())
    val state: StateFlow<SplitBillState> = _state.asStateFlow()

    fun setTotalBill(amount: Long) {
        _state.update {
            it.copy(totalBillStr = if (amount > 0L) amount.toString() else "")
        }
    }

    fun setTotalBillStr(str: String) {
        val clean = str.replace("[^0-9]".toRegex(), "")
        _state.update { it.copy(totalBillStr = clean) }
    }

    fun resetState() {
        _state.value = SplitBillState()
    }

    fun setNumberOfPeople(countStr: String) {
        val clean = countStr.replace("[^0-9]".toRegex(), "")
        _state.update { it.copy(numberOfPeople = clean) }
    }

    fun setMode(mode: SplitMode) {
        _state.update { it.copy(mode = mode) }
    }

    fun setTaxPercentage(tax: String) {
        val clean = tax.replace("[^0-9]".toRegex(), "")
        _state.update { it.copy(taxPercentage = clean) }
    }

    fun addPerson(defaultName: String? = null) {
        _state.update { s ->
            val friendNum = s.customItems.size
            val name = defaultName ?: if (friendNum == 0) "Saya" else "Teman $friendNum"
            s.copy(customItems = s.customItems + CustomSplitItem(name = name))
        }
    }

    fun removePerson(personId: String) {
        _state.update { s ->
            s.copy(customItems = s.customItems.filter { it.id != personId })
        }
    }

    fun updatePersonName(personId: String, newName: String) {
        _state.update { s ->
            s.copy(customItems = s.customItems.map {
                if (it.id == personId) it.copy(name = newName) else it
            })
        }
    }

    fun addAmountToPerson(personId: String) {
        _state.update { s ->
            s.copy(customItems = s.customItems.map { person ->
                if (person.id == personId) {
                    person.copy(amounts = person.amounts + SubItem())
                } else {
                    person
                }
            })
        }
    }

    fun updatePersonAmount(personId: String, amountId: String, newAmountStr: String) {
        val clean = newAmountStr.replace("[^0-9]".toRegex(), "")
        _state.update { s ->
            s.copy(customItems = s.customItems.map { person ->
                if (person.id == personId) {
                    person.copy(amounts = person.amounts.map { sub ->
                        if (sub.id == amountId) sub.copy(amountStr = clean) else sub
                    })
                } else {
                    person
                }
            })
        }
    }

    fun removePersonAmount(personId: String, amountId: String) {
        _state.update { s ->
            s.copy(customItems = s.customItems.map { person ->
                if (person.id == personId) {
                    val filtered = person.amounts.filter { it.id != amountId }
                    person.copy(amounts = if (filtered.isEmpty()) listOf(SubItem()) else filtered)
                } else {
                    person
                }
            })
        }
    }

    // Math & rounding logic
    private fun roundUpToHundreds(amount: Double): Long {
        return (ceil(amount / 100.0) * 100).toLong()
    }

    fun calculateEqualSplit(): Long {
        val s = _state.value
        val people = s.numberOfPeople.toIntOrNull() ?: 1
        if (people <= 0) return 0L
        val cleanBill = s.totalBillStr.replace("[^0-9]".toRegex(), "").toDoubleOrNull() ?: 0.0
        if (cleanBill <= 0.0) return 0L
        val taxPct = s.taxPercentage.toDoubleOrNull() ?: 0.0
        val finalBill = cleanBill * (1.0 + (taxPct / 100.0))
        return roundUpToHundreds(finalBill / people)
    }

    fun calculateTahuDiriSplit(): TahuDiriResult {
        val s = _state.value
        val taxPct = s.taxPercentage.toDoubleOrNull() ?: 0.0

        val itemized = s.customItems.map { person ->
            val subtotal = person.totalPrice
            val taxAmount = (subtotal * (taxPct / 100.0)).toLong()
            val finalPrice = roundUpToHundreds(subtotal * (1.0 + (taxPct / 100.0)))
            PersonShare(
                id = person.id,
                name = person.name.ifBlank { "Peserta" },
                subtotal = subtotal,
                taxAmount = taxAmount,
                finalAmount = finalPrice
            )
        }

        val subtotalAll = itemized.sumOf { it.subtotal }
        val grandTotal = itemized.sumOf { it.finalAmount }
        val totalTax = maxOf(0L, grandTotal - subtotalAll)

        return TahuDiriResult(
            itemizedShares = itemized,
            subtotalAll = subtotalAll,
            totalTax = totalTax,
            grandTotal = grandTotal
        )
    }
}

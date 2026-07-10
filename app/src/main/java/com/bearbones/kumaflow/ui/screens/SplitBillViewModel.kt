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

data class CustomSplitItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val price: Long = 0L
)

data class SplitBillState(
    val totalBill: Long = 0L,
    val numberOfPeople: String = "2",
    val mode: SplitMode = SplitMode.SAMA_RATA,
    val taxPercentage: String = "15",
    val customItems: List<CustomSplitItem> = emptyList()
)

class SplitBillViewModel : ViewModel() {
    private val _state = MutableStateFlow(SplitBillState())
    val state: StateFlow<SplitBillState> = _state.asStateFlow()

    fun setTotalBill(amount: Long) {
        _state.update { it.copy(totalBill = amount) }
    }

    fun resetState() {
        _state.value = SplitBillState()
    }

    fun setNumberOfPeople(countStr: String) {
        // Only allow numbers
        if (countStr.all { it.isDigit() }) {
            _state.update { it.copy(numberOfPeople = countStr) }
        }
    }

    fun setMode(mode: SplitMode) {
        _state.update { it.copy(mode = mode) }
    }

    fun setTaxPercentage(tax: String) {
        if (tax.all { it.isDigit() }) {
            _state.update { it.copy(taxPercentage = tax) }
        }
    }

    fun addCustomItem() {
        _state.update {
            it.copy(customItems = it.customItems + CustomSplitItem(name = "Person ${it.customItems.size + 1}"))
        }
    }

    fun updateCustomItemName(id: String, newName: String) {
        _state.update { state ->
            val updated = state.customItems.map { if (it.id == id) it.copy(name = newName) else it }
            state.copy(customItems = updated)
        }
    }

    fun updateCustomItemPrice(id: String, newPriceStr: String) {
        val cleanStr = newPriceStr.replace("[^0-9]".toRegex(), "")
        val newPrice = cleanStr.toLongOrNull() ?: 0L
        _state.update { state ->
            val updated = state.customItems.map { if (it.id == id) it.copy(price = newPrice) else it }
            state.copy(customItems = updated)
        }
    }

    fun removeCustomItem(id: String) {
        _state.update { state ->
            state.copy(customItems = state.customItems.filter { it.id != id })
        }
    }

    // Mathematical logic
    
    private fun roundUpToHundreds(amount: Double): Long {
        return (ceil(amount / 100.0) * 100).toLong()
    }

    fun calculateEqualSplit(): Long {
        val s = _state.value
        val people = s.numberOfPeople.toIntOrNull() ?: 1
        if (people <= 0) return 0L
        val taxPct = s.taxPercentage.toDoubleOrNull() ?: 0.0
        val finalBill = s.totalBill * (1 + (taxPct / 100.0))
        return roundUpToHundreds(finalBill / people)
    }

    data class TahuDiriResult(
        val itemizedShares: List<Pair<String, Long>>,
        val remainingPerPerson: Long,
        val remainingPeopleCount: Int
    )

    fun calculateTahuDiriSplit(): TahuDiriResult {
        val s = _state.value
        val totalPeople = s.numberOfPeople.toIntOrNull() ?: 1
        val taxPct = s.taxPercentage.toDoubleOrNull() ?: 0.0

        val itemized = s.customItems.map { item ->
            val finalPrice = item.price * (1 + (taxPct / 100.0))
            // We DO NOT round up the custom item immediately to avoid compounding rounding errors, 
            // OR we do round it up so they pay in round numbers? 
            // The spec says: "The user who paid the initial bill must NEVER lose money on decimals."
            // Rounding up every individual share is the safest way to ensure no loss.
            val roundedPrice = roundUpToHundreds(finalPrice)
            item.name to roundedPrice
        }

        val totalCustomPrice = itemized.sumOf { it.second }
        val finalTotalBill = s.totalBill * (1 + (taxPct / 100.0))
        val remainingBill = finalTotalBill.toLong() - totalCustomPrice

        val remainingPeopleCount = maxOf(0, totalPeople - s.customItems.size)
        
        val remainingPerPerson = if (remainingPeopleCount > 0 && remainingBill > 0) {
            roundUpToHundreds(remainingBill.toDouble() / remainingPeopleCount)
        } else {
            0L
        }

        return TahuDiriResult(itemized, remainingPerPerson, remainingPeopleCount)
    }
}

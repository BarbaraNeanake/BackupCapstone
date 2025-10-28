package com.example.smartparking.ui.liveparkingpage

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartparking.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SlotRect(
    val id: String,
    val xPct: Float,
    val yPct: Float,
    val wPct: Float,
    val hPct: Float,
    val occupied: Boolean,
    val accessible: Boolean = false
)

data class Lot(
    val name: String,
    @DrawableRes val imageRes: Int,
    val slots: List<SlotRect>
) {
    val used: Int get() = slots.count { it.occupied }
    val free: Int get() = slots.size - used
}

data class LiveUiState(
    val loading: Boolean = true,
    val lots: List<Lot> = emptyList(),
    val error: String? = null
)

class LiveParkingViewModel : ViewModel() {

    private val _ui = MutableStateFlow(LiveUiState())
    val ui: StateFlow<LiveUiState> = _ui

    init { reload() }

    /** Placeholder: nanti ganti panggil repository (REST/WebSocket) */
    fun reload() {
        viewModelScope.launch {
            _ui.value = LiveUiState(loading = true)
            try {
                delay(250) // simulasi fetch
                _ui.value = LiveUiState(
                    loading = false,
                    lots = listOf(denahAksesUtama())
                )
            } catch (e: Exception) {
                _ui.value = LiveUiState(loading = false, error = e.message ?: "Unknown error")
            }
        }
    }

    /** Preset koordinat yang “pas” untuk res/drawable/liveparkingmap.png */
    private fun denahAksesUtama(): Lot {
        return Lot(
            name = "Akses Utama FT UGM",
            imageRes = R.drawable.liveparkingmap,
            slots = listOf(
                // empat slot kiri–tengah
                SlotRect("S1", xPct = 0.17f, yPct = 0.42f, wPct = 0.11f, hPct = 0.46f, occupied = false),
                SlotRect("S2", xPct = 0.35f, yPct = 0.42f, wPct = 0.11f, hPct = 0.46f, occupied = true),
                SlotRect("S3", xPct = 0.53f, yPct = 0.42f, wPct = 0.11f, hPct = 0.46f, occupied = false),
                SlotRect("S4", xPct = 0.71f, yPct = 0.42f, wPct = 0.11f, hPct = 0.46f, occupied = false),

                // satu slot biasa kanan
                SlotRect("S5", xPct = 0.84f, yPct = 0.42f, wPct = 0.07f, hPct = 0.46f, occupied = true),

                // slot disabilitas besar (biru) paling kanan
                SlotRect("D1", xPct = 0.91f, yPct = 0.38f, wPct = 0.08f, hPct = 0.55f, occupied = false, accessible = true)
            )
        )
    }
}

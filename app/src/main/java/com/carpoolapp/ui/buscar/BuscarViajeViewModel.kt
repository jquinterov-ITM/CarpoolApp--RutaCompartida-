package com.carpoolapp.ui.buscar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.usecase.BuscarViajesUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BuscarUiState {
    object Idle : BuscarUiState()
    data class Resultado(val viajes: List<Viaje>) : BuscarUiState()
    data class Error(val mensaje: String) : BuscarUiState()
}

@HiltViewModel
class BuscarViajeViewModel @Inject constructor(
    private val buscarViajesUseCase: BuscarViajesUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<BuscarUiState>(BuscarUiState.Idle)
    val uiState: StateFlow<BuscarUiState> = _uiState.asStateFlow()

    fun buscar(destino: String) {
        val usuarioId = auth.currentUser?.uid ?: return
        if (destino.isBlank()) {
            _uiState.value = BuscarUiState.Idle
            return
        }
        viewModelScope.launch {
            buscarViajesUseCase(usuarioId, destino)
                .catch { e ->
                    _uiState.value = BuscarUiState.Error(e.message ?: "Error al buscar")
                }
                .collect { viajes ->
                    _uiState.value = BuscarUiState.Resultado(viajes)
                }
        }
    }
}

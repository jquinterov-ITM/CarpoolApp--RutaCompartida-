package com.carpoolapp.ui.mis_viajes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.repository.ViajeRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MisViajesUiState {
    object Loading : MisViajesUiState()
    data class Success(
        val comoConductor: List<Viaje>,
        val comoPasajero: List<Viaje>
    ) : MisViajesUiState()
    data class Error(val mensaje: String) : MisViajesUiState()
}

@HiltViewModel
class MisViajesViewModel @Inject constructor(
    private val viajeRepository: ViajeRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<MisViajesUiState>(MisViajesUiState.Loading)
    val uiState: StateFlow<MisViajesUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val comoConductor = mutableListOf<Viaje>()
                viajeRepository.getViajesPorConductor(uid).collect { viajes ->
                    comoConductor.clear()
                    comoConductor.addAll(viajes)
                    val comoPasajero = viajeRepository.getViajesComoPasajero(uid)
                    _uiState.value = MisViajesUiState.Success(comoConductor, comoPasajero)
                }
            } catch (e: Exception) {
                _uiState.value = MisViajesUiState.Error(e.message ?: "Error al cargar viajes")
            }
        }
    }
}

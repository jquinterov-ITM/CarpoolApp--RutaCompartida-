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
import android.util.Log
import kotlinx.coroutines.delay

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
        val uid = auth.currentUser?.uid
        Log.d("MisViajesVM", "cargar() uid=${uid}")
        if (uid == null) {
            _uiState.value = MisViajesUiState.Error("Inicia sesion para ver viajes")
            return
        }
        viewModelScope.launch {
            // timeout fallback: if after 8s still loading, show error so UI doesn't stay bloqueada
            launch {
                delay(8000)
                if (_uiState.value is MisViajesUiState.Loading) {
                    Log.w("MisViajesVM", "Timeout cargando viajes para uid=$uid")
                    _uiState.value = MisViajesUiState.Error("No se pudo cargar viajes. Reintenta.")
                }
            }
            try {
                val comoConductor = mutableListOf<Viaje>()
                viajeRepository.getViajesPorConductor(uid).collect { viajes ->
                    Log.d("MisViajesVM", "getViajesPorConductor emitted ${viajes.size} viajes for uid=$uid")
                    comoConductor.clear()
                    comoConductor.addAll(viajes)
                    val comoPasajero = viajeRepository.getViajesComoPasajero(uid)
                    Log.d("MisViajesVM", "getViajesComoPasajero returned ${comoPasajero.size} viajes for uid=$uid")
                    _uiState.value = MisViajesUiState.Success(comoConductor, comoPasajero)
                }
            } catch (e: Exception) {
                Log.w("MisViajesVM", "Error cargando viajes", e)
                _uiState.value = MisViajesUiState.Error(e.message ?: "Error al cargar viajes")
            }
        }
    }
}

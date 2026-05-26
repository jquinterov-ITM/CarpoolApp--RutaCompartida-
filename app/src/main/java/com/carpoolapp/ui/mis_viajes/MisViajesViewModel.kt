package com.carpoolapp.ui.mis_viajes

import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.repository.ViajeRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject
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
            // Cancel previous collectors before starting new ones.
            try {
                _conductorJob?.cancel()
            } catch (_: Exception) {}
            try {
                _createdEventsJob?.cancel()
            } catch (_: Exception) {}

            val uid = auth.currentUser?.uid
            Log.d("MisViajesVM", "cargar() uid=${uid}")
            if (uid == null) {
                _uiState.value = MisViajesUiState.Error("Inicia sesion para ver viajes")
                return
            }

            // timeout fallback: if after 8s still loading, show error so UI doesn't stay bloqueada
            viewModelScope.launch {
                delay(8000)
                if (_uiState.value is MisViajesUiState.Loading) {
                    Log.w("MisViajesVM", "Timeout cargando viajes para uid=$uid")
                    _uiState.value = MisViajesUiState.Error("No se pudo cargar viajes. Reintenta.")
                }
            }

            val comoConductor = mutableListOf<Viaje>()

            // Collector para eventos creados (optimistic updates) en coroutine independiente
            _createdEventsJob = viewModelScope.launch {
                viajeRepository.createdEvents()
                    .catch { e -> Log.w("MisViajesVM", "createdEvents flow error", e) }
                    .collect { nuevo ->
                        try {
                            if (nuevo.conductorId == uid) {
                                Log.d("MisViajesVM", "Received created event for uid=$uid id=${nuevo.id}")
                                if (comoConductor.none { it.id == nuevo.id }) {
                                    comoConductor.add(0, nuevo)
                                    val comoPasajeroNow = try { viajeRepository.getViajesComoPasajero(uid) } catch (e: Exception) { emptyList<Viaje>() }
                                    _uiState.value = MisViajesUiState.Success(comoConductor.toList(), comoPasajeroNow)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("MisViajesVM", "Error procesando created event", e)
                        }
                    }
            }

            // Collector para los viajes del conductor (real-time) en coroutine independiente
            _conductorJob = viewModelScope.launch {
                viajeRepository.getViajesPorConductor(uid)
                    .catch { e -> Log.w("MisViajesVM", "getViajesPorConductor flow error", e); emit(emptyList()) }
                    .collect { viajes ->
                        try {
                            Log.d("MisViajesVM", "getViajesPorConductor emitted ${'$'}{viajes.size} viajes for uid=$uid")
                            comoConductor.clear()
                            comoConductor.addAll(viajes)
                            val comoPasajero = try { viajeRepository.getViajesComoPasajero(uid) } catch (e: Exception) { emptyList<Viaje>() }
                            Log.d("MisViajesVM", "getViajesComoPasajero returned ${'$'}{comoPasajero.size} viajes for uid=$uid")
                            _uiState.value = MisViajesUiState.Success(comoConductor.toList(), comoPasajero)
                        } catch (e: Exception) {
                            Log.w("MisViajesVM", "Error procesando viajes", e)
                        }
                    }
            }
    }

    private var _collectJob: Job? = null
    private var _conductorJob: Job? = null
    private var _createdEventsJob: Job? = null
}

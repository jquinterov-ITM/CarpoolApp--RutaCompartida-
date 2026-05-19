package com.carpoolapp.ui.solicitudes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carpoolapp.domain.model.Solicitud
import com.carpoolapp.domain.usecase.AceptarSolicitudUseCase
import com.carpoolapp.domain.repository.SolicitudRepository
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.repository.ViajeRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SolicitudesUiState {
    object Loading : SolicitudesUiState()
    data class Success(val viaje: Viaje, val solicitudes: List<Solicitud>) : SolicitudesUiState()
    data class Error(val mensaje: String) : SolicitudesUiState()
}

@HiltViewModel
class SolicitudesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val solicitudRepository: SolicitudRepository,
    private val aceptarSolicitudUseCase: AceptarSolicitudUseCase,
    private val viajeRepository: ViajeRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val tripId: String = savedStateHandle["tripId"] ?: ""

    private val _uiState = MutableStateFlow<SolicitudesUiState>(SolicitudesUiState.Loading)
    val uiState: StateFlow<SolicitudesUiState> = _uiState.asStateFlow()

    init {
        cargarSolicitudes()
    }

    private fun cargarSolicitudes() {
        viewModelScope.launch {
            viajeRepository.getFeed(auth.currentUser?.uid ?: "")
                .catch { }
                .collect { viajes ->
                    val viaje = viajes.find { it.id == tripId } ?: return@collect
                    solicitudRepository.getSolicitudesPorViaje(tripId)
                        .catch { }
                        .collect { solicitudes ->
                            _uiState.value = SolicitudesUiState.Success(viaje, solicitudes)
                        }
                }
        }
    }

    fun aceptar(solicitudId: String) {
        viewModelScope.launch {
            try {
                aceptarSolicitudUseCase(tripId, solicitudId)
            } catch (e: Exception) {
                _uiState.value = SolicitudesUiState.Error(e.message ?: "Error al aceptar")
            }
        }
    }

    fun rechazar(solicitudId: String) {
        viewModelScope.launch {
            try {
                solicitudRepository.rechazar(tripId, solicitudId)
            } catch (e: Exception) {
                _uiState.value = SolicitudesUiState.Error(e.message ?: "Error al rechazar")
            }
        }
    }
}

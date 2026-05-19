package com.carpoolapp.ui.detalle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carpoolapp.domain.model.Solicitud
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.usecase.EnviarSolicitudUseCase
import com.carpoolapp.domain.repository.SolicitudRepository
import com.carpoolapp.domain.repository.ViajeRepository
import com.carpoolapp.domain.usecase.GetFeedUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DetalleUiState {
    object Loading : DetalleUiState()
    data class Success(val viaje: Viaje, val solicitudes: List<Solicitud>) : DetalleUiState()
    object EnviandoSolicitud : DetalleUiState()
    object SolicitudExitosa : DetalleUiState()
    data class Error(val mensaje: String) : DetalleUiState()
}

@HiltViewModel
class ViajeDetalleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val viajeRepository: ViajeRepository,
    private val solicitudRepository: SolicitudRepository,
    private val enviarSolicitudUseCase: EnviarSolicitudUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val tripId: String = savedStateHandle["tripId"] ?: ""
    private val esConductor: Boolean = savedStateHandle["esConductor"] ?: false

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Loading)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    init {
        cargarDetalle()
    }

    private fun cargarDetalle() {
        viewModelScope.launch {
            viajeRepository.getFeed(auth.currentUser?.uid ?: "")
                .catch { e ->
                    _uiState.value = DetalleUiState.Error(e.message ?: "Error al cargar")
                }
                .collect { viajes ->
                    val viaje = viajes.find { it.id == tripId }
                    if (viaje != null) {
                        solicitudRepository.getSolicitudesPorViaje(tripId)
                            .catch { }
                            .collect { solicitudes ->
                                _uiState.value = DetalleUiState.Success(viaje, solicitudes)
                            }
                    }
                }
        }
    }

    fun enviarSolicitud() {
        val usuario = auth.currentUser ?: return
        val nombre = usuario.displayName ?: usuario.email?.substringBefore("@") ?: ""
        viewModelScope.launch {
            _uiState.value = DetalleUiState.EnviandoSolicitud
            try {
                enviarSolicitudUseCase(
                    tripId,
                    Solicitud(
                        pasajeroId = usuario.uid,
                        pasajeroNombre = nombre
                    )
                )
                _uiState.value = DetalleUiState.SolicitudExitosa
            } catch (e: Exception) {
                _uiState.value = DetalleUiState.Error(e.message ?: "Error al enviar solicitud")
            }
        }
    }
}

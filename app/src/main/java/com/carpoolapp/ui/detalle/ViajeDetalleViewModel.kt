package com.carpoolapp.ui.detalle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carpoolapp.domain.model.Solicitud
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.usecase.EnviarSolicitudUseCase
import com.carpoolapp.domain.usecase.FinalizarViajeUseCase
import com.carpoolapp.domain.repository.SolicitudRepository
import com.carpoolapp.domain.repository.ViajeRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DetalleUiState {
    object Loading : DetalleUiState()
    data class Success(
        val viaje: Viaje,
        val solicitudes: List<Solicitud>,
        val esConductor: Boolean,
        val yaSolicito: Boolean = false
    ) : DetalleUiState()
    object EnviandoSolicitud : DetalleUiState()
    object SolicitudExitosa : DetalleUiState()
    object Finalizando : DetalleUiState()
    object Cancelando : DetalleUiState()
    data class Error(val mensaje: String) : DetalleUiState()
}

@HiltViewModel
class ViajeDetalleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val viajeRepository: ViajeRepository,
    private val solicitudRepository: SolicitudRepository,
    private val enviarSolicitudUseCase: EnviarSolicitudUseCase,
    private val finalizarViajeUseCase: FinalizarViajeUseCase,
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
            android.util.Log.d("ViajeDetalle", "Cargando viaje: $tripId")
            val viaje = viajeRepository.getViajePorId(tripId)
            if (viaje != null) {
                android.util.Log.d("ViajeDetalle", "Viaje cargado: ${viaje.conductorNombre}, descripcion: ${viaje.descripcion}")
                val esConductorReal = viaje.conductorId == auth.currentUser?.uid
                val yaSolicito = viaje.pasajeroIds.contains(auth.currentUser?.uid)
                android.util.Log.d("ViajeDetalle", "esConductor: $esConductorReal, yaSolicito: $yaSolicito")
                
                solicitudRepository.getSolicitudesPorViaje(tripId)
                    .catch { e ->
                        android.util.Log.w("ViajeDetalle", "Error en flow solicitudes: ${e.message}")
                        _uiState.value = DetalleUiState.Success(
                            viaje = viaje,
                            solicitudes = emptyList(),
                            esConductor = esConductorReal,
                            yaSolicito = yaSolicito
                        )
                    }
                    .collect { solicitudes ->
                        android.util.Log.d("ViajeDetalle", "Solicitudes cargadas: ${solicitudes.size}")
                        
                        _uiState.value = DetalleUiState.Success(
                            viaje = viaje,
                            solicitudes = solicitudes,
                            esConductor = esConductorReal,
                            yaSolicito = yaSolicito
                        )
                    }
            } else {
                android.util.Log.e("ViajeDetalle", "Viaje no encontrado: $tripId")
                _uiState.value = DetalleUiState.Error("Viaje no encontrado")
            }
        }
    }
    
    fun cancelarViaje() {
        viewModelScope.launch {
            _uiState.value = DetalleUiState.Cancelando
            try {
                viajeRepository.cancelarViaje(tripId)
                cargarDetalle()
            } catch (e: Exception) {
                _uiState.value = DetalleUiState.Error(e.message ?: "Error al cancelar viaje")
            }
        }
    }

    fun enviarSolicitud() {
        val usuario = auth.currentUser ?: return
        val nombre = usuario.displayName ?: usuario.email?.substringBefore("@") ?: ""
        val email = usuario.email ?: ""
        
        viewModelScope.launch {
            _uiState.value = DetalleUiState.EnviandoSolicitud
            try {
                val result = enviarSolicitudUseCase(
                    tripId,
                    Solicitud(
                        tripId = tripId,
                        pasajeroId = usuario.uid,
                        pasajeroNombre = nombre,
                        pasajeroEmail = email
                    )
                )
                result.fold(
                    onSuccess = { _uiState.value = DetalleUiState.SolicitudExitosa },
                    onFailure = { error ->
                        _uiState.value = DetalleUiState.Error(error.message ?: "Error al enviar solicitud")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = DetalleUiState.Error(e.message ?: "Error al enviar solicitud")
            }
        }
    }

    fun aceptarSolicitud(solicitudId: String) {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid
                android.util.Log.d("ViajeDetalleVM", "=== ACEPTAR SOLICITUD ===")
                android.util.Log.d("ViajeDetalleVM", "solicitudId: $solicitudId, tripId: $tripId, usuario: $uid")
                
                if (solicitudId.isBlank()) {
                    _uiState.value = DetalleUiState.Error("ID de solicitud vacío")
                    return@launch
                }
                if (tripId.isBlank()) {
                    _uiState.value = DetalleUiState.Error("ID de viaje vacío")
                    return@launch
                }
                
                solicitudRepository.aceptar(tripId, solicitudId)
                android.util.Log.d("ViajeDetalleVM", "Solicitud aceptada correctamente")
                cargarDetalle()
            } catch (e: Exception) {
                android.util.Log.e("ViajeDetalleVM", "Error al aceptar: ${e.message}", e)
                _uiState.value = DetalleUiState.Error("Error al aceptar: ${e.message ?: "Error desconocido"}")
            }
        }
    }

    fun rechazarSolicitud(solicitudId: String) {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid
                android.util.Log.d("ViajeDetalleVM", "=== RECHAZAR SOLICITUD ===")
                android.util.Log.d("ViajeDetalleVM", "solicitudId: $solicitudId, tripId: $tripId, usuario: $uid")
                
                if (solicitudId.isBlank()) {
                    _uiState.value = DetalleUiState.Error("ID de solicitud vacío")
                    return@launch
                }
                if (tripId.isBlank()) {
                    _uiState.value = DetalleUiState.Error("ID de viaje vacío")
                    return@launch
                }
                
                solicitudRepository.rechazar(tripId, solicitudId)
                android.util.Log.d("ViajeDetalleVM", "Solicitud rechazada correctamente")
                cargarDetalle()
            } catch (e: Exception) {
                android.util.Log.e("ViajeDetalleVM", "Error al rechazar: ${e.message}", e)
                _uiState.value = DetalleUiState.Error("Error al rechazar: ${e.message ?: "Error desconocido"}")
            }
        }
    }

    fun finalizarViaje() {
        viewModelScope.launch {
            _uiState.value = DetalleUiState.Finalizando
            try {
                finalizarViajeUseCase(tripId)
                cargarDetalle()
            } catch (e: Exception) {
                _uiState.value = DetalleUiState.Error(e.message ?: "Error al finalizar viaje")
            }
        }
    }

    fun cancelarSolicitud() {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val solicitudes = solicitudRepository.getSolicitudesPorViaje(tripId).first()
                val solicitud = solicitudes.find { it.pasajeroId == uid && it.estado == com.carpoolapp.domain.model.SolicitudEstado.PENDIENTE }
                if (solicitud != null) {
                    solicitudRepository.cancelarSolicitud(tripId, solicitud.id)
                }
            } catch (e: Exception) {
                _uiState.value = DetalleUiState.Error(e.message ?: "Error al cancelar solicitud")
            }
        }
    }
}

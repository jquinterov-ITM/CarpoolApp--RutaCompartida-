package com.carpoolapp.ui.publicar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carpoolapp.domain.model.TipoViaje
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.model.ViajeEstado
import com.carpoolapp.domain.repository.UsuarioRepository
import com.carpoolapp.domain.usecase.PublicarViajeUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PublicarUiState {
    object Idle : PublicarUiState()
    object Publicando : PublicarUiState()
    data class Exitoso(val viajeId: String) : PublicarUiState()
    data class Error(val mensaje: String) : PublicarUiState()
}

@HiltViewModel
class PublicarViajeViewModel @Inject constructor(
    private val publicarViajeUseCase: PublicarViajeUseCase,
    private val auth: FirebaseAuth,
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PublicarUiState>(PublicarUiState.Idle)
    val uiState: StateFlow<PublicarUiState> = _uiState.asStateFlow()

    fun publicar(
        origen: String,
        destino: String,
        asientos: Int,
        asientosTotales: Int,
        precio: Double,
        descripcion: String,
        tipo: TipoViaje,
        fechaHora: Long = 0L
    ) {
        val usuario = auth.currentUser ?: return
        val nombre = usuario.displayName ?: usuario.email?.substringBefore("@") ?: ""

        viewModelScope.launch {
            _uiState.value = PublicarUiState.Publicando
            try {
                val usuarioData = usuarioRepository.getUsuario(usuario.uid).first()
                val viaje = Viaje(
                    conductorId = usuario.uid,
                    conductorNombre = nombre,
                    origen = origen,
                    destino = destino,
                    asientosDisponibles = asientos,
                    asientosTotales = asientosTotales,
                    precio = precio,
                    descripcion = descripcion,
                    fechaHora = fechaHora,
                    tipo = tipo,
                    estado = if (tipo == TipoViaje.INMEDIATO) ViajeEstado.ACTIVO else ViajeEstado.PROGRAMADO,
                    vehiculoConductor = usuarioData?.vehiculo
                )
                val id = publicarViajeUseCase(viaje)
                _uiState.value = PublicarUiState.Exitoso(id)
            } catch (e: Exception) {
                _uiState.value = PublicarUiState.Error(e.message ?: "Error al publicar viaje")
            }
        }
    }
}

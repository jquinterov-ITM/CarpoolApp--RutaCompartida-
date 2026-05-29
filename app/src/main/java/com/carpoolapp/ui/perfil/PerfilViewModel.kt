package com.carpoolapp.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carpoolapp.domain.model.Usuario
import com.carpoolapp.domain.model.Vehiculo
import com.carpoolapp.domain.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PerfilUiState {
    object Loading : PerfilUiState()
    data class Success(val usuario: Usuario) : PerfilUiState()
    data class Error(val mensaje: String) : PerfilUiState()
    object VehiculoActualizado : PerfilUiState()
    object FotoActualizada : PerfilUiState()
    object NombreActualizado : PerfilUiState()
    object ConductorActualizado : PerfilUiState()
}

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<PerfilUiState>(PerfilUiState.Loading)
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()
    
    // Flow separado para mensajes de feedback (no interrumpe el estado Success)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    init {
        cargarPerfil()
    }

    private fun cargarPerfil() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            usuarioRepository.getUsuario(uid)
                .catch { e ->
                    _uiState.value = PerfilUiState.Error(e.message ?: "Error al cargar perfil")
                }
                .collect { usuario ->
                    if (usuario != null) {
                        _uiState.value = PerfilUiState.Success(usuario)
                    }
                }
        }
    }

    fun actualizarVehiculo(vehiculo: Vehiculo) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                usuarioRepository.actualizarVehiculo(uid, vehiculo)
                _feedbackMessage.value = "✅ Vehículo actualizado correctamente"
                kotlinx.coroutines.delay(3000)
                _feedbackMessage.value = null
            } catch (e: Exception) {
                _feedbackMessage.value = "❌ Error: ${e.message ?: "Error al actualizar"}"
            }
        }
    }

    fun actualizarFotoUrl(fotoUrl: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                android.util.Log.d("PerfilViewModel", "Guardando foto de ${fotoUrl.length} caracteres para usuario $uid")
                usuarioRepository.actualizarFotoUrl(uid, fotoUrl)
                android.util.Log.d("PerfilViewModel", "Foto guardada en Firestore - esperando snapshot listener...")
                
                // NO llamamos cargarPerfil() - el snapshot listener ya está activo desde init
                // y automáticamente emitirá los datos actualizados cuando Firestore se actualice
                // Solo mostramos el mensaje de éxito
                _feedbackMessage.value = "✅ Foto de perfil actualizada"
                kotlinx.coroutines.delay(3000)
                _feedbackMessage.value = null
            } catch (e: Exception) {
                android.util.Log.e("PerfilViewModel", "Error al guardar foto: ${e.message}", e)
                _feedbackMessage.value = "❌ Error: ${e.message ?: "Error desconocido"}"
            }
        }
    }

    fun actualizarNombre(nombre: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                usuarioRepository.actualizarNombre(uid, nombre)
                // Actualizar el estado Success con el nuevo nombre
                val currentState = _uiState.value
                if (currentState is PerfilUiState.Success) {
                    _uiState.value = PerfilUiState.Success(currentState.usuario.copy(nombre = nombre))
                }
                _feedbackMessage.value = "✅ Nombre actualizado correctamente"
                kotlinx.coroutines.delay(3000)
                _feedbackMessage.value = null
            } catch (e: Exception) {
                _feedbackMessage.value = "❌ Error: ${e.message ?: "Error al actualizar nombre"}"
            }
        }
    }

    fun toggleEsConductor(esConductor: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                usuarioRepository.actualizarEsConductor(uid, esConductor)
                _feedbackMessage.value = "✅ Tipo de usuario actualizado"
                kotlinx.coroutines.delay(3000)
                _feedbackMessage.value = null
            } catch (e: Exception) {
                _feedbackMessage.value = "❌ Error: ${e.message ?: "Error al actualizar"}"
            }
        }
    }

    fun cerrarSesion() {
        auth.signOut()
    }
}

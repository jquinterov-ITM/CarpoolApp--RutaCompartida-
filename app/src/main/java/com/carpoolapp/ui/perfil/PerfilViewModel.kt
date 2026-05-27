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
    object ConductorActualizado : PerfilUiState()
}

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<PerfilUiState>(PerfilUiState.Loading)
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

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
                _uiState.value = PerfilUiState.VehiculoActualizado
            } catch (e: Exception) {
                _uiState.value = PerfilUiState.Error(e.message ?: "Error al actualizar")
            }
        }
    }

    fun actualizarFotoUrl(fotoUrl: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                usuarioRepository.actualizarFotoUrl(uid, fotoUrl)
                _uiState.value = PerfilUiState.FotoActualizada
            } catch (e: Exception) {
                _uiState.value = PerfilUiState.Error(e.message ?: "Error al actualizar foto")
            }
        }
    }

    fun toggleEsConductor(esConductor: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                usuarioRepository.actualizarEsConductor(uid, esConductor)
                _uiState.value = PerfilUiState.ConductorActualizado
            } catch (e: Exception) {
                _uiState.value = PerfilUiState.Error(e.message ?: "Error al actualizar")
            }
        }
    }

    fun cerrarSesion() {
        auth.signOut()
    }
}

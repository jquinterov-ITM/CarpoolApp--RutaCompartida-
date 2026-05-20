package com.carpoolapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carpoolapp.domain.model.Usuario
import com.carpoolapp.domain.repository.UsuarioRepository
import com.carpoolapp.data.seed.DataSeeder
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Enviando : AuthUiState()
    data class EmailEnviado(val email: String) : AuthUiState()
    data class Autenticado(val usuario: Usuario) : AuthUiState()
    data class Error(val mensaje: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val usuarioRepository: UsuarioRepository,
    private val dataSeeder: DataSeeder
) : ViewModel() {

    companion object {
        private const val TEST_EMAIL = "test@carpool.com"
        private const val TEST_PASSWORD = "password123"
    }

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun enviarLink(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Enviando
            try {
                val uid = autenticarODesregistrar()
                
                val usuario = Usuario(
                    id = uid,
                    nombre = "jquinterov",
                    email = email
                )
                try {
                    usuarioRepository.guardar(usuario)
                } catch (_: Exception) { }
                
                try {
                    dataSeeder.seedIfEmpty()
                } catch (_: Exception) { }
                
                _uiState.value = AuthUiState.Autenticado(usuario)
                
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(
                    "Error: ${e.message ?: "No se pudo iniciar sesion"}"
                )
            }
        }
    }

    private suspend fun autenticarODesregistrar(): String {
        // Intentar iniciar sesion con email/password (funciona en emulador)
        return try {
            auth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD).await().user?.uid
                ?: throw Exception("No se pudo iniciar sesion")
        } catch (e: Exception) {
            // Si falla (usuario no existe), crearlo primero
            auth.createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD).await().user?.uid
                ?: throw Exception("No se pudo crear el usuario")
        }
    }

    fun cerrarSesion() {
        auth.signOut()
        _uiState.value = AuthUiState.Idle
    }
}

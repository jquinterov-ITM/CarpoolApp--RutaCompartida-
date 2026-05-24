package com.carpoolapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carpoolapp.data.seed.DataSeeder
import com.carpoolapp.domain.model.Usuario
import com.carpoolapp.domain.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        auth.setLanguageCode("es")
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Enviando
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Usuario nulo")
                val nombre = user.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
                val usuario = Usuario(id = user.uid, nombre = nombre, email = email)
                try { usuarioRepository.guardar(usuario) } catch (_: Exception) {}
                try { dataSeeder.seedIfEmpty() } catch (_: Exception) {}
                _uiState.value = AuthUiState.Autenticado(usuario)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(mensajeFirebaseAuth(e, "correo y contraseña"))
            }
        }
    }

    fun registerWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Enviando
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Usuario nulo")
                val nombre = user.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
                val usuario = Usuario(id = user.uid, nombre = nombre, email = email)
                try { usuarioRepository.guardar(usuario) } catch (_: Exception) {}
                try { dataSeeder.seedIfEmpty() } catch (_: Exception) {}
                _uiState.value = AuthUiState.Autenticado(usuario)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(mensajeFirebaseAuth(e, "registro con correo y contraseña"))
            }
        }
    }

    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Enviando
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: throw Exception("Usuario nulo")
                val nombre = user.displayName?.takeIf { it.isNotBlank() } ?: user.email?.substringBefore("@") ?: "Usuario"
                val email = user.email ?: ""
                val usuario = Usuario(id = user.uid, nombre = nombre, email = email)
                try { usuarioRepository.guardar(usuario) } catch (_: Exception) {}
                try { dataSeeder.seedIfEmpty() } catch (_: Exception) {}
                _uiState.value = AuthUiState.Autenticado(usuario)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(mensajeFirebaseAuth(e, "Google"))
            }
        }
    }

    private fun mensajeFirebaseAuth(error: Exception, metodo: String): String {
        val mensaje = error.message.orEmpty()
        val operacionNoPermitida = mensaje.contains("This operation is not allowed", ignoreCase = true) ||
            mensaje.contains("operation is not allowed", ignoreCase = true)

        if (operacionNoPermitida) {
            return "Firebase no tiene habilitado $metodo en Authentication. Activa Email/Password y Google en Firebase Console > Authentication > Sign-in method."
        }

        return "Error $metodo: ${mensaje.ifBlank { "No se pudo completar el inicio de sesión" }}"
    }

    fun cerrarSesion() {
        auth.signOut()
        _uiState.value = AuthUiState.Idle
    }
}

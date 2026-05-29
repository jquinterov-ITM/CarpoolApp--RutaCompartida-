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
    data class RegistroExitoso(val email: String) : AuthUiState()
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
                try { dataSeeder.seedRequestForUser(user.uid) } catch (_: Exception) {}
                _uiState.value = AuthUiState.RegistroExitoso(email)
            } catch (e: Exception) {
                val mensaje = cuandoError(e, "registro")
                android.util.Log.e("AuthViewModel", "Error registro: ${e.message}", e)
                _uiState.value = AuthUiState.Error(mensaje)
            }
        }
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
                try { dataSeeder.seedRequestForUser(user.uid) } catch (_: Exception) {}
                _uiState.value = AuthUiState.Autenticado(usuario)
            } catch (e: Exception) {
                val mensaje = cuandoError(e, "inicio de sesión")
                android.util.Log.e("AuthViewModel", "Error login: ${e.message}", e)
                _uiState.value = AuthUiState.Error(mensaje)
            }
        }
    }

    private fun cuandoError(error: Exception, operacion: String): String {
        val mensaje = error.message.orEmpty()
        
        return when {
            mensaje.contains("already in use", ignoreCase = true) ->
                "Este correo ya tiene una cuenta. ¿Quieres iniciar sesión en vez de crear una?"
            
            mensaje.contains("weak password", ignoreCase = true) ->
                "Contraseña muy débil. Usa al menos 6 caracteres."
            
            mensaje.contains("invalid email", ignoreCase = true) ->
                "El formato del correo no es válido. Verifica que tenga @ y dominio."
            
            mensaje.contains("network", ignoreCase = true) ||
            mensaje.contains("connection", ignoreCase = true) ->
                "No hay conexión a internet. Verifica tu conexión e intenta de nuevo."
            
            mensaje.contains("operation not allowed", ignoreCase = true) ->
                "El método de autenticación no está habilitado en Firebase. Contacta al administrador."
            
            mensaje.contains("too many requests", ignoreCase = true) ->
                "Demasiados intentos. Espera unos minutos e intenta de nuevo."
            
            mensaje.contains("expired", ignoreCase = true) ->
                "Esta credencial ha expirado. Intenta de nuevo."
            
            // Login fallido - puede ser usuario no existe O contraseña incorrecta
            mensaje.contains("wrong password", ignoreCase = true) ||
            mensaje.contains("credential is incorrect", ignoreCase = true) ||
            mensaje.contains("invalid credential", ignoreCase = true) ||
            mensaje.contains("user not found", ignoreCase = true) ||
            mensaje.contains("no user record", ignoreCase = true) ->
                "Correo o contraseña incorrectos. Si no tienes cuenta, regístrate primero."
            
            else -> "Error en $operacion: ${mensaje.ifBlank { "No se pudo completar la operación" }}"
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
                val fotoUrl = user.photoUrl?.toString()
                val usuario = Usuario(id = user.uid, nombre = nombre, email = email, fotoUrl = fotoUrl)
                try { 
                    usuarioRepository.guardar(usuario)
                    if (!fotoUrl.isNullOrBlank()) {
                        usuarioRepository.actualizarFotoUrl(user.uid, fotoUrl)
                    }
                } catch (_: Exception) {}
                try { dataSeeder.seedIfEmpty() } catch (_: Exception) {}
                try { dataSeeder.seedRequestForUser(user.uid) } catch (_: Exception) {}
                _uiState.value = AuthUiState.Autenticado(usuario)
            } catch (e: Exception) {
                val mensaje = cuandoError(e, "Google")
                android.util.Log.e("AuthViewModel", "Error Google: ${e.message}", e)
                _uiState.value = AuthUiState.Error(mensaje)
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

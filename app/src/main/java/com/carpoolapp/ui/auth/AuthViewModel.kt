package com.carpoolapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carpoolapp.domain.model.Usuario
import com.carpoolapp.domain.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.actionCodeSettings
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
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val actionCodeSettings = actionCodeSettings {
        url = "https://carpoolapp.page.link/finishSignUp"
        handleCodeInApp = true
        setAndroidPackageName("com.carpoolapp", true, "26")
    }

    fun enviarLink(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Enviando
            try {
                // Enviar link al emulador (queda registrado)
                auth.sendSignInLinkToEmail(email, actionCodeSettings).await()
                
                // AUTO-COMPLETAR para testing - hardcodeado con tu email
                // Usamos auth anónimo para que Firebase Auth funcione en el emulador
                val authResult = auth.signInAnonymously().await()
                val uid = authResult.user?.uid ?: "test-anon-uid"
                
                // Crear usuario y guardar en Firestore
                val usuario = Usuario(
                    id = uid,
                    nombre = "jquinterov",
                    email = "jquinterov@gmail.com"
                )
                usuarioRepository.guardar(usuario)
                _uiState.value = AuthUiState.Autenticado(usuario)
                
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Error al enviar link")
            }
        }
    }

    fun verificarLink(email: String, link: String) {
        viewModelScope.launch {
            try {
                if (auth.isSignInWithEmailLink(link)) {
                    val result = auth.signInWithEmailLink(email, link).await()
                    val uid = result.user?.uid ?: return@launch
                    val usuario = Usuario(
                        id = uid,
                        email = email,
                        nombre = email.substringBefore("@")
                    )
                    usuarioRepository.guardar(usuario)
                    _uiState.value = AuthUiState.Autenticado(usuario)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Error al verificar link")
            }
        }
    }

    fun cerrarSesion() {
        auth.signOut()
        _uiState.value = AuthUiState.Idle
    }
}

package com.carpoolapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.usecase.GetFeedUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val viajes: List<Viaje>) : HomeUiState()
    data class Error(val mensaje: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getFeedUseCase: GetFeedUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        cargarFeed()
    }

    fun cargarFeed() {
        val usuarioId = auth.currentUser?.uid
        if (usuarioId == null) {
            _uiState.value = HomeUiState.Error("Inicia sesion para ver viajes")
            return
        }
        viewModelScope.launch {
            getFeedUseCase(usuarioId)
                .catch { e ->
                    _uiState.value = HomeUiState.Error(e.message ?: "Error al cargar viajes")
                }
                .collect { viajes ->
                    _uiState.value = HomeUiState.Success(viajes)
                }
        }
    }
}

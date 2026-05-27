package com.carpoolapp.ui.home

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carpoolapp.MainActivity
import com.carpoolapp.R
import com.carpoolapp.domain.model.Solicitud
import com.carpoolapp.domain.model.SolicitudEstado
import com.carpoolapp.domain.model.Viaje
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
import kotlinx.coroutines.Job
import javax.inject.Inject

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val viajes: List<Viaje>) : HomeUiState()
    data class Error(val mensaje: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val getFeedUseCase: GetFeedUseCase,
    private val viajeRepository: ViajeRepository,
    private val solicitudRepository: SolicitudRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private var _solicitudesJob: Job? = null
    private var solicitudesPrevias = emptyList<Solicitud>()

    init {
        cargarFeed()
    }
    
    fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "solicitudes_viaje",
                "Solicitudes de viaje",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando hay nuevas solicitudes en tus viajes"
                enableVibration(true)
            }
            val manager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    fun mostrarNotificacionSolicitud(pasajeroNombre: String, viajeOrigen: String, viajeDestino: String, tripId: String) {
        crearCanalNotificacion()
        
        val intent = Intent(application, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("tripId", tripId)
            putExtra("navigateToTrip", true)
            putExtra("esConductor", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            application,
            tripId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(application, "solicitudes_viaje")
            .setSmallIcon(R.drawable.ic_person_grey_24dp)
            .setContentTitle("Nueva solicitud")
            .setContentText("$pasajeroNombre solicitó tu viaje: $viajeOrigen → $viajeDestino")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        val manager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt() + tripId.hashCode(), notification)
        android.util.Log.d("HomeViewModel", "Notificación mostrada: $pasajeroNombre solicitó viaje $tripId")
    }

    fun cargarFeed() {
        val usuarioId = auth.currentUser?.uid
        if (usuarioId == null) {
            _uiState.value = HomeUiState.Error("Inicia sesion para ver viajes")
            return
        }
        
        // Cancel previous solicitudes listener
        _solicitudesJob?.cancel()
        
        // Start listener for new requests on user's trips
        _solicitudesJob = viewModelScope.launch {
            viajeRepository.getViajesPorConductor(usuarioId)
                .catch { e -> android.util.Log.w("HomeViewModel", "Error getting viajes", e) }
                .collect { viajes ->
                    android.util.Log.d("HomeViewModel", "Monitoreando ${viajes.size} viajes para solicitudes")
                    viajes.forEach { viaje ->
                        try {
                            solicitudRepository.getSolicitudesPorViaje(viaje.id)
                                .catch { e -> android.util.Log.w("HomeViewModel", "Error en flow solicitudes", e) }
                                .collect { solicitudes ->
                                    val nuevasSolicitudes = solicitudes.filter { nueva ->
                                        solicitudesPrevias.none { anterior -> 
                                            anterior.id == nueva.id && anterior.estado == nueva.estado 
                                        }
                                    }.filter { it.estado == SolicitudEstado.PENDIENTE }
                                    
                                    if (nuevasSolicitudes.isNotEmpty()) {
                                        android.util.Log.d("HomeViewModel", "${nuevasSolicitudes.size} nuevas solicitudes detectadas")
                                        nuevasSolicitudes.forEach { solicitud ->
                                            mostrarNotificacionSolicitud(
                                                solicitud.pasajeroNombre,
                                                viaje.origen,
                                                viaje.destino,
                                                viaje.id
                                            )
                                        }
                                        solicitudesPrevias = solicitudesPrevias + nuevasSolicitudes
                                    }
                                }
                        } catch (e: Exception) {
                            android.util.Log.w("HomeViewModel", "Error monitoreando solicitudes", e)
                        }
                    }
                }
        }
        
        viewModelScope.launch {
            try {
                getFeedUseCase(usuarioId)
                    .catch { e ->
                        _uiState.value = HomeUiState.Error(mapToUserMessage(e.message))
                    }
                    .collect { viajes ->
                        _uiState.value = HomeUiState.Success(viajes)
                    }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(mapToUserMessage(e.message))
            }
        }
    }

    private fun mapToUserMessage(rawMessage: String?): String {
        val message = rawMessage.orEmpty()
        return if (message.contains("PERMISSION_DENIED", ignoreCase = true)) {
            "No tienes permisos para leer los viajes en Firestore. Verifica reglas y usuario autenticado."
        } else {
            rawMessage ?: "Error al cargar viajes"
        }
    }
}

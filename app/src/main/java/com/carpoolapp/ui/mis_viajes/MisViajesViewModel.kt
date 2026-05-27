package com.carpoolapp.ui.mis_viajes

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.carpoolapp.MainActivity
import com.carpoolapp.R
import com.carpoolapp.domain.model.Solicitud
import com.carpoolapp.domain.model.SolicitudEstado
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.repository.SolicitudRepository
import com.carpoolapp.domain.repository.ViajeRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed class MisViajesUiState {
    object Loading : MisViajesUiState()
    data class Success(
        val comoConductor: List<Viaje>,
        val comoPasajero: List<Viaje>
    ) : MisViajesUiState()
    data class Error(val mensaje: String) : MisViajesUiState()
}

@HiltViewModel
class MisViajesViewModel @Inject constructor(
    private val application: Application,
    private val viajeRepository: ViajeRepository,
    private val solicitudRepository: SolicitudRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<MisViajesUiState>(MisViajesUiState.Loading)
    val uiState: StateFlow<MisViajesUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        // Cancel previous collectors before starting new ones.
        try {
            _conductorJob?.cancel()
        } catch (_: Exception) {}
        try {
            _createdEventsJob?.cancel()
        } catch (_: Exception) {}
        try {
            _solicitudesJob?.cancel()
        } catch (_: Exception) {}

        val uid = auth.currentUser?.uid
        Log.d("MisViajesVM", "cargar() uid=${uid}")
        if (uid == null) {
            _uiState.value = MisViajesUiState.Error("Inicia sesion para ver viajes")
            return
        }

        val comoConductor = mutableListOf<Viaje>()

        // Listener global para solicitudes nuevas en todos los viajes del conductor
        _solicitudesJob = viewModelScope.launch {
            viajeRepository.getViajesPorConductor(uid)
                .catch { e -> Log.w("MisViajesVM", "Error getting viajes para solicitudes", e) }
                .collect { viajes ->
                    Log.d("MisViajesVM", "Monitoreando ${viajes.size} viajes para solicitudes")
                    viajes.forEach { viaje ->
                        try {
                            solicitudRepository.getSolicitudesPorViaje(viaje.id)
                                .catch { e -> Log.w("MisViajesVM", "Error en flow solicitudes viaje ${viaje.id}", e) }
                                .collect { solicitudes ->
                                    val nuevasSolicitudes = solicitudes.filter { nueva ->
                                        solicitudesPrevias.none { anterior -> 
                                            anterior.id == nueva.id && anterior.estado == nueva.estado 
                                        }
                                    }.filter { it.estado == SolicitudEstado.PENDIENTE }
                                    
                                    if (nuevasSolicitudes.isNotEmpty()) {
                                        Log.d("MisViajesVM", "${nuevasSolicitudes.size} nuevas solicitudes detectadas")
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
                            Log.w("MisViajesVM", "Error monitoreando solicitudes viaje ${viaje.id}", e)
                        }
                    }
                }
        }

        // Collector para eventos creados (optimistic updates) en coroutine independiente
        _createdEventsJob = viewModelScope.launch {
            viajeRepository.createdEvents()
                .catch { e -> Log.w("MisViajesVM", "createdEvents flow error", e) }
                .collect { nuevo ->
                    try {
                        if (nuevo.conductorId == uid) {
                            Log.d("MisViajesVM", "Received created event for uid=$uid id=${nuevo.id}")
                            if (comoConductor.none { it.id == nuevo.id }) {
                                comoConductor.add(0, nuevo)
                                val comoPasajeroNow = try { fetchComoPasajeroOnce(uid) } catch (e: Exception) { emptyList<Viaje>() }
                                _uiState.value = MisViajesUiState.Success(comoConductor.toList(), comoPasajeroNow)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("MisViajesVM", "Error procesando created event", e)
                    }
                }
        }

        // Collector para los viajes del conductor (real-time) en coroutine independiente
        _conductorJob = viewModelScope.launch {
            viajeRepository.getViajesPorConductor(uid)
                .catch { e -> Log.w("MisViajesVM", "getViajesPorConductor flow error", e); emit(emptyList()) }
                .collect { viajes ->
                    try {
                        Log.d("MisViajesVM", "getViajesPorConductor emitted ${viajes.size} viajes for uid=$uid")
                        comoConductor.clear()
                        comoConductor.addAll(viajes)
                        val comoPasajero = try { fetchComoPasajeroOnce(uid) } catch (e: Exception) { emptyList<Viaje>() }
                        Log.d("MisViajesVM", "getViajesComoPasajero returned ${comoPasajero.size} viajes for uid=$uid")
                        _uiState.value = MisViajesUiState.Success(comoConductor.toList(), comoPasajero)
                    } catch (e: Exception) {
                            Log.w("MisViajesVM", "Error procesando viajes", e)
                        }
                    }
            }
    }

    private var _conductorJob: Job? = null
    private var _createdEventsJob: Job? = null
    private var _solicitudesJob: Job? = null
    private var solicitudesPrevias = emptyList<Solicitud>()

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
        Log.d("MisViajesVM", "Notificación mostrada: $pasajeroNombre solicitó viaje $tripId")
    }

    private suspend fun fetchComoPasajeroOnce(uid: String): List<Viaje> {
        return viajeRepository.getViajesComoPasajero(uid)
    }
}

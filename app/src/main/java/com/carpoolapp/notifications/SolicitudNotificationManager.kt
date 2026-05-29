package com.carpoolapp.notifications

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.carpoolapp.MainActivity
import com.carpoolapp.R
import com.carpoolapp.domain.model.SolicitudEstado
import com.carpoolapp.domain.repository.SolicitudRepository
import com.carpoolapp.domain.repository.ViajeRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolicitudNotificationManager @Inject constructor(
    private val application: Application,
    private val viajeRepository: ViajeRepository,
    private val solicitudRepository: SolicitudRepository,
    private val auth: FirebaseAuth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var solicitudJobs = mutableMapOf<String, Job>()
    
    private val prefs: SharedPreferences by lazy {
        application.getSharedPreferences("notificaciones", Context.MODE_PRIVATE)
    }
    
    private val notificadas: MutableSet<String>
        get() = prefs.getStringSet("notificadas", emptySet())?.toMutableSet() ?: mutableSetOf()
    
    private fun agregarNotificada(id: String) {
        val current = notificadas
        current.add(id)
        prefs.edit { putStringSet("notificadas", current) }
    }
    
    private fun estaNotificada(id: String): Boolean = notificadas.contains(id)

    fun startListening() {
        android.util.Log.d("NotifMgr", "startListening() llamado")
        crearCanalNotificacion()
        val usuarioId = auth.currentUser?.uid ?: run {
            android.util.Log.w("NotifMgr", "No hay usuario autenticado")
            return
        }
        iniciarMonitoreo(usuarioId)
    }

    private fun iniciarMonitoreo(usuarioId: String) {
        detenerMonitoreo()
        crearCanalNotificacion()

        android.util.Log.d("NotifMgr", "Iniciando monitoreo para usuario: $usuarioId")

        job = scope.launch {
            viajeRepository.getViajesPorConductor(usuarioId)
                .catch { e -> android.util.Log.w("NotifMgr", "Error obteniendo viajes", e) }
                .collect { viajes ->
                    android.util.Log.d("NotifMgr", "Viajes del conductor: ${viajes.size}, notificadas persistentes: ${notificadas.size}")
                    val currentIds = viajes.map { it.id }.toSet()
                    solicitudJobs.keys.filter { it !in currentIds }.forEach { idEliminado ->
                        solicitudJobs.remove(idEliminado)?.cancel()
                    }
                    viajes.forEach { viaje ->
                        if (viaje.id !in solicitudJobs) {
                            android.util.Log.d("NotifMgr", "Creando listener para viaje: ${viaje.id}")
                            solicitudJobs[viaje.id] = scope.launch {
                                solicitudRepository.getSolicitudesPorViaje(viaje.id)
                                    .catch { e -> android.util.Log.w("NotifMgr", "Error en solicitudes ${viaje.id}", e) }
                                    .collect { solicitudes ->
                                        android.util.Log.d("NotifMgr", "Solicitudes en viaje ${viaje.id}: ${solicitudes.size}")
                                        solicitudes
                                            .filter { 
                                                val esPendiente = it.estado == SolicitudEstado.PENDIENTE
                                                val estaNotificada = !estaNotificada(it.id)
                                                android.util.Log.d("NotifMgr", "Solicitud ${it.id}: pendiente=$esPendiente, noNotificada=$estaNotificada")
                                                esPendiente && estaNotificada
                                            }
                                            .forEach { solicitud ->
                                                agregarNotificada(solicitud.id)
                                                android.util.Log.d("NotifMgr", "Agregada a notificadas: ${solicitud.id}, total: ${notificadas.size}")
                                                mostrarNotificacion(
                                                    pasajeroNombre = solicitud.pasajeroNombre,
                                                    viajeOrigen = viaje.origen,
                                                    viajeDestino = viaje.destino,
                                                    tripId = viaje.id
                                                )
                                            }
                                    }
                            }
                        }
                    }
                }
        }
    }

    private fun detenerMonitoreo() {
        android.util.Log.d("NotifMgr", "Deteniendo monitoreo. Notificadas persistentes: ${notificadas.size}")
        solicitudJobs.values.forEach { it.cancel() }
        solicitudJobs.clear()
        job?.cancel()
        job = null
        // Ya no limpiamos las notificadas porque son persistentes
    }

    fun stopListening() {
        android.util.Log.d("NotifMgr", "stopListening() llamado")
        detenerMonitoreo()
        scope.cancel()
    }

    private fun crearCanalNotificacion() {
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

    private fun mostrarNotificacion(
        pasajeroNombre: String,
        viajeOrigen: String,
        viajeDestino: String,
        tripId: String
    ) {
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
        android.util.Log.d("NotifMgr", "Notificación mostrada: $pasajeroNombre solicitó viaje $tripId")
    }
}

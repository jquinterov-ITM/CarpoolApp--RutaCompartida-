package com.carpoolapp.data.repository

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.carpoolapp.MainActivity
import com.carpoolapp.R
import com.carpoolapp.data.mapper.toDomain
import com.carpoolapp.data.mapper.toDto
import com.carpoolapp.data.remote.firestore.FirestoreSolicitudDataSource
import com.carpoolapp.domain.model.Solicitud
import com.carpoolapp.domain.repository.SolicitudRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SolicitudRepositoryImpl @Inject constructor(
    private val application: Application,
    private val dataSource: FirestoreSolicitudDataSource
) : SolicitudRepository {

    override fun getSolicitudesPorViaje(tripId: String): Flow<List<Solicitud>> {
        return dataSource.getSolicitudesPorViaje(tripId)
            .map { list -> list.map { it.toDomain(tripId) } }
    }

    override suspend fun enviar(tripId: String, solicitud: Solicitud) {
        dataSource.crear(tripId, solicitud.toDto())
    }

    override suspend fun aceptar(tripId: String, requestId: String) {
        android.util.Log.d("SolicitudRepository", "Aceptando solicitud $requestId en viaje $tripId")
        
        val solicitudData = dataSource.getSolicitudById(tripId, requestId)
        dataSource.aceptarConTransaction(tripId, requestId)
        android.util.Log.d("SolicitudRepository", "Solicitud aceptada en Firestore")
        
        if (solicitudData != null) {
            enviarNotificacionAlPasajero(
                pasajeroNombre = solicitudData.pasajeroNombre,
                viajeOrigen = solicitudData.tripId,
                viajeDestino = "Tu solicitud fue ACEPTADA",
                tripId = tripId,
                tipo = "solicitud_aceptada",
                titulo = "¡Solicitud Aceptada!",
                cuerpo = "El conductor aceptó tu solicitud de viaje"
            )
        }
    }

    override suspend fun rechazar(tripId: String, requestId: String) {
        android.util.Log.d("SolicitudRepository", "Rechazando solicitud $requestId en viaje $tripId")
        
        val solicitudData = dataSource.getSolicitudById(tripId, requestId)
        dataSource.actualizarEstado(tripId, requestId, "RECHAZADA")
        android.util.Log.d("SolicitudRepository", "Solicitud rechazada en Firestore")
        
        if (solicitudData != null) {
            enviarNotificacionAlPasajero(
                pasajeroNombre = solicitudData.pasajeroNombre,
                viajeOrigen = solicitudData.tripId,
                viajeDestino = "Tu solicitud fue RECHAZADA",
                tripId = tripId,
                tipo = "solicitud_rechazada",
                titulo = "Solicitud Rechazada",
                cuerpo = "El conductor rechazó tu solicitud de viaje"
            )
        }
    }

    override suspend fun cancelarSolicitud(tripId: String, solicitudId: String) {
        dataSource.actualizarEstado(tripId, solicitudId, "CANCELADA")
    }
    
    private fun enviarNotificacionAlPasajero(
        pasajeroNombre: String,
        viajeOrigen: String,
        viajeDestino: String,
        tripId: String,
        tipo: String,
        titulo: String,
        cuerpo: String
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "solicitudes_viaje",
                    "Solicitudes de viaje",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones de viajes y solicitudes"
                    enableVibration(true)
                }
                val manager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
            
            val intent = Intent(application, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("tripId", tripId)
                putExtra("navigateToTrip", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                application,
                tripId.hashCode() + 1000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(application, "solicitudes_viaje")
                .setSmallIcon(R.drawable.ic_person_grey_24dp)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            
            val manager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(System.currentTimeMillis().toInt() + tripId.hashCode() + 1000, notification)
            android.util.Log.d("SolicitudRepository", "Notificación enviada a pasajero: $pasajeroNombre")
        } catch (e: Exception) {
            android.util.Log.e("SolicitudRepository", "Error enviando notificación: ${e.message}", e)
        }
    }
}

package com.carpoolapp.data.repository

import com.carpoolapp.data.mapper.toDomain
import com.carpoolapp.data.mapper.toDto
import com.carpoolapp.data.remote.firestore.FirestoreSolicitudDataSource
import com.carpoolapp.domain.model.Solicitud
import com.carpoolapp.domain.repository.SolicitudRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

class SolicitudRepositoryImpl @Inject constructor(
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
        android.util.Log.d("SolicitudRepository", "=== ACEPTANDO SOLICITUD ===")
        android.util.Log.d("SolicitudRepository", "tripId: $tripId, requestId: $requestId")
        
        val solicitudData = dataSource.getSolicitudById(tripId, requestId)
        val viajeData = dataSource.getViajeById(tripId)
        
        android.util.Log.d("SolicitudRepository", "solicitudData: ${solicitudData?.pasajeroId ?: "null"}")
        android.util.Log.d("SolicitudRepository", "viajeData: ${viajeData?.conductorNombre ?: "null"}")
        
        dataSource.aceptarConTransaction(tripId, requestId)
        android.util.Log.d("SolicitudRepository", "Solicitud aceptada en Firestore")
        
        // Enviar notificación al PASAJERO (no al conductor)
        if (solicitudData != null && viajeData != null) {
            android.util.Log.d("SolicitudRepository", "Enviando notificación a: ${solicitudData.pasajeroId}")
            enviarNotificacionFCM(
                userId = solicitudData.pasajeroId,
                titulo = "¡Solicitud Aceptada!",
                cuerpo = "${viajeData.conductorNombre} aceptó tu solicitud",
                tripId = tripId,
                tipo = "solicitud_aceptada"
            )
        } else {
            android.util.Log.e("SolicitudRepository", "No se pudo enviar notificación: solicitudData=$solicitudData, viajeData=$viajeData")
        }
    }

    override suspend fun rechazar(tripId: String, requestId: String) {
        android.util.Log.d("SolicitudRepository", "Rechazando solicitud $requestId en viaje $tripId")
        
        val solicitudData = dataSource.getSolicitudById(tripId, requestId)
        val viajeData = dataSource.getViajeById(tripId)
        
        dataSource.actualizarEstado(tripId, requestId, "RECHAZADA")
        android.util.Log.d("SolicitudRepository", "Solicitud rechazada en Firestore")
        
        // Enviar notificación al PASAJERO (no al conductor)
        if (solicitudData != null && viajeData != null) {
            enviarNotificacionFCM(
                userId = solicitudData.pasajeroId,
                titulo = "Solicitud Rechazada",
                cuerpo = "${viajeData.conductorNombre} rechazó tu solicitud",
                tripId = tripId,
                tipo = "solicitud_rechazada"
            )
        }
    }

    override suspend fun cancelarSolicitud(tripId: String, solicitudId: String) {
        dataSource.actualizarEstado(tripId, solicitudId, "CANCELADA")
    }
    
    private suspend fun enviarNotificacionFCM(
        userId: String,
        titulo: String,
        cuerpo: String,
        tripId: String,
        tipo: String
    ) {
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            
            val notificacion = hashMapOf(
                "userId" to userId,
                "titulo" to titulo,
                "cuerpo" to cuerpo,
                "tripId" to tripId,
                "tipo" to tipo,
                "leida" to false,
                "timestamp" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection("notifications").add(notificacion).await()
            android.util.Log.d("SolicitudRepository", "Notificación guardada en Firestore para $userId: $titulo")
        } catch (e: Exception) {
            android.util.Log.e("SolicitudRepository", "Error guardando notificación: ${e.message}", e)
        }
    }
}

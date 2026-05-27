package com.carpoolapp.data.remote.firestore

import com.carpoolapp.data.remote.dto.SolicitudDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreSolicitudDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun requestsCollection(tripId: String) =
        firestore.collection("trips").document(tripId).collection("requests")

    fun getSolicitudesPorViaje(tripId: String): Flow<List<SolicitudDto>> = callbackFlow {
        val currentUser = requireAuthOrClose(this) ?: return@callbackFlow
        try {
            val listener = requestsCollection(tripId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.w("FirestoreSolicitudDS", "Error escuchando solicitudes: ${error.message}")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val solicitudes = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject<SolicitudDto>()?.copy(id = doc.id)
                    } ?: emptyList()
                    android.util.Log.d("FirestoreSolicitudDS", "Solicitudes recibidas: ${solicitudes.size}")
                    trySend(solicitudes)
                }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreSolicitudDS", "Error creando listener: ${e.message}")
            trySend(emptyList())
            close()
        }
    }

    suspend fun crear(tripId: String, dto: SolicitudDto): String {
        return firestoreSafe("FirestoreSolicitudDS", "") {
            val dtoWithTripId = dto.copy(tripId = tripId)
            val ref = requestsCollection(tripId).add(dtoWithTripId).await()
            ref.id
        }
    }

    suspend fun actualizarEstado(tripId: String, requestId: String, estado: String) {
        android.util.Log.d("FirestoreSolicitudDS", "Actualizando estado de $requestId a $estado")
        firestoreSafe("FirestoreSolicitudDS", Unit) {
            requestsCollection(tripId).document(requestId)
                .update("estado", estado).await()
            android.util.Log.d("FirestoreSolicitudDS", "Estado actualizado correctamente")
        }
    }
    
    suspend fun getSolicitudById(tripId: String, requestId: String): com.carpoolapp.data.remote.dto.SolicitudDto? {
        return try {
            val doc = requestsCollection(tripId).document(requestId).get().await()
            if (doc.exists()) {
                com.carpoolapp.data.remote.dto.SolicitudDto.fromDocument(requestId, doc.data ?: emptyMap())
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreSolicitudDS", "Error obteniendo solicitud $requestId: ${e.message}", e)
            null
        }
    }

    suspend fun aceptarConTransaction(tripId: String, requestId: String) {
        android.util.Log.d("FirestoreSolicitudDS", "Iniciando transacción para aceptar $requestId")
        firestoreSafe("FirestoreSolicitudDS", Unit) {
            try {
                firestore.runTransaction { transaction ->
                    val tripRef = firestore.collection("trips").document(tripId)
                    val requestRef = requestsCollection(tripId).document(requestId)
                    
                    android.util.Log.d("FirestoreSolicitudDS", "Leyendo documentos...")
                    val trip = transaction.get(tripRef)
                    val request = transaction.get(requestRef)
                    
                    if (!trip.exists()) {
                        android.util.Log.e("FirestoreSolicitudDS", "El viaje $tripId no existe")
                        throw Exception("El viaje no existe")
                    }
                    if (!request.exists()) {
                        android.util.Log.e("FirestoreSolicitudDS", "La solicitud $requestId no existe")
                        throw Exception("La solicitud no existe")
                    }
                    
                    val asientos = trip.getLong("asientosDisponibles") ?: 0
                    android.util.Log.d("FirestoreSolicitudDS", "Asientos disponibles: $asientos")
                    if (asientos <= 0) throw IllegalStateException("No hay asientos disponibles")
                    
                    val asientosSolicitados = (request.getLong("asientosSolicitados") ?: 1).toInt()
                    android.util.Log.d("FirestoreSolicitudDS", "Asientos solicitados: $asientosSolicitados")
                    if (asientosSolicitados > asientos) {
                        throw IllegalStateException("No hay suficientes asientos disponibles")
                    }
                    
                    val pasajerosActuales = trip.get("pasajeroIds") as? List<String> ?: emptyList()
                    val pasajeroId = request.getString("pasajeroId") ?: ""
                    android.util.Log.d("FirestoreSolicitudDS", "Pasajero ID: $pasajeroId, actuales: ${pasajerosActuales.size}")
                    
                    transaction.update(tripRef, "asientosDisponibles", asientos - asientosSolicitados)
                    transaction.update(tripRef, "pasajeroIds", pasajerosActuales + pasajeroId)
                    transaction.update(requestRef, "estado", "ACEPTADA")
                    
                    android.util.Log.d("FirestoreSolicitudDS", "Transacción completada")
                }.await()
                android.util.Log.d("FirestoreSolicitudDS", "Solicitud aceptada exitosamente")
            } catch (e: Exception) {
                android.util.Log.e("FirestoreSolicitudDS", "Error en transacción: ${e.message}", e)
                throw e
            }
        }
    }
}

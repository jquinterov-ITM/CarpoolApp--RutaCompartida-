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
        val listener = requestsCollection(tripId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val solicitudes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<SolicitudDto>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(solicitudes)
            }
        awaitClose { listener.remove() }
    }

    suspend fun crear(tripId: String, dto: SolicitudDto): String {
        val ref = requestsCollection(tripId).add(dto).await()
        return ref.id
    }

    suspend fun actualizarEstado(tripId: String, requestId: String, estado: String) {
        requestsCollection(tripId).document(requestId)
            .update("estado", estado).await()
    }

    suspend fun aceptarConTransaction(tripId: String, requestId: String) {
        firestore.runTransaction { transaction ->
            val tripRef = firestore.collection("trips").document(tripId)
            val requestRef = requestsCollection(tripId).document(requestId)
            val trip = transaction.get(tripRef)

            val asientos = trip.getLong("asientosDisponibles") ?: 0
            if (asientos <= 0) throw IllegalStateException("No hay asientos disponibles")

            transaction.update(tripRef, "asientosDisponibles", asientos - 1)
            transaction.update(requestRef, "estado", "ACEPTADA")
        }.await()
    }
}

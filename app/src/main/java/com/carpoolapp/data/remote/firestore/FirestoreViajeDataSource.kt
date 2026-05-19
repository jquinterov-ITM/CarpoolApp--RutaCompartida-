package com.carpoolapp.data.remote.firestore

import com.carpoolapp.data.remote.dto.ViajeDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreViajeDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection get() = firestore.collection("trips")

    fun getFeed(usuarioId: String): Flow<List<ViajeDto>> = callbackFlow {
        val listener = collection
            .whereNotEqualTo("estado", "CANCELADO")
            .whereNotEqualTo("conductorId", usuarioId)
            .orderBy("fechaHora")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val viajes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<ViajeDto>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(viajes)
            }
        awaitClose { listener.remove() }
    }

    fun getFeedPorDestino(usuarioId: String, destino: String): Flow<List<ViajeDto>> = callbackFlow {
        val listener = collection
            .whereNotEqualTo("estado", "CANCELADO")
            .whereNotEqualTo("conductorId", usuarioId)
            .orderBy("fechaHora")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val viajes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<ViajeDto>()?.copy(id = doc.id)
                }?.filter { it.destino.contains(destino, ignoreCase = true) } ?: emptyList()
                trySend(viajes)
            }
        awaitClose { listener.remove() }
    }

    fun getViajesPorConductor(conductorId: String): Flow<List<ViajeDto>> = callbackFlow {
        val listener = collection
            .whereEqualTo("conductorId", conductorId)
            .orderBy("fechaHora", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val viajes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<ViajeDto>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(viajes)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getViajesPorPasajero(pasajeroId: String): List<ViajeDto> {
        val requests = firestore.collectionGroup("requests")
            .whereEqualTo("pasajeroId", pasajeroId)
            .get()
            .await()
        val tripIds = requests.documents.mapNotNull { it.reference.parent.parent?.id }
        if (tripIds.isEmpty()) return emptyList()
        val trips = collection
            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), tripIds)
            .get()
            .await()
        return trips.documents.mapNotNull { doc ->
            doc.toObject<ViajeDto>()?.copy(id = doc.id)
        }
    }

    suspend fun crear(dto: ViajeDto): String {
        val ref = collection.add(dto).await()
        return ref.id
    }

    suspend fun actualizarEstado(id: String, estado: String) {
        collection.document(id).update("estado", estado).await()
    }
}

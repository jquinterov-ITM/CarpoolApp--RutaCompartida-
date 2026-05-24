package com.carpoolapp.data.remote.firestore

import com.carpoolapp.data.remote.dto.ViajeDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreViajeDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection get() = firestore.collection("trips")

private fun documentToDto(id: String, data: Map<String, Any?>): ViajeDto? {
        return try {
            ViajeDto(
                id = id,
                conductorId = data["conductorId"] as? String ?: return null,
                conductorNombre = data["conductorNombre"] as? String ?: "",
                origen = data["origen"] as? String ?: "",
                destino = data["destino"] as? String ?: "",
                fechaHora = data["fechaHora"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                asientosDisponibles = (data["asientosDisponibles"] as? Long)?.toInt() ?: 0,
                tipo = data["tipo"] as? String ?: "PROGRAMADO",
                estado = data["estado"] as? String ?: "PROGRAMADO",
                createdAt = data["createdAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now()
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getFeed(usuarioId: String): Flow<List<ViajeDto>> = callbackFlow {
        val listener = collection
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(); return@addSnapshotListener }
                val viajes = snapshot?.documents?.mapNotNull { doc ->
                    documentToDto(doc.id, doc.data ?: emptyMap<String, Any?>())
                }?.filter { 
                    it.estado != "CANCELADO" && it.conductorId != usuarioId 
                } ?: emptyList()
                trySend(viajes)
            }
        awaitClose { listener.remove() }
    }

    fun getFeedPorDestino(usuarioId: String, destino: String): Flow<List<ViajeDto>> = callbackFlow {
        val listener = collection
            .orderBy("fechaHora")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(); return@addSnapshotListener }
                val viajes = snapshot?.documents?.mapNotNull { doc ->
                    documentToDto(doc.id, doc.data ?: emptyMap())
                }?.filter { 
                    it.estado != "CANCELADO" && 
                    it.conductorId != usuarioId &&
                    it.destino.contains(destino, ignoreCase = true) 
                } ?: emptyList()
                trySend(viajes)
            }
        awaitClose { listener.remove() }
    }

    fun getViajesPorConductor(conductorId: String): Flow<List<ViajeDto>> = callbackFlow {
        val listener = collection
            .whereEqualTo("conductorId", conductorId)
            .orderBy("fechaHora", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(); return@addSnapshotListener }
                val viajes = snapshot?.documents?.mapNotNull { doc ->
                    documentToDto(doc.id, doc.data ?: emptyMap())
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
            documentToDto(doc.id, doc.data ?: emptyMap())
        }
    }

    suspend fun crear(dto: ViajeDto): String {
        val ref = collection.add(dto).await()
        return ref.id
    }

    suspend fun actualizarEstado(id: String, estado: String) {
        collection.document(id).update("estado", estado).await()
    }

    suspend fun seedDemoDataIfNeeded() {
        val count = collection.limit(1).get().await().size()
        if (count > 0) return

        val now = System.currentTimeMillis()
        val demoTrips = listOf(
            mapOf(
                "conductorId" to "demo_driver_1",
                "conductorNombre" to "Carlos Mendoza",
                "origen" to "Centro, Ciudad de Mexico",
                "destino" to "Politecnico, Ciudad de Mexico",
                "fechaHora" to com.google.firebase.Timestamp.now(),
                "asientosDisponibles" to 3,
                "tipo" to "PROGRAMADO",
                "estado" to "PROGRAMADO",
                "createdAt" to com.google.firebase.Timestamp.now()
            ),
            mapOf(
                "conductorId" to "demo_driver_2",
                "conductorNombre" to "Ana Lopez",
                "origen" to "Condesa, Ciudad de Mexico",
                "destino" to "Santa Fe, Ciudad de Mexico",
                "fechaHora" to com.google.firebase.Timestamp.now(),
                "asientosDisponibles" to 2,
                "tipo" to "PROGRAMADO",
                "estado" to "PROGRAMADO",
                "createdAt" to com.google.firebase.Timestamp.now()
            ),
            mapOf(
                "conductorId" to "demo_driver_3",
                "conductorNombre" to "Pedro Ramirez",
                "origen" to "Coyoacan, Ciudad de Mexico",
                "destino" to "Centro Historico, Ciudad de Mexico",
                "fechaHora" to com.google.firebase.Timestamp.now(),
                "asientosDisponibles" to 4,
                "tipo" to "PROGRAMADO",
                "estado" to "PROGRAMADO",
                "createdAt" to com.google.firebase.Timestamp.now()
            ),
            mapOf(
                "conductorId" to "demo_driver_4",
                "conductorNombre" to "Maria Garcia",
                "origen" to "Roma, Ciudad de Mexico",
                "destino" to "Insurgentes, Ciudad de Mexico",
                "fechaHora" to com.google.firebase.Timestamp.now(),
                "asientosDisponibles" to 1,
                "tipo" to "PROGRAMADO",
                "estado" to "PROGRAMADO",
                "createdAt" to com.google.firebase.Timestamp.now()
            ),
            mapOf(
                "conductorId" to "demo_driver_5",
                "conductorNombre" to "Luis Hernandez",
                "origen" to "Del Valle, Ciudad de Mexico",
                "destino" to "Naucalpan, Estado de Mexico",
                "fechaHora" to com.google.firebase.Timestamp.now(),
                "asientosDisponibles" to 2,
                "tipo" to "PROGRAMADO",
                "estado" to "PROGRAMADO",
                "createdAt" to com.google.firebase.Timestamp.now()
            )
        )

        demoTrips.forEach { trip ->
            collection.add(trip).await()
        }
    }
}

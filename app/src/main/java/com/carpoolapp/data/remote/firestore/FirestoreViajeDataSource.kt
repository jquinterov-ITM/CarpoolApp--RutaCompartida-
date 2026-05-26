package com.carpoolapp.data.remote.firestore

import com.carpoolapp.data.remote.dto.ViajeDto
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
            .orderBy("fechaHora")
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
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        val query = collection
            .whereEqualTo("conductorId", conductorId)
            .orderBy("fechaHora", com.google.firebase.firestore.Query.Direction.DESCENDING)

        fun attach() {
            try {
                listener?.remove()
            } catch (_: Exception) {}
            listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreViajeDS", "Error listening viajes por conductor", error)
                    trySend(emptyList())
                    // retry attaching after short delay
                    try {
                        launch {
                            kotlinx.coroutines.delay(2000)
                            attach()
                        }
                    } catch (_: Exception) {}
                    return@addSnapshotListener
                }
                val viajes = snapshot?.documents?.mapNotNull { doc ->
                    documentToDto(doc.id, doc.data ?: emptyMap())
                } ?: emptyList()
                trySend(viajes)
            }
        }

        attach()
        awaitClose { try { listener?.remove() } catch (_: Exception) {} }
    }

    suspend fun getViajesPorPasajero(pasajeroId: String): List<ViajeDto> {
        val result: List<ViajeDto> = firestoreSafe("FirestoreViajeDS", emptyList()) {
            val requests = firestore.collectionGroup("requests")
                .whereEqualTo("pasajeroId", pasajeroId)
                .get()
                .await()
            val tripIds = requests.documents.mapNotNull { it.reference.parent.parent?.id }
            if (tripIds.isEmpty()) return@firestoreSafe emptyList()
            val trips = collection
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), tripIds)
                .get()
                .await()
            trips.documents.mapNotNull { doc ->
                documentToDto(doc.id, doc.data ?: emptyMap())
            }
        }
        return result
    }

    suspend fun crear(dto: ViajeDto): String {
        // Use a document reference to get the generated id and store it inside the document
        val ref = collection.document()
        val dtoWithId = dto.copy(id = ref.id)
        ref.set(dtoWithId).await()
        return ref.id
    }

    suspend fun actualizarEstado(id: String, estado: String) {
        try {
            collection.document(id).update("estado", estado).await()
        } catch (e: Exception) {
            Log.w("FirestoreViajeDS", "Error actualizando estado para $id", e)
        }
    }

    suspend fun seedDemoDataIfNeeded() {
        val existingCount: Int = firestoreSafe("FirestoreViajeDS", -1) {
            collection.limit(1).get().await().size()
        }
        if (existingCount > 0) return
        if (existingCount < 0) return

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
            try {
                collection.add(trip).await()
            } catch (e: Exception) {
                Log.w("FirestoreViajeDS", "Error agregando demo trip", e)
            }
        }
    }
}

package com.carpoolapp.data.seed

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.carpoolapp.data.remote.firestore.firestoreSafe
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSeeder @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun seedIfEmpty() {
        val existing = firestoreSafe("DataSeeder", null as com.google.firebase.firestore.QuerySnapshot?) {
            firestore.collection("trips").limit(1).get().await()
        }
        if (existing == null) {
            println("DataSeeder: no se pudo comprobar existencia de datos")
            return
        }
        if (existing.documents.isNotEmpty()) return // Ya hay datos

        val otherUserId = "demo-conductor-001"
        val otherUserName = "Carlos Mendoza"
        val now = Timestamp.now()
        val hour = 3600L * 1000

        val trips = listOf(
            mapOf(
                "conductorId" to otherUserId,
                "conductorNombre" to otherUserName,
                "origen" to "Centro",
                "destino" to "Zona Norte",
                "fechaHora" to Timestamp(now.seconds + hour * 2, 0),
                "asientosDisponibles" to 3,
                "tipo" to "PROGRAMADO",
                "estado" to "PROGRAMADO",
                "createdAt" to now
            ),
            mapOf(
                "conductorId" to otherUserId,
                "conductorNombre" to otherUserName,
                "origen" to "Plaza Principal",
                "destino" to "Universidad",
                "fechaHora" to Timestamp(now.seconds + hour * 1, 0),
                "asientosDisponibles" to 2,
                "tipo" to "INMEDIATO",
                "estado" to "ACTIVO",
                "createdAt" to now
            ),
            mapOf(
                "conductorId" to "demo-conductor-002",
                "conductorNombre" to "Ana López",
                "origen" to "Terminal de Buses",
                "destino" to "Centro Comercial",
                "fechaHora" to Timestamp(now.seconds + hour * 3, 0),
                "asientosDisponibles" to 4,
                "tipo" to "PROGRAMADO",
                "estado" to "PROGRAMADO",
                "createdAt" to now
            ),
            mapOf(
                "conductorId" to "demo-conductor-003",
                "conductorNombre" to "Pedro Ramírez",
                "origen" to "Zona Sur",
                "destino" to "Aeropuerto",
                "fechaHora" to Timestamp(now.seconds + hour * 4, 0),
                "asientosDisponibles" to 1,
                "tipo" to "PROGRAMADO",
                "estado" to "PROGRAMADO",
                "createdAt" to now
            ),
            mapOf(
                "conductorId" to otherUserId,
                "conductorNombre" to otherUserName,
                "origen" to "Estación Metro",
                "destino" to "Parque Central",
                "fechaHora" to Timestamp(now.seconds + hour * 5, 0),
                "asientosDisponibles" to 2,
                "tipo" to "INMEDIATO",
                "estado" to "ACTIVO",
                "createdAt" to now
            )
        )

        val batch = firestore.batch()
        for (trip in trips) {
            val ref = firestore.collection("trips").document()
            batch.set(ref, trip)
        }
        batch.commit().await()
    }

    suspend fun seedRequestForUser(pasajeroId: String) {
        // Check if user already has a request
        val existing = firestoreSafe("DataSeeder-req", null as com.google.firebase.firestore.QuerySnapshot?) {
            firestore.collectionGroup("requests").whereEqualTo("pasajeroId", pasajeroId).limit(1).get().await()
        }
        if (existing == null) return
        if (existing.documents.isNotEmpty()) return

        // Find any trip to attach the request to
        val tripSnap = firestoreSafe("DataSeeder-trip", null as com.google.firebase.firestore.QuerySnapshot?) {
            firestore.collection("trips").limit(1).get().await()
        }
        if (tripSnap == null) return
        val tripDoc = tripSnap.documents.firstOrNull() ?: return

        val request = mapOf(
            "pasajeroId" to pasajeroId,
            "estado" to "PENDIENTE",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        try {
            tripDoc.reference.collection("requests").add(request).await()
        } catch (_: Exception) {
            // ignore on debug seeding
        }
    }
}

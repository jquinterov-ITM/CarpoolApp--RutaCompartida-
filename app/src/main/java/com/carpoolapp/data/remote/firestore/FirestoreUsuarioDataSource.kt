package com.carpoolapp.data.remote.firestore

import com.carpoolapp.data.remote.dto.UsuarioDto
import com.carpoolapp.data.remote.dto.VehiculoDto
import com.carpoolapp.data.remote.dto.toDto
import com.carpoolapp.domain.model.Vehiculo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreUsuarioDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection get() = firestore.collection("users")

    fun getUsuario(userId: String): Flow<UsuarioDto?> = callbackFlow {
        val currentUser = requireAuthOrClose(this) ?: return@callbackFlow
        val listener = collection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(); return@addSnapshotListener }
                if (snapshot == null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                // Manual deserialization for backwards compatibility
                val data = snapshot.data ?: emptyMap()
                val vehiculoData = data["vehiculo"]
                val vehiculoDto = when {
                    vehiculoData == null -> null
                    vehiculoData is String -> VehiculoDto(placa = vehiculoData) // Backwards compatibility
                    vehiculoData is Map<*, *> -> VehiculoDto(
                        marca = vehiculoData["marca"] as? String ?: "",
                        modelo = vehiculoData["modelo"] as? String ?: "",
                        ano = (vehiculoData["ano"] as? Long)?.toInt() ?: 0,
                        color = vehiculoData["color"] as? String ?: "",
                        placa = vehiculoData["placa"] as? String ?: "",
                        fotoUrl = vehiculoData["fotoUrl"] as? String?
                    )
                    else -> null
                }
                
                val usuario = UsuarioDto(
                    id = snapshot.id,
                    nombre = data["nombre"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    fotoUrl = data["fotoUrl"] as? String?,
                    vehiculo = vehiculoDto,
                    esConductor = data["esConductor"] as? Boolean ?: false,
                    calificacion = (data["calificacion"] as? Double) ?: (data["calificacion"] as? Long)?.toDouble() ?: 5.0,
                    viajesCompletados = (data["viajesCompletados"] as? Long)?.toInt() ?: 0,
                    fcmToken = data["fcmToken"] as? String?,
                    createdAt = data["createdAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now()
                )
                
                trySend(usuario)
            }
        awaitClose { listener.remove() }
    }

    suspend fun guardar(dto: UsuarioDto) {
        firestoreSafe("FirestoreUsuarioDS", Unit) {
            collection.document(dto.id).set(dto).await()
        }
    }

    suspend fun actualizarVehiculo(userId: String, vehiculo: Vehiculo) {
        firestoreSafe("FirestoreUsuarioDS", Unit) {
            val vehiculoDto = vehiculo.toDto()
            collection.document(userId).update("vehiculo", vehiculoDto).await()
        }
    }

    suspend fun actualizarFcmToken(userId: String, token: String) {
        firestoreSafe("FirestoreUsuarioDS", Unit) {
            collection.document(userId).update("fcmToken", token).await()
        }
    }

    suspend fun actualizarFotoUrl(userId: String, fotoUrl: String) {
        firestoreSafe("FirestoreUsuarioDS", Unit) {
            collection.document(userId).update("fotoUrl", fotoUrl).await()
        }
    }

    suspend fun actualizarEsConductor(userId: String, esConductor: Boolean) {
        firestoreSafe("FirestoreUsuarioDS", Unit) {
            collection.document(userId).update("esConductor", esConductor).await()
        }
    }

    suspend fun actualizarCalificacion(userId: String, calificacion: Double) {
        firestoreSafe("FirestoreUsuarioDS", Unit) {
            collection.document(userId).update("calificacion", calificacion).await()
        }
    }

    suspend fun incrementarViajesCompletados(userId: String) {
        firestoreSafe("FirestoreUsuarioDS", Unit) {
            val docRef = collection.document(userId)
            val doc = docRef.get().await()
            val actuales = (doc.getLong("viajesCompletados") ?: 0).toInt()
            docRef.update("viajesCompletados", actuales + 1).await()
        }
    }
}

package com.carpoolapp.data.remote.firestore

import com.carpoolapp.data.remote.dto.UsuarioDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.toObject
import android.util.Log
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
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w("FirestoreUsuarioDS", "No authenticated user — skipping usuario listener")
            close()
            return@callbackFlow
        }
        val listener = collection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreUsuarioDS", "Listener error", error)
                    close()
                    return@addSnapshotListener
                }
                val usuario = snapshot?.toObject<UsuarioDto>()?.copy(id = snapshot.id)
                trySend(usuario)
            }
        awaitClose { listener.remove() }
    }

    suspend fun guardar(dto: UsuarioDto) {
        collection.document(dto.id).set(dto).await()
    }

    suspend fun actualizarVehiculo(userId: String, vehiculo: String) {
        collection.document(userId).update("vehiculo", vehiculo).await()
    }

    suspend fun actualizarFcmToken(userId: String, token: String) {
        collection.document(userId).update("fcmToken", token).await()
    }
}

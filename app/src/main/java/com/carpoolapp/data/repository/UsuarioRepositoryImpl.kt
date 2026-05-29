package com.carpoolapp.data.repository

import com.carpoolapp.data.mapper.toDomain
import com.carpoolapp.data.mapper.toDto
import com.carpoolapp.data.remote.firestore.FirestoreUsuarioDataSource
import com.carpoolapp.domain.model.Usuario
import com.carpoolapp.domain.model.Vehiculo
import com.carpoolapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UsuarioRepositoryImpl @Inject constructor(
    private val dataSource: FirestoreUsuarioDataSource
) : UsuarioRepository {

    override fun getUsuario(id: String): Flow<Usuario?> {
        return dataSource.getUsuario(id).map { it?.toDomain() }
    }

    override suspend fun guardar(usuario: Usuario) {
        dataSource.guardar(usuario.toDto())
    }

    override suspend fun actualizarVehiculo(usuarioId: String, vehiculo: Vehiculo) {
        dataSource.actualizarVehiculo(usuarioId, vehiculo)
    }

    override suspend fun actualizarFcmToken(usuarioId: String, token: String) {
        dataSource.actualizarFcmToken(usuarioId, token)
    }

    override suspend fun actualizarFotoUrl(usuarioId: String, fotoUrl: String) {
        android.util.Log.d("UsuarioRepository", "Actualizando foto para usuario $usuarioId, tamaño: ${fotoUrl.length} chars")
        dataSource.actualizarFotoUrl(usuarioId, fotoUrl)
        android.util.Log.d("UsuarioRepository", "Foto actualizada en Firestore")
    }

    override suspend fun actualizarNombre(usuarioId: String, nombre: String) {
        dataSource.actualizarNombre(usuarioId, nombre)
    }

    override suspend fun actualizarEsConductor(usuarioId: String, esConductor: Boolean) {
        dataSource.actualizarEsConductor(usuarioId, esConductor)
    }

    override suspend fun actualizarCalificacion(usuarioId: String, calificacion: Double) {
        dataSource.actualizarCalificacion(usuarioId, calificacion)
    }

    override suspend fun incrementarViajesCompletados(usuarioId: String) {
        dataSource.incrementarViajesCompletados(usuarioId)
    }
    
    override suspend fun incrementarViajesComoConductor(usuarioId: String) {
        dataSource.incrementarCampo("viajesComoConductor", usuarioId)
    }
    
    override suspend fun incrementarViajesComoPasajero(usuarioId: String) {
        dataSource.incrementarCampo("viajesComoPasajero", usuarioId)
    }
}

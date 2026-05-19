package com.carpoolapp.data.repository

import com.carpoolapp.data.mapper.toDomain
import com.carpoolapp.data.mapper.toDto
import com.carpoolapp.data.remote.firestore.FirestoreUsuarioDataSource
import com.carpoolapp.domain.model.Usuario
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

    override suspend fun actualizarVehiculo(usuarioId: String, vehiculo: String) {
        dataSource.actualizarVehiculo(usuarioId, vehiculo)
    }

    override suspend fun actualizarFcmToken(usuarioId: String, token: String) {
        dataSource.actualizarFcmToken(usuarioId, token)
    }
}

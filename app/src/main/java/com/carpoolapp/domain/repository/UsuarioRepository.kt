package com.carpoolapp.domain.repository

import com.carpoolapp.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface UsuarioRepository {
    fun getUsuario(id: String): Flow<Usuario?>
    suspend fun guardar(usuario: Usuario)
    suspend fun actualizarVehiculo(usuarioId: String, vehiculo: String)
    suspend fun actualizarFcmToken(usuarioId: String, token: String)
}

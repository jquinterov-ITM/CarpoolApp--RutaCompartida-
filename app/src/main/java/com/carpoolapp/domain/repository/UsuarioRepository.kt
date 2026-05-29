package com.carpoolapp.domain.repository

import com.carpoolapp.domain.model.Usuario
import com.carpoolapp.domain.model.Vehiculo
import kotlinx.coroutines.flow.Flow

interface UsuarioRepository {
    fun getUsuario(id: String): Flow<Usuario?>
    suspend fun guardar(usuario: Usuario)
    suspend fun actualizarVehiculo(usuarioId: String, vehiculo: Vehiculo)
    suspend fun actualizarFcmToken(usuarioId: String, token: String)
    suspend fun actualizarFotoUrl(usuarioId: String, fotoUrl: String)
    suspend fun actualizarNombre(usuarioId: String, nombre: String)
    suspend fun actualizarEsConductor(usuarioId: String, esConductor: Boolean)
    suspend fun actualizarCalificacion(usuarioId: String, calificacion: Double)
    suspend fun incrementarViajesCompletados(usuarioId: String)
    suspend fun incrementarViajesComoConductor(usuarioId: String)
    suspend fun incrementarViajesComoPasajero(usuarioId: String)
}

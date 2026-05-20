package com.carpoolapp.domain.repository

import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.model.ViajeEstado
import kotlinx.coroutines.flow.Flow

interface ViajeRepository {
    fun getFeed(usuarioId: String, destino: String? = null): Flow<List<Viaje>>
    fun getViajesPorConductor(conductorId: String): Flow<List<Viaje>>
    suspend fun getViajesComoPasajero(pasajeroId: String): List<Viaje>
    suspend fun crear(viaje: Viaje): String
    suspend fun actualizarEstado(viajeId: String, estado: ViajeEstado)
    suspend fun seedDemoDataIfNeeded()
}

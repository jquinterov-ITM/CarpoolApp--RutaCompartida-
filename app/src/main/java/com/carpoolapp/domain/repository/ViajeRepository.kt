package com.carpoolapp.domain.repository

import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.model.ViajeEstado
import kotlinx.coroutines.flow.Flow

interface ViajeRepository {
    fun getFeed(usuarioId: String, destino: String? = null): Flow<List<Viaje>>
    fun getViajesPorConductor(conductorId: String): Flow<List<Viaje>>
    suspend fun getViajesComoPasajero(pasajeroId: String): List<Viaje>
    suspend fun getViajePorId(viajeId: String): Viaje?
    suspend fun crear(viaje: Viaje): String
    fun createdEvents(): kotlinx.coroutines.flow.Flow<Viaje>
    suspend fun actualizarEstado(viajeId: String, estado: ViajeEstado)
    suspend fun finalizarViaje(viajeId: String)
    suspend fun cancelarViaje(viajeId: String)
    suspend fun seedDemoDataIfNeeded()
}

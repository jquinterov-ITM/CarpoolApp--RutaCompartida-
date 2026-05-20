package com.carpoolapp.data.repository

import com.carpoolapp.data.mapper.toDomain
import com.carpoolapp.data.mapper.toDto
import com.carpoolapp.data.remote.firestore.FirestoreViajeDataSource
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.model.ViajeEstado
import com.carpoolapp.domain.repository.ViajeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ViajeRepositoryImpl @Inject constructor(
    private val dataSource: FirestoreViajeDataSource
) : ViajeRepository {

    override fun getFeed(usuarioId: String, destino: String?): Flow<List<Viaje>> {
        val flow = if (destino.isNullOrBlank()) {
            dataSource.getFeed(usuarioId)
        } else {
            dataSource.getFeedPorDestino(usuarioId, destino)
        }
        return flow.map { list -> list.map { it.toDomain() } }
    }

    override fun getViajesPorConductor(conductorId: String): Flow<List<Viaje>> {
        return dataSource.getViajesPorConductor(conductorId)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getViajesComoPasajero(pasajeroId: String): List<Viaje> {
        return dataSource.getViajesPorPasajero(pasajeroId).map { it.toDomain() }
    }

    override suspend fun crear(viaje: Viaje): String {
        return dataSource.crear(viaje.toDto())
    }

    override suspend fun actualizarEstado(viajeId: String, estado: ViajeEstado) {
        dataSource.actualizarEstado(viajeId, estado.name)
    }

    override suspend fun seedDemoDataIfNeeded() {
        dataSource.seedDemoDataIfNeeded()
    }
}

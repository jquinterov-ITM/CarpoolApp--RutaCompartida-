package com.carpoolapp.data.repository

import com.carpoolapp.data.mapper.toDomain
import com.carpoolapp.data.mapper.toDto
import com.carpoolapp.data.remote.firestore.FirestoreViajeDataSource
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.model.ViajeEstado
import com.carpoolapp.domain.repository.ViajeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

class ViajeRepositoryImpl @Inject constructor(
    private val dataSource: FirestoreViajeDataSource
) : ViajeRepository {

    // Emit events for newly created viajes so UI can update optimistically.
    // Set replay=1 so new subscribers (screens opened after creation)
    // receive the last created viaje immediately.
    private val _CreatedNotifier = MutableSharedFlow<com.carpoolapp.domain.model.Viaje>(replay = 1, extraBufferCapacity = 64)
    override fun createdEvents(): kotlinx.coroutines.flow.Flow<Viaje> = _CreatedNotifier.asSharedFlow()

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
        val id = dataSource.crear(viaje.toDto())
        // emit created viaje with id so listeners can update immediately
        try {
            _CreatedNotifier.tryEmit(viaje.copy(id = id))
            android.util.Log.d("ViajeRepo", "emitted created event id=$id")
        } catch (_: Exception) {}
        return id
    }

    override suspend fun actualizarEstado(viajeId: String, estado: ViajeEstado) {
        dataSource.actualizarEstado(viajeId, estado.name)
    }

    override suspend fun seedDemoDataIfNeeded() {
        dataSource.seedDemoDataIfNeeded()
    }
}

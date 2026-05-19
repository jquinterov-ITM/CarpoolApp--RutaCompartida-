package com.carpoolapp.data.repository

import com.carpoolapp.data.mapper.toDomain
import com.carpoolapp.data.mapper.toDto
import com.carpoolapp.data.remote.firestore.FirestoreSolicitudDataSource
import com.carpoolapp.domain.model.Solicitud
import com.carpoolapp.domain.repository.SolicitudRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SolicitudRepositoryImpl @Inject constructor(
    private val dataSource: FirestoreSolicitudDataSource
) : SolicitudRepository {

    override fun getSolicitudesPorViaje(tripId: String): Flow<List<Solicitud>> {
        return dataSource.getSolicitudesPorViaje(tripId)
            .map { list -> list.map { it.toDomain(tripId) } }
    }

    override suspend fun enviar(tripId: String, solicitud: Solicitud) {
        dataSource.crear(tripId, solicitud.toDto())
    }

    override suspend fun aceptar(tripId: String, requestId: String) {
        dataSource.aceptarConTransaction(tripId, requestId)
    }

    override suspend fun rechazar(tripId: String, requestId: String) {
        dataSource.actualizarEstado(tripId, requestId, "RECHAZADA")
    }
}

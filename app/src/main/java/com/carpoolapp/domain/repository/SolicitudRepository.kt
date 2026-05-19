package com.carpoolapp.domain.repository

import com.carpoolapp.domain.model.Solicitud
import com.carpoolapp.domain.model.SolicitudEstado
import kotlinx.coroutines.flow.Flow

interface SolicitudRepository {
    fun getSolicitudesPorViaje(tripId: String): Flow<List<Solicitud>>
    suspend fun enviar(tripId: String, solicitud: Solicitud)
    suspend fun aceptar(tripId: String, requestId: String)
    suspend fun rechazar(tripId: String, requestId: String)
}

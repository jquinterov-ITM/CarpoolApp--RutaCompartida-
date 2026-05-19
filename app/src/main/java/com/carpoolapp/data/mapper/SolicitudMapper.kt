package com.carpoolapp.data.mapper

import com.carpoolapp.data.remote.dto.SolicitudDto
import com.carpoolapp.domain.model.Solicitud
import com.carpoolapp.domain.model.SolicitudEstado

fun SolicitudDto.toDomain(tripId: String = ""): Solicitud = Solicitud(
    id = id,
    tripId = tripId,
    pasajeroId = pasajeroId,
    pasajeroNombre = pasajeroNombre,
    estado = try { SolicitudEstado.valueOf(estado) } catch (_: Exception) { SolicitudEstado.PENDIENTE }
)

fun Solicitud.toDto(): SolicitudDto = SolicitudDto(
    pasajeroId = pasajeroId,
    pasajeroNombre = pasajeroNombre,
    estado = estado.name
)

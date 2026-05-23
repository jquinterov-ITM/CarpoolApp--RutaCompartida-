package com.carpoolapp.data.mapper

import com.carpoolapp.data.remote.dto.SolicitudDto
import com.carpoolapp.domain.model.Solicitud
import com.carpoolapp.domain.model.SolicitudEstado

fun SolicitudDto.toDomain(tripId: String = ""): Solicitud {
    val solicitudEstado = try {
        SolicitudEstado.valueOf(estado)
    } catch (e: IllegalArgumentException) {
        android.util.Log.w("SolicitudMapper", "Unknown SolicitudEstado: $estado, defaulting to PENDIENTE", e)
        SolicitudEstado.PENDIENTE
    }
    
    return Solicitud(
        id = id,
        tripId = tripId,
        pasajeroId = pasajeroId,
        pasajeroNombre = pasajeroNombre,
        estado = solicitudEstado
    )
}

fun Solicitud.toDto(): SolicitudDto = SolicitudDto(
    pasajeroId = pasajeroId,
    pasajeroNombre = pasajeroNombre,
    estado = estado.name
)

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
        tripId = if (tripId.isNotEmpty()) tripId else this.tripId,
        pasajeroId = pasajeroId,
        pasajeroNombre = pasajeroNombre,
        pasajeroEmail = pasajeroEmail,
        pasajeroPhone = pasajeroPhone,
        pasajeroCalificacion = pasajeroCalificacion,
        asientosSolicitados = asientosSolicitados,
        mensaje = mensaje,
        estado = solicitudEstado,
        createdAt = createdAt.toDate().time
    )
}

fun Solicitud.toDto(): SolicitudDto = SolicitudDto(
    tripId = tripId,
    pasajeroId = pasajeroId,
    pasajeroNombre = pasajeroNombre,
    pasajeroEmail = pasajeroEmail,
    pasajeroPhone = pasajeroPhone,
    pasajeroCalificacion = pasajeroCalificacion,
    asientosSolicitados = asientosSolicitados,
    mensaje = mensaje,
    estado = estado.name,
    createdAt = if (createdAt > 0L) com.google.firebase.Timestamp(java.util.Date(createdAt)) else com.google.firebase.Timestamp.now()
)

package com.carpoolapp.domain.model

data class Solicitud(
    val id: String = "",
    val tripId: String = "",
    val pasajeroId: String = "",
    val pasajeroNombre: String = "",
    val estado: SolicitudEstado = SolicitudEstado.PENDIENTE
)

enum class SolicitudEstado { PENDIENTE, ACEPTADA, RECHAZADA }

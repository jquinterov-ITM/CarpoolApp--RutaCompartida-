package com.carpoolapp.domain.model

data class Solicitud(
    val id: String = "",
    val tripId: String = "",
    val pasajeroId: String = "",
    val pasajeroNombre: String = "",
    val pasajeroEmail: String = "",
    val pasajeroPhone: String = "",
    val pasajeroCalificacion: Double = 5.0,
    val asientosSolicitados: Int = 1,
    val mensaje: String = "",
    val estado: SolicitudEstado = SolicitudEstado.PENDIENTE,
    val createdAt: Long = 0L
)

enum class SolicitudEstado { PENDIENTE, ACEPTADA, RECHAZADA, CANCELADA }

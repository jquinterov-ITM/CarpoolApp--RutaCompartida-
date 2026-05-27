package com.carpoolapp.data.remote.dto

import com.google.firebase.Timestamp

data class SolicitudDto(
    val id: String = "",
    val tripId: String = "",
    val pasajeroId: String = "",
    val pasajeroNombre: String = "",
    val pasajeroEmail: String = "",
    val pasajeroPhone: String = "",
    val pasajeroCalificacion: Double = 5.0,
    val asientosSolicitados: Int = 1,
    val mensaje: String = "",
    val estado: String = "PENDIENTE",
    val createdAt: Timestamp = Timestamp.now()
) {
    companion object {
        fun fromDocument(id: String, map: Map<String, Any?>): SolicitudDto = SolicitudDto(
            id = id,
            tripId = map["tripId"] as? String ?: "",
            pasajeroId = map["pasajeroId"] as? String ?: "",
            pasajeroNombre = map["pasajeroNombre"] as? String ?: "",
            pasajeroEmail = map["pasajeroEmail"] as? String ?: "",
            pasajeroPhone = map["pasajeroPhone"] as? String ?: "",
            pasajeroCalificacion = (map["pasajeroCalificacion"] as? Double) ?: (map["pasajeroCalificacion"] as? Long)?.toDouble() ?: 5.0,
            asientosSolicitados = (map["asientosSolicitados"] as? Long)?.toInt() ?: 1,
            mensaje = map["mensaje"] as? String ?: "",
            estado = map["estado"] as? String ?: "PENDIENTE",
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
        )
    }
}

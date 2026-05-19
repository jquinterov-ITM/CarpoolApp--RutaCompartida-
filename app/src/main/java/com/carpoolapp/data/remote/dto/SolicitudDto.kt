package com.carpoolapp.data.remote.dto

import com.google.firebase.Timestamp

data class SolicitudDto(
    val id: String = "",
    val pasajeroId: String = "",
    val pasajeroNombre: String = "",
    val estado: String = "PENDIENTE",
    val createdAt: Timestamp = Timestamp.now()
) {
    companion object {
        fun fromDocument(id: String, map: Map<String, Any?>): SolicitudDto = SolicitudDto(
            id = id,
            pasajeroId = map["pasajeroId"] as? String ?: "",
            pasajeroNombre = map["pasajeroNombre"] as? String ?: "",
            estado = map["estado"] as? String ?: "PENDIENTE",
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
        )
    }
}

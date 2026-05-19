package com.carpoolapp.data.remote.dto

import com.google.firebase.Timestamp

data class ViajeDto(
    val id: String = "",
    val conductorId: String = "",
    val conductorNombre: String = "",
    val origen: String = "",
    val destino: String = "",
    val fechaHora: Timestamp = Timestamp.now(),
    val asientosDisponibles: Int = 0,
    val tipo: String = "PROGRAMADO",
    val estado: String = "PROGRAMADO",
    val createdAt: Timestamp = Timestamp.now()
) {
    companion object {
        fun fromDocument(id: String, map: Map<String, Any?>): ViajeDto = ViajeDto(
            id = id,
            conductorId = map["conductorId"] as? String ?: "",
            conductorNombre = map["conductorNombre"] as? String ?: "",
            origen = map["origen"] as? String ?: "",
            destino = map["destino"] as? String ?: "",
            fechaHora = map["fechaHora"] as? Timestamp ?: Timestamp.now(),
            asientosDisponibles = (map["asientosDisponibles"] as? Long)?.toInt() ?: 0,
            tipo = map["tipo"] as? String ?: "PROGRAMADO",
            estado = map["estado"] as? String ?: "PROGRAMADO",
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
        )
    }
}

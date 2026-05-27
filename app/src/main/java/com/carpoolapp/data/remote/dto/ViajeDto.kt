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
    val asientosTotales: Int = 0,
    val precio: Double? = null,
    val descripcion: String = "",
    val pasajeroIds: List<String> = emptyList(),
    val tipo: String = "PROGRAMADO",
    val estado: String = "PROGRAMADO",
    val vehiculoConductor: VehiculoDto? = null,
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
            asientosTotales = (map["asientosTotales"] as? Long)?.toInt() ?: 0,
            precio = (map["precio"] as? Double) ?: (map["precio"] as? Long)?.toDouble(),
            descripcion = map["descripcion"] as? String ?: "",
            pasajeroIds = (map["pasajeroIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            tipo = map["tipo"] as? String ?: "PROGRAMADO",
            estado = map["estado"] as? String ?: "PROGRAMADO",
            vehiculoConductor = (map["vehiculoConductor"] as? Map<*, *>)?.let { data ->
                VehiculoDto(
                    marca = data["marca"] as? String ?: "",
                    modelo = data["modelo"] as? String ?: "",
                    ano = (data["ano"] as? Long)?.toInt() ?: 0,
                    color = data["color"] as? String ?: "",
                    placa = data["placa"] as? String ?: "",
                    fotoUrl = data["fotoUrl"] as? String?
                )
            },
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
        )
    }
}

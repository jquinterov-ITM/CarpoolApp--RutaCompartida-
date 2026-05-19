package com.carpoolapp.domain.model

data class Viaje(
    val id: String = "",
    val conductorId: String = "",
    val conductorNombre: String = "",
    val origen: String = "",
    val destino: String = "",
    val fechaHora: Long = 0L,
    val asientosDisponibles: Int = 0,
    val tipo: TipoViaje = TipoViaje.PROGRAMADO,
    val estado: ViajeEstado = ViajeEstado.PROGRAMADO
)

enum class TipoViaje { INMEDIATO, PROGRAMADO }
enum class ViajeEstado { PROGRAMADO, ACTIVO, COMPLETADO, CANCELADO }

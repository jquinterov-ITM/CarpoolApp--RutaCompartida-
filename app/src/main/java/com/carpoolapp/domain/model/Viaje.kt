package com.carpoolapp.domain.model

data class Viaje(
    val id: String = "",
    val conductorId: String = "",
    val conductorNombre: String = "",
    val origen: String = "",
    val destino: String = "",
    val fechaHora: Long = 0L,
    val asientosDisponibles: Int = 0,
    val asientosTotales: Int = 0,
    val precio: Double? = null,
    val descripcion: String = "",
    val pasajeroIds: List<String> = emptyList(),
    val tipo: TipoViaje = TipoViaje.PROGRAMADO,
    val estado: ViajeEstado = ViajeEstado.PROGRAMADO,
    val vehiculoConductor: com.carpoolapp.domain.model.Vehiculo? = null
)

enum class TipoViaje { INMEDIATO, PROGRAMADO }
enum class ViajeEstado { PROGRAMADO, ACTIVO, EN_PROGRESO, COMPLETADO, CANCELADO }

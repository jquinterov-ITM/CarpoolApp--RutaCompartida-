package com.carpoolapp.data.mapper

import com.carpoolapp.data.remote.dto.ViajeDto
import com.carpoolapp.domain.model.TipoViaje
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.model.ViajeEstado

fun ViajeDto.toDomain(): Viaje {
    val tipoViaje = try {
        TipoViaje.valueOf(tipo)
    } catch (e: IllegalArgumentException) {
        android.util.Log.w("ViajeMapper", "Unknown TipoViaje: $tipo, defaulting to PROGRAMADO", e)
        TipoViaje.PROGRAMADO
    }
    
    val viajeEstado = try {
        ViajeEstado.valueOf(estado)
    } catch (e: IllegalArgumentException) {
        android.util.Log.w("ViajeMapper", "Unknown ViajeEstado: $estado, defaulting to PROGRAMADO", e)
        ViajeEstado.PROGRAMADO
    }
    
    return Viaje(
        id = id,
        conductorId = conductorId,
        conductorNombre = conductorNombre,
        origen = origen,
        destino = destino,
        fechaHora = fechaHora.toDate().time,
        asientosDisponibles = asientosDisponibles,
        tipo = tipoViaje,
        estado = viajeEstado
    )
}

fun Viaje.toDto(): ViajeDto = ViajeDto(
    conductorId = conductorId,
    conductorNombre = conductorNombre,
    origen = origen,
    destino = destino,
    asientosDisponibles = asientosDisponibles,
    tipo = tipo.name,
    estado = estado.name
)

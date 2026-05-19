package com.carpoolapp.data.mapper

import com.carpoolapp.data.remote.dto.ViajeDto
import com.carpoolapp.domain.model.TipoViaje
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.model.ViajeEstado

fun ViajeDto.toDomain(): Viaje = Viaje(
    id = id,
    conductorId = conductorId,
    conductorNombre = conductorNombre,
    origen = origen,
    destino = destino,
    fechaHora = fechaHora.seconds * 1000,
    asientosDisponibles = asientosDisponibles,
    tipo = try { TipoViaje.valueOf(tipo) } catch (_: Exception) { TipoViaje.PROGRAMADO },
    estado = try { ViajeEstado.valueOf(estado) } catch (_: Exception) { ViajeEstado.PROGRAMADO }
)

fun Viaje.toDto(): ViajeDto = ViajeDto(
    conductorId = conductorId,
    conductorNombre = conductorNombre,
    origen = origen,
    destino = destino,
    asientosDisponibles = asientosDisponibles,
    tipo = tipo.name,
    estado = estado.name
)

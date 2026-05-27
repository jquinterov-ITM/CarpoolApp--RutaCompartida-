package com.carpoolapp.data.mapper

import com.carpoolapp.data.remote.dto.UsuarioDto
import com.carpoolapp.data.remote.dto.toDomain
import com.carpoolapp.data.remote.dto.toDto
import com.carpoolapp.domain.model.Usuario
import com.carpoolapp.domain.model.Vehiculo

fun UsuarioDto.toDomain(): Usuario = Usuario(
    id = id,
    nombre = nombre,
    email = email,
    fotoUrl = fotoUrl,
    vehiculo = vehiculo?.toDomain(),
    esConductor = esConductor,
    calificacion = calificacion,
    viajesCompletados = viajesCompletados
)

fun Usuario.toDto(): UsuarioDto = UsuarioDto(
    id = id,
    nombre = nombre,
    email = email,
    fotoUrl = fotoUrl,
    vehiculo = vehiculo?.toDto(),
    esConductor = esConductor,
    calificacion = calificacion,
    viajesCompletados = viajesCompletados
)

package com.carpoolapp.data.mapper

import com.carpoolapp.data.remote.dto.UsuarioDto
import com.carpoolapp.domain.model.Usuario

fun UsuarioDto.toDomain(): Usuario = Usuario(
    id = id,
    nombre = nombre,
    email = email,
    vehiculo = vehiculo
)

fun Usuario.toDto(): UsuarioDto = UsuarioDto(
    id = id,
    nombre = nombre,
    email = email,
    vehiculo = vehiculo
)

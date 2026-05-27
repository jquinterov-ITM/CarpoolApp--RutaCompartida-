package com.carpoolapp.data.remote.dto

data class VehiculoDto(
    val marca: String = "",
    val modelo: String = "",
    val ano: Int = 0,
    val color: String = "",
    val placa: String = "",
    val fotoUrl: String? = null
)

fun VehiculoDto.toDomain(): com.carpoolapp.domain.model.Vehiculo {
    return com.carpoolapp.domain.model.Vehiculo(
        marca = marca,
        modelo = modelo,
        ano = ano,
        color = color,
        placa = placa,
        fotoUrl = fotoUrl
    )
}

fun com.carpoolapp.domain.model.Vehiculo.toDto(): VehiculoDto {
    return VehiculoDto(
        marca = marca,
        modelo = modelo,
        ano = ano,
        color = color,
        placa = placa,
        fotoUrl = fotoUrl
    )
}

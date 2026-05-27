package com.carpoolapp.domain.model

import com.carpoolapp.data.remote.dto.VehiculoDto

data class Vehiculo(
    val marca: String = "",
    val modelo: String = "",
    val ano: Int = 0,
    val color: String = "",
    val placa: String = "",
    val fotoUrl: String? = null
)

fun Vehiculo.toDto() = VehiculoDto(
    marca = marca,
    modelo = modelo,
    ano = ano,
    color = color,
    placa = placa,
    fotoUrl = fotoUrl
)

fun VehiculoDto.toDomain() = Vehiculo(
    marca = marca,
    modelo = modelo,
    ano = ano,
    color = color,
    placa = placa,
    fotoUrl = fotoUrl
)

enum class MarcaVehiculo(val displayName: String) {
    TOYOTA("Toyota"),
    NISSAN("Nissan"),
    CHEVROLET("Chevrolet"),
    FORD("Ford"),
    HONDA("Honda"),
    HYUNDAI("Hyundai"),
    KIA("Kia"),
    MAZDA("Mazda"),
    VOLKSWAGEN("Volkswagen"),
    RENAULT("Renault"),
    PEUGEOT("Peugeot"),
    CITROEN("Citroën"),
    FIAT("Fiat"),
    JEEP("Jeep"),
    BMW("BMW"),
    MERCEDES_BENZ("Mercedes-Benz"),
    AUDI("Audi"),
    SUBARU("Subaru"),
    MITSUBISHI("Mitsubishi"),
    SUZUKI("Suzuki"),
    VOLVO("Volvo"),
    LEXUS("Lexus"),
    TESLA("Tesla"),
    OTRO("Otro")
}

val COLORES_VEHICULO = listOf(
    "Blanco",
    "Negro",
    "Gris",
    "Plateado",
    "Rojo",
    "Azul",
    "Verde",
    "Amarillo",
    "Naranja",
    "Café",
    "Beige",
    "Dorado",
    "Morado",
    "Rosa",
    "Otro"
)

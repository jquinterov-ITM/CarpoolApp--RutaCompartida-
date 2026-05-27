package com.carpoolapp.domain.model

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val fotoUrl: String? = null,
    val vehiculo: Vehiculo? = null,
    val esConductor: Boolean = false,
    val calificacion: Double = 5.0,
    val viajesCompletados: Int = 0
)

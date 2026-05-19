package com.carpoolapp.domain.model

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val vehiculo: String? = null
)

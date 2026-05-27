package com.carpoolapp.data.remote.dto

import com.google.firebase.Timestamp

data class UsuarioDto(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val fotoUrl: String? = null,
    val vehiculo: VehiculoDto? = null,
    val esConductor: Boolean = false,
    val calificacion: Double = 5.0,
    val viajesCompletados: Int = 0,
    val fcmToken: String? = null,
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)

package com.carpoolapp.data.remote.dto

import com.google.firebase.Timestamp

data class UsuarioDto(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val vehiculo: String? = null,
    val fcmToken: String? = null,
    val createdAt: Timestamp = Timestamp.now()
) {
    companion object {
        fun fromDocument(id: String, map: Map<String, Any?>): UsuarioDto = UsuarioDto(
            id = id,
            nombre = map["nombre"] as? String ?: "",
            email = map["email"] as? String ?: "",
            vehiculo = map["vehiculo"] as? String?,
            fcmToken = map["fcmToken"] as? String?,
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
        )
    }
}

package com.carpoolapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "viajes")
data class ViajeEntity(
    @PrimaryKey val id: String,
    val conductorId: String,
    val conductorNombre: String,
    val origen: String,
    val destino: String,
    val fechaHora: Long,
    val asientosDisponibles: Int,
    val tipo: String,
    val estado: String
)

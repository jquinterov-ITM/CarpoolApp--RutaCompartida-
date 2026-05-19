package com.carpoolapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.carpoolapp.data.local.dao.UsuarioDao
import com.carpoolapp.data.local.dao.ViajeDao
import com.carpoolapp.data.local.entity.UsuarioEntity
import com.carpoolapp.data.local.entity.ViajeEntity

@Database(
    entities = [UsuarioEntity::class, ViajeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun viajeDao(): ViajeDao
}

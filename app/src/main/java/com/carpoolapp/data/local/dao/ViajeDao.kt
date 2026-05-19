package com.carpoolapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.carpoolapp.data.local.entity.ViajeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ViajeDao {
    @Query("SELECT * FROM viajes WHERE destino LIKE '%' || :destino || '%' ORDER BY fechaHora ASC")
    fun buscarPorDestino(destino: String): Flow<List<ViajeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(viajes: List<ViajeEntity>)

    @Query("DELETE FROM viajes")
    suspend fun eliminarTodos()
}

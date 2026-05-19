package com.carpoolapp.di

import android.content.Context
import androidx.room.Room
import com.carpoolapp.data.local.dao.UsuarioDao
import com.carpoolapp.data.local.dao.ViajeDao
import com.carpoolapp.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "carpoolapp.db")
            .build()

    @Provides
    fun provideUsuarioDao(db: AppDatabase): UsuarioDao = db.usuarioDao()

    @Provides
    fun provideViajeDao(db: AppDatabase): ViajeDao = db.viajeDao()
}

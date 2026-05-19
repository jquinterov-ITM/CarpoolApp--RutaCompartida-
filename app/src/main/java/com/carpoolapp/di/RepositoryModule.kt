package com.carpoolapp.di

import com.carpoolapp.data.repository.SolicitudRepositoryImpl
import com.carpoolapp.data.repository.UsuarioRepositoryImpl
import com.carpoolapp.data.repository.ViajeRepositoryImpl
import com.carpoolapp.domain.repository.SolicitudRepository
import com.carpoolapp.domain.repository.UsuarioRepository
import com.carpoolapp.domain.repository.ViajeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindViajeRepo(impl: ViajeRepositoryImpl): ViajeRepository

    @Binds
    abstract fun bindSolicitudRepo(impl: SolicitudRepositoryImpl): SolicitudRepository

    @Binds
    abstract fun bindUsuarioRepo(impl: UsuarioRepositoryImpl): UsuarioRepository
}

package com.carpoolapp.domain.usecase

import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.repository.ViajeRepository
import javax.inject.Inject

class PublicarViajeUseCase @Inject constructor(
    private val viajeRepository: ViajeRepository
) {
    suspend operator fun invoke(viaje: Viaje): String {
        return viajeRepository.crear(viaje)
    }
}

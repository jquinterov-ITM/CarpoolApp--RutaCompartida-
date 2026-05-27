package com.carpoolapp.domain.usecase

import com.carpoolapp.domain.repository.ViajeRepository
import javax.inject.Inject

class FinalizarViajeUseCase @Inject constructor(
    private val viajeRepository: ViajeRepository
) {
    suspend operator fun invoke(viajeId: String) {
        viajeRepository.finalizarViaje(viajeId)
    }
}

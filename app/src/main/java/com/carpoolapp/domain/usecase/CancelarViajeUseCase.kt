package com.carpoolapp.domain.usecase

import com.carpoolapp.domain.model.ViajeEstado
import com.carpoolapp.domain.repository.ViajeRepository
import javax.inject.Inject

class CancelarViajeUseCase @Inject constructor(
    private val viajeRepository: ViajeRepository
) {
    suspend operator fun invoke(viajeId: String) {
        viajeRepository.actualizarEstado(viajeId, ViajeEstado.CANCELADO)
    }
}

package com.carpoolapp.domain.usecase

import com.carpoolapp.domain.repository.SolicitudRepository
import javax.inject.Inject

class AceptarSolicitudUseCase @Inject constructor(
    private val solicitudRepository: SolicitudRepository
) {
    suspend operator fun invoke(tripId: String, requestId: String) {
        solicitudRepository.aceptar(tripId, requestId)
    }
}

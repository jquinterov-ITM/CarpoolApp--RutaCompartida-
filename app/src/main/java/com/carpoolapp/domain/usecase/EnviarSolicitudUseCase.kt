package com.carpoolapp.domain.usecase

import com.carpoolapp.domain.model.Solicitud
import com.carpoolapp.domain.repository.SolicitudRepository
import javax.inject.Inject

class EnviarSolicitudUseCase @Inject constructor(
    private val solicitudRepository: SolicitudRepository
) {
    suspend operator fun invoke(tripId: String, solicitud: Solicitud) {
        solicitudRepository.enviar(tripId, solicitud)
    }
}

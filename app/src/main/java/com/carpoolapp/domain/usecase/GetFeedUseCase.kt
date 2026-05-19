package com.carpoolapp.domain.usecase

import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.repository.ViajeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFeedUseCase @Inject constructor(
    private val viajeRepository: ViajeRepository
) {
    operator fun invoke(usuarioId: String): Flow<List<Viaje>> {
        return viajeRepository.getFeed(usuarioId)
    }
}

package com.carpoolapp.domain.usecase

import com.carpoolapp.domain.repository.UsuarioRepository
import com.carpoolapp.domain.repository.ViajeRepository
import javax.inject.Inject

class FinalizarViajeUseCase @Inject constructor(
    private val viajeRepository: ViajeRepository,
    private val usuarioRepository: UsuarioRepository
) {
    suspend operator fun invoke(viajeId: String) {
        // Finalizar el viaje
        viajeRepository.finalizarViaje(viajeId)
        
        // Obtener el viaje para saber conductor y pasajeros
        val viaje = viajeRepository.getViajePorId(viajeId)
        if (viaje != null) {
            // Incrementar viajes como CONDUCTOR
            try {
                usuarioRepository.incrementarViajesComoConductor(viaje.conductorId)
            } catch (e: Exception) {
                android.util.Log.e("FinalizarViaje", "Error incrementando viajes conductor: ${e.message}")
            }
            
            // Incrementar viajes como PASAJERO a cada pasajero
            viaje.pasajeroIds.forEach { pasajeroId ->
                try {
                    usuarioRepository.incrementarViajesComoPasajero(pasajeroId)
                } catch (e: Exception) {
                    android.util.Log.e("FinalizarViaje", "Error incrementando viajes pasajero $pasajeroId: ${e.message}")
                }
            }
        }
    }
}

package com.carpoolapp.domain.usecase

import android.util.Log
import com.carpoolapp.domain.model.Solicitud
import com.carpoolapp.domain.model.SolicitudEstado
import com.carpoolapp.domain.repository.SolicitudRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class EnviarSolicitudUseCase @Inject constructor(
    private val solicitudRepository: SolicitudRepository
) {
    suspend operator fun invoke(tripId: String, solicitud: Solicitud): Result<Unit> {
        return try {
            val solicitudesExistentes = solicitudRepository.getSolicitudesPorViaje(tripId).first()
            val solicitudPendiente = solicitudesExistentes.find { 
                it.pasajeroId == solicitud.pasajeroId && 
                it.estado == SolicitudEstado.PENDIENTE 
            }
            
            if (solicitudPendiente != null) {
                Result.failure(Exception("Ya tienes una solicitud pendiente para este viaje"))
            } else {
                // Verificar si fue rechazada previamente para permitir reenviar
                val solicitudRechazada = solicitudesExistentes.find { 
                    it.pasajeroId == solicitud.pasajeroId && 
                    it.estado == SolicitudEstado.RECHAZADA 
                }
                
                if (solicitudRechazada != null) {
                    Log.d("EnviarSolicitud", "Reenviando solicitud previamente rechazada")
                }
                
                solicitudRepository.enviar(tripId, solicitud)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("EnviarSolicitud", "Error al enviar solicitud: ${e.message}", e)
            Result.failure(e)
        }
    }
}

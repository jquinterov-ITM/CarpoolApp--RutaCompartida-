package com.carpoolapp.domain.usecase

import kotlinx.coroutines.flow.first
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.model.ViajeEstado
import com.carpoolapp.domain.repository.ViajeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.assertEquals

class GetFeedUseCaseTest {

    private val sample = Viaje(
        id = "t1",
        conductorId = "c1",
        conductorNombre = "Juan Perez",
        origen = "A",
        destino = "B",
        fechaHora = 0L,
        asientosDisponibles = 3,
        estado = ViajeEstado.PROGRAMADO
    )

    private class FakeRepo(private val lista: List<Viaje>) : ViajeRepository {
        override fun getFeed(usuarioId: String, destino: String?): Flow<List<Viaje>> = flowOf(lista)
        override fun getViajesPorConductor(conductorId: String) = flowOf(emptyList<Viaje>())
        override suspend fun getViajesComoPasajero(pasajeroId: String): List<Viaje> = emptyList()
        override suspend fun crear(viaje: Viaje): String = ""
        override suspend fun actualizarEstado(viajeId: String, estado: ViajeEstado) {}
        override suspend fun seedDemoDataIfNeeded() {}
    }

    @Test
    fun `invoke returns feed from repository`() = runTest {
        val repo = FakeRepo(listOf(sample))
        val useCase = GetFeedUseCase(repo)

        val emitted = useCase("u1").first()
        org.junit.Assert.assertEquals(1, emitted.size)
        org.junit.Assert.assertEquals("t1", emitted[0].id)
    }
}

package com.carpoolapp.domain.usecase

import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.model.ViajeEstado
import com.carpoolapp.domain.repository.ViajeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.assertEquals

class PublicarViajeUseCaseTest {

    @Test
    fun `invoke calls repository crear and returns id`() = runTest {
        val viaje = Viaje(
            id = "",
            conductorId = "c1",
            conductorNombre = "Ana",
            origen = "X",
            destino = "Y",
            fechaHora = 0L,
            asientosDisponibles = 4,
            estado = ViajeEstado.PROGRAMADO
        )
        val repo = mockk<ViajeRepository>()
        coEvery { repo.crear(viaje) } returns "newId"

        val useCase = PublicarViajeUseCase(repo)
        val result = useCase(viaje)

        assertEquals("newId", result)
        coVerify { repo.crear(viaje) }
    }
}

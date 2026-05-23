package com.carpoolapp.ui.home

import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.model.ViajeEstado
import com.carpoolapp.domain.usecase.GetFeedUseCase
import com.carpoolapp.domain.repository.ViajeRepository
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val sample = Viaje(
        id = "t1",
        conductorId = "c1",
        conductorNombre = "Juan",
        origen = "A",
        destino = "B",
        fechaHora = 0L,
        asientosDisponibles = 2,
        estado = ViajeEstado.PROGRAMADO
    )

    @Test
    fun `cargarFeed emits Success when repository returns list`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        val viajeRepo = mockk<ViajeRepository>(relaxed = true)
        coEvery { viajeRepo.seedDemoDataIfNeeded() } returns Unit

        val feedFlow = flow { emit(listOf(sample)) }
        val getFeed = mockk<GetFeedUseCase>()
        every { getFeed.invoke(any()) } returns feedFlow

        // mock FirebaseAuth and user
        val auth = mockk<com.google.firebase.auth.FirebaseAuth>(relaxed = true)
        val user = mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)
        every { auth.currentUser } returns user
        every { user.uid } returns "u1"

        val vm = HomeViewModel(getFeed, viajeRepo, auth)

        // advance coroutine execution in the test scope
        advanceUntilIdle()

        val state = vm.uiState.value
        when (state) {
            is HomeUiState.Success -> org.junit.Assert.assertEquals(1, state.viajes.size)
            else -> throw AssertionError("Expected Success state")
        }

        coVerify { viajeRepo.seedDemoDataIfNeeded() }
    }

    @Test
    fun `cargarFeed emits Error when no user`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        val viajeRepo = mockk<ViajeRepository>(relaxed = true)
        val getFeed = mockk<GetFeedUseCase>(relaxed = true)
        val auth = mockk<com.google.firebase.auth.FirebaseAuth>(relaxed = true)
        every { auth.currentUser } returns null

        val vm = HomeViewModel(getFeed, viajeRepo, auth)
        advanceUntilIdle()

        val state = vm.uiState.value
        when (state) {
            is HomeUiState.Error -> org.junit.Assert.assertEquals("Inicia sesion para ver viajes", state.mensaje)
            else -> throw AssertionError("Expected Error state when no user")
        }
    }
}

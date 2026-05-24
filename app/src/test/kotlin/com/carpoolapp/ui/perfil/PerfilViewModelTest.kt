package com.carpoolapp.ui.perfil

import app.cash.turbine.test
import com.carpoolapp.domain.model.Usuario
import com.carpoolapp.domain.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Test

class PerfilViewModelTest {

    private class FakeUsuarioRepository(initial: Usuario?) : UsuarioRepository {
        private val _state = MutableStateFlow<Usuario?>(initial)
        override fun getUsuario(id: String): Flow<Usuario?> = _state.asStateFlow()
        override suspend fun guardar(usuario: Usuario) { _state.value = usuario }
        override suspend fun actualizarVehiculo(usuarioId: String, vehiculo: String) {
            val current = _state.value
            if (current != null && current.id == usuarioId) {
                _state.value = current.copy(vehiculo = vehiculo)
            } else {
                _state.value = Usuario(id = usuarioId, nombre = "", email = "", vehiculo = vehiculo)
            }
        }
        override suspend fun actualizarFcmToken(usuarioId: String, token: String) {}
    }

    @Test
    fun cargaPerfil_y_actualizaVehiculo_emiteEstados() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        val usuarioInicial = Usuario(id = "u1", nombre = "Juan", email = "j@x.com", vehiculo = null)
        val repo = FakeUsuarioRepository(usuarioInicial)

        val auth = mockk<FirebaseAuth>()
        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user
        every { user.uid } returns "u1"

        val vm = PerfilViewModel(repo, auth)

        vm.uiState.test {
            // Primera emisión: Loading
            val primera = awaitItem()
            assert(primera is PerfilUiState.Loading)

            // Luego debe llegar Success con el usuario cargado
            val success = awaitItem()
            assert(success is PerfilUiState.Success)

            // Llamar a actualizar vehículo
            vm.actualizarVehiculo("Toyota Corolla")

            // Debería emitirse VehiculoActualizado
            val actualizado = awaitItem()
            assert(actualizado is PerfilUiState.VehiculoActualizado)

            // Y luego la fuente de datos actualizada volverá a emitir Success
            val after = awaitItem()
            if (after is PerfilUiState.Success) {
                val u = after.usuario
                assert(u.vehiculo == "Toyota Corolla")
            } else {
                throw AssertionError("Se esperaba Success tras actualizar vehículo")
            }

            cancelAndIgnoreRemainingEvents()
        }
        Dispatchers.resetMain()
    }
}

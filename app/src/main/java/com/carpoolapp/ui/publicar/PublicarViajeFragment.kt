package com.carpoolapp.ui.publicar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.carpoolapp.databinding.FragmentPublicarViajeBinding
import com.carpoolapp.domain.model.TipoViaje
import com.carpoolapp.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PublicarViajeFragment : BaseFragment<FragmentPublicarViajeBinding>() {

    private val viewModel: PublicarViajeViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentPublicarViajeBinding =
        FragmentPublicarViajeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPublicar.setOnClickListener {
            val origen = binding.origenInput.text.toString().trim()
            val destino = binding.destinoInput.text.toString().trim()
            val asientos = binding.asientosInput.text.toString().toIntOrNull() ?: 0
            val tipo = if (binding.btnInmediato.isChecked) TipoViaje.INMEDIATO else TipoViaje.PROGRAMADO

            if (origen.isNotBlank() && destino.isNotBlank() && asientos > 0) {
                viewModel.publicar(origen, destino, asientos, tipo)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PublicarUiState.Publicando -> {
                            binding.btnPublicar.isEnabled = false
                            binding.btnPublicar.text = "Publicando…"
                        }
                        is PublicarUiState.Exitoso -> {
                            findNavController().navigateUp()
                        }
                        is PublicarUiState.Error -> {
                            binding.btnPublicar.isEnabled = true
                            binding.btnPublicar.text = "Publicar Viaje"
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

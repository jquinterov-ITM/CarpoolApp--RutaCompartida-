package com.carpoolapp.ui.publicar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        // Dar feedback visual manteniendo la selección exclusiva del grupo
        binding.tipoToggle.addOnButtonCheckedListener { group, checkedId, isChecked ->
            val button = group.findViewById<com.google.android.material.button.MaterialButton>(checkedId)
            if (isChecked) {
                button.setBackgroundColor(requireContext().getColor(com.carpoolapp.R.color.primary))
                button.setTextColor(requireContext().getColor(com.carpoolapp.R.color.on_primary))
            } else {
                button.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                button.setTextColor(requireContext().getColor(com.carpoolapp.R.color.text_primary))
            }
        }

        binding.btnPublicar.setOnClickListener {
            val origen = binding.origenInput.text.toString().trim()
            val destino = binding.destinoInput.text.toString().trim()
            val asientos = binding.asientosInput.text.toString().toIntOrNull() ?: 0
            val tipo = if (binding.btnInmediato.isChecked) TipoViaje.INMEDIATO else TipoViaje.PROGRAMADO

            if (origen.isBlank() || destino.isBlank()) {
                Toast.makeText(requireContext(), "Ingresa origen y destino", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (asientos <= 0) {
                Toast.makeText(requireContext(), "Ingresa al menos 1 asiento", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.publicar(origen, destino, asientos, tipo)
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
                            Toast.makeText(
                                requireContext(),
                                "✅ Viaje publicado exitosamente",
                                Toast.LENGTH_LONG
                            ).show()
                            findNavController().navigateUp()
                        }
                        is PublicarUiState.Error -> {
                            binding.btnPublicar.isEnabled = true
                            binding.btnPublicar.text = "Publicar Viaje"
                            Toast.makeText(
                                requireContext(),
                                "❌ Error: ${state.mensaje}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

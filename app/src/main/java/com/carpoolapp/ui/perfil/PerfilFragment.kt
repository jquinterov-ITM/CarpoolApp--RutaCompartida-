package com.carpoolapp.ui.perfil

import android.graphics.Color
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
import com.carpoolapp.databinding.FragmentPerfilBinding
import com.carpoolapp.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PerfilFragment : BaseFragment<FragmentPerfilBinding>() {

    private val viewModel: PerfilViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentPerfilBinding = FragmentPerfilBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGuardar.setOnClickListener {
            val vehiculo = binding.vehiculoInput.text.toString().trim()
            if (vehiculo.isBlank()) {
                Toast.makeText(requireContext(), "Ingresa la información del vehículo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.actualizarVehiculo(vehiculo)
        }

        binding.btnCerrarSesion.setOnClickListener {
            viewModel.cerrarSesion()
            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
            findNavController().navigate(
                com.carpoolapp.R.id.authFragment,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(com.carpoolapp.R.id.nav_graph, true)
                    .build()
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PerfilUiState.Success -> {
                            val u = state.usuario
                            val initials = u.nombre.split(" ")
                                .joinToString("") { it.take(1).uppercase() }
                                .take(2)
                            binding.avatar.text = initials.ifEmpty { "?" }
                            binding.tvNombre.text = u.nombre
                            binding.tvEmail.text = u.email
                            binding.vehiculoInput.setText(u.vehiculo ?: "")
                        }
                        is PerfilUiState.Error -> {
                            binding.mensajeFeedback.text = "❌ ${state.mensaje}"
                            binding.mensajeFeedback.setTextColor(Color.parseColor("#C62828"))
                            binding.mensajeFeedback.setBackgroundResource(com.carpoolapp.R.drawable.feedback_background_error)
                            binding.mensajeFeedback.visibility = View.VISIBLE
                        }
                        is PerfilUiState.VehiculoActualizado -> {
                            binding.mensajeFeedback.text = "✅ Vehículo actualizado correctamente"
                            binding.mensajeFeedback.setTextColor(Color.parseColor("#2E7D32"))
                            binding.mensajeFeedback.setBackgroundResource(com.carpoolapp.R.drawable.feedback_background)
                            binding.mensajeFeedback.visibility = View.VISIBLE
                            
                            // Ocultar mensaje después de 3 segundos
                            binding.root.postDelayed({
                                binding.mensajeFeedback.visibility = View.GONE
                            }, 3000)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

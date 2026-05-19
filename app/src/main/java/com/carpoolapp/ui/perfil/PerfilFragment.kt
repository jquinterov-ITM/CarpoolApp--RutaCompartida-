package com.carpoolapp.ui.perfil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            viewModel.actualizarVehiculo(binding.vehiculoInput.text.toString())
        }

        binding.btnCerrarSesion.setOnClickListener {
            viewModel.cerrarSesion()
            findNavController().navigate(com.carpoolapp.R.id.authFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PerfilUiState.Success -> {
                            val u = state.usuario
                            binding.tvNombre.text = u.nombre
                            binding.tvEmail.text = u.email
                            binding.vehiculoInput.setText(u.vehiculo ?: "")
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

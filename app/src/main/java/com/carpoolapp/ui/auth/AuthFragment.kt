package com.carpoolapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.carpoolapp.databinding.FragmentAuthBinding
import com.carpoolapp.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthFragment : BaseFragment<FragmentAuthBinding>() {

    private val viewModel: AuthViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentAuthBinding = FragmentAuthBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnEnviarLink.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            if (email.isNotBlank()) {
                viewModel.enviarLink(email)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Enviando -> {
                            binding.progress.visibility = View.VISIBLE
                            binding.btnEnviarLink.isEnabled = false
                            binding.mensaje.visibility = View.GONE
                        }
                        is AuthUiState.EmailEnviado -> {
                            binding.progress.visibility = View.GONE
                            binding.btnEnviarLink.isEnabled = true
                            binding.mensaje.text = "Redirigiendo…"
                            binding.mensaje.visibility = View.VISIBLE
                        }
                        is AuthUiState.Autenticado -> {
                            findNavController().navigate(
                                AuthFragmentDirections.actionAuthToHome()
                            )
                        }
                        is AuthUiState.Error -> {
                            binding.progress.visibility = View.GONE
                            binding.btnEnviarLink.isEnabled = true
                            binding.mensaje.text = state.mensaje
                            binding.mensaje.visibility = View.VISIBLE
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

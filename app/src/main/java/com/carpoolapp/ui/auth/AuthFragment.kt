package com.carpoolapp.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.carpoolapp.MainActivity
import com.carpoolapp.databinding.FragmentAuthBinding
import com.carpoolapp.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthFragment : BaseFragment<FragmentAuthBinding>() {

    private val viewModel: AuthViewModel by viewModels()
    private var authHandled = false
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (!idToken.isNullOrBlank()) {
                    viewModel.signInWithGoogleIdToken(idToken)
                } else {
                    binding.mensaje.visibility = View.VISIBLE
                    binding.mensaje.text = "Google no devolvió un token válido"
                }
            } catch (e: Exception) {
                binding.mensaje.visibility = View.VISIBLE
                binding.mensaje.text = "Error Google: ${e.message ?: "No se pudo iniciar con Google"}"
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentAuthBinding = FragmentAuthBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.emailInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.mensaje.visibility = View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.passwordInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.mensaje.visibility = View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnSignIn.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()
            if (email.isNotBlank() && password.isNotBlank()) {
                viewModel.signInWithEmail(email, password)
            } else {
                binding.mensaje.visibility = View.VISIBLE
                binding.mensaje.text = "Ingresa email y contraseña"
            }
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()
            android.util.Log.d("AuthFragment", "btnRegister clicked - email: $email, password length: ${password.length}")
            if (email.isNotBlank() && password.isNotBlank()) {
                viewModel.registerWithEmail(email, password)
            } else {
                binding.mensaje.visibility = View.VISIBLE
                binding.mensaje.text = "Ingresa email y contraseña"
            }
        }

        binding.btnGoogle.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(com.carpoolapp.R.string.google_web_client_id))
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(requireContext(), gso)
            googleSignInLauncher.launch(client.signInIntent)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Enviando -> {
                            binding.progress.visibility = View.VISIBLE
                            binding.btnSignIn.isEnabled = false
                            binding.btnRegister.isEnabled = false
                            binding.btnGoogle.isEnabled = false
                            binding.mensaje.visibility = View.GONE
                        }
                        is AuthUiState.RegistroExitoso -> {
                            binding.progress.visibility = View.GONE
                            binding.btnSignIn.isEnabled = true
                            binding.btnRegister.isEnabled = true
                            binding.btnGoogle.isEnabled = true
                            binding.mensaje.apply {
                                visibility = View.VISIBLE
                                text = "✅ Cuenta creada: ${state.email}. Ahora inicia sesión."
                                setTextColor(requireContext().getColor(com.carpoolapp.R.color.success_color))
                            }
                        }
                        is AuthUiState.Autenticado -> {
                            if (!authHandled) {
                                authHandled = true
                                val intent = Intent(requireContext(), MainActivity::class.java)
                                startActivity(intent)
                                requireActivity().finish()
                            }
                        }
                        is AuthUiState.Error -> {
                            binding.progress.visibility = View.GONE
                            binding.btnSignIn.isEnabled = true
                            binding.btnRegister.isEnabled = true
                            binding.btnGoogle.isEnabled = true
                            binding.mensaje.apply {
                                visibility = View.VISIBLE
                                text = state.mensaje
                                setTextColor(requireContext().getColor(com.carpoolapp.R.color.error_color))
                            }
                            android.util.Log.d("AuthFragment", "Error mostrado: ${state.mensaje}")
                        }
                        AuthUiState.Idle -> {
                            // No cambiar el mensaje - mantener errores visibles hasta que el usuario escriba
                        }
                        is AuthUiState.EmailEnviado -> {
                            // Estado intermedio, no hacer nada
                        }
                    }
                }
            }
        }
    }
}

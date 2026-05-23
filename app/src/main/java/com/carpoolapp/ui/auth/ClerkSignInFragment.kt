package com.carpoolapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import android.net.Uri
import com.carpoolapp.R
import com.carpoolapp.databinding.FragmentClerkSignInBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ClerkSignInFragment : Fragment() {
    private var _binding: FragmentClerkSignInBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ClerkAuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClerkSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.signInButton.setOnClickListener {
            signInWithGoogle()
        }

        binding.signUpButton.setOnClickListener {
            signUpWithGoogle()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is ClerkAuthState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.signInButton.isEnabled = false
                        binding.signUpButton.isEnabled = false
                    }
                    is ClerkAuthState.SignedIn -> {
                        binding.progressBar.visibility = View.GONE
                        findNavController().navigate(R.id.action_clerkSignInFragment_to_homeFragment)
                    }
                    is ClerkAuthState.SignedOut -> {
                        binding.progressBar.visibility = View.GONE
                        binding.signInButton.isEnabled = true
                        binding.signUpButton.isEnabled = true
                    }
                    is ClerkAuthState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.errorMessage.visibility = View.VISIBLE
                        binding.errorMessage.text = state.message
                        binding.signInButton.isEnabled = true
                        binding.signUpButton.isEnabled = true
                    }
                }
            }
        }
    }

    private fun signInWithGoogle() {
        try {
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            val url = viewModel.getSignInUrl()
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
        } catch (e: Exception) {
            binding.errorMessage.visibility = View.VISIBLE
            binding.errorMessage.text = "Error: ${e.message}"
        }
    }

    private fun signUpWithGoogle() {
        try {
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            val url = viewModel.getSignUpUrl()
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
        } catch (e: Exception) {
            binding.errorMessage.visibility = View.VISIBLE
            binding.errorMessage.text = "Error: ${e.message}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


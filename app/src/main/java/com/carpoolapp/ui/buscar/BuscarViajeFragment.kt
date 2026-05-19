package com.carpoolapp.ui.buscar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.carpoolapp.databinding.FragmentBuscarBinding
import com.carpoolapp.ui.common.BaseFragment
import com.carpoolapp.ui.home.ViajeAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BuscarViajeFragment : BaseFragment<FragmentBuscarBinding>() {

    private val viewModel: BuscarViajeViewModel by viewModels()
    private lateinit var adapter: ViajeAdapter

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentBuscarBinding = FragmentBuscarBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ViajeAdapter { }
        binding.rvResultados.adapter = adapter

        binding.buscarInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.buscar(binding.buscarInput.text.toString().trim())
                true
            } else false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is BuscarUiState.Resultado -> adapter.submitList(state.viajes)
                        else -> {}
                    }
                }
            }
        }
    }
}

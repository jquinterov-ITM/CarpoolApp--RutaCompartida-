package com.carpoolapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.carpoolapp.databinding.FragmentHomeBinding
import com.carpoolapp.databinding.ItemViajeBinding
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: ViajeAdapter

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ViajeAdapter { viaje ->
            findNavController().navigate(
                HomeFragmentDirections.actionHomeToDetalle(
                    tripId = viaje.id,
                    esConductor = false
                )
            )
        }
        binding.rvViajes.adapter = adapter

        binding.fabPublicar.setOnClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeToPublicar()
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is HomeUiState.Loading -> {
                            binding.progress.visibility = View.VISIBLE
                        }
                        is HomeUiState.Success -> {
                            binding.progress.visibility = View.GONE
                            adapter.submitList(state.viajes)
                            val empty = state.viajes.isEmpty()
                            binding.tvVacio.visibility = if (empty) View.VISIBLE else View.GONE
                            binding.tvVacioIcon.visibility = if (empty) View.VISIBLE else View.GONE
                        }
                        is HomeUiState.Error -> {
                            binding.progress.visibility = View.GONE
                            binding.tvVacio.text = state.mensaje
                            binding.tvVacio.visibility = View.VISIBLE
                            binding.tvVacioIcon.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }
}

class ViajeAdapter(
    private val onClick: (Viaje) -> Unit
) : ListAdapter<Viaje, ViajeAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemViajeBinding) : RecyclerView.ViewHolder(binding.root)

    companion object DiffCallback : DiffUtil.ItemCallback<Viaje>() {
        override fun areItemsTheSame(a: Viaje, b: Viaje) = a.id == b.id
        override fun areContentsTheSame(a: Viaje, b: Viaje) = a == b
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
        val binding = ItemViajeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val viaje = getItem(position)
        holder.binding.apply {
            tvConductor.text = viaje.conductorNombre
            tvOrigen.text = viaje.origen
            tvDestino.text = viaje.destino
            tvAsientos.text = "${viaje.asientosDisponibles} asientos disponibles"
            tvEstado.text = viaje.estado.name

            val estadoColor = when (viaje.estado) {
                com.carpoolapp.domain.model.ViajeEstado.PROGRAMADO -> com.carpoolapp.R.color.estado_programado
                com.carpoolapp.domain.model.ViajeEstado.ACTIVO -> com.carpoolapp.R.color.estado_activo
                com.carpoolapp.domain.model.ViajeEstado.COMPLETADO -> com.carpoolapp.R.color.estado_completado
                com.carpoolapp.domain.model.ViajeEstado.CANCELADO -> com.carpoolapp.R.color.estado_cancelado
            }
            tvEstado.setTextColor(ContextCompat.getColor(root.context, estadoColor))
            tvEstado.background.setTint(ContextCompat.getColor(root.context, estadoColor))

            if (viaje.fechaHora > 0) {
                val sdf = java.text.SimpleDateFormat("EEE, MMM d 'a las' h:mm a", java.util.Locale.getDefault())
                tvFecha.text = sdf.format(java.util.Date(viaje.fechaHora))
            } else {
                tvFecha.visibility = View.GONE
            }

            root.setOnClickListener { onClick(viaje) }
        }
    }
}

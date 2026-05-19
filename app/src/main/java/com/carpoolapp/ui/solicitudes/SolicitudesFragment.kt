package com.carpoolapp.ui.solicitudes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.carpoolapp.databinding.FragmentSolicitudesBinding
import com.carpoolapp.databinding.ItemSolicitudBinding
import com.carpoolapp.domain.model.Solicitud
import com.carpoolapp.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SolicitudesFragment : BaseFragment<FragmentSolicitudesBinding>() {

    private val viewModel: SolicitudesViewModel by viewModels()
    private lateinit var adapter: SolicitudAdapter

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentSolicitudesBinding =
        FragmentSolicitudesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SolicitudAdapter(
            onAceptar = { solicitud -> viewModel.aceptar(solicitud.id) },
            onRechazar = { solicitud -> viewModel.rechazar(solicitud.id) }
        )
        binding.rvSolicitudes.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is SolicitudesUiState.Success -> {
                            binding.tvTitulo.text = "${state.viaje.origen} → ${state.viaje.destino}"
                            adapter.submitList(state.solicitudes.filter {
                                it.estado == com.carpoolapp.domain.model.SolicitudEstado.PENDIENTE
                            })
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

class SolicitudAdapter(
    private val onAceptar: (Solicitud) -> Unit,
    private val onRechazar: (Solicitud) -> Unit
) : ListAdapter<Solicitud, SolicitudAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemSolicitudBinding) : RecyclerView.ViewHolder(binding.root)

    companion object DiffCallback : DiffUtil.ItemCallback<Solicitud>() {
        override fun areItemsTheSame(a: Solicitud, b: Solicitud) = a.id == b.id
        override fun areContentsTheSame(a: Solicitud, b: Solicitud) = a == b
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
        val binding = ItemSolicitudBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val solicitud = getItem(position)
        holder.binding.apply {
            tvPasajero.text = solicitud.pasajeroNombre
            tvEstadoSolicitud.text = solicitud.estado.name
            btnAceptar.setOnClickListener { onAceptar(solicitud) }
            btnRechazar.setOnClickListener { onRechazar(solicitud) }
        }
    }
}

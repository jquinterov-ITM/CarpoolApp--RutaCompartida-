package com.carpoolapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.carpoolapp.R
import com.carpoolapp.databinding.FragmentHomeBinding
import com.carpoolapp.databinding.ItemViajeBinding
import com.carpoolapp.domain.model.TipoViaje
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.domain.model.ViajeEstado
import com.carpoolapp.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: ViajeAdapter

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ViajeAdapter { viaje, sharedView ->
            val transitionName = "trip_${'$'}{viaje.id}"
            sharedView.transitionName = transitionName
            val extras = FragmentNavigatorExtras(sharedView to transitionName)
            findNavController().navigate(HomeFragmentDirections.actionHomeToDetalle(tripId = viaje.id, esConductor = false), extras)
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

    override fun onResume() {
        super.onResume()
        viewModel.cargarFeed()
    }
}

class ViajeAdapter(
    private val onClick: (Viaje, View) -> Unit
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

            val vehiculoInfo = viaje.vehiculoConductor?.placa?.uppercase() ?: ""
            tvVehiculo.text = vehiculoInfo
            tvVehiculo.visibility = if (vehiculoInfo.isNotEmpty()) View.VISIBLE else View.GONE

            val (textColor, bgColorRes) = when (viaje.estado) {
                ViajeEstado.PROGRAMADO -> {
                    root.context.getColor(R.color.on_primary) to R.drawable.bg_estado_programado
                }
                ViajeEstado.ACTIVO, ViajeEstado.EN_PROGRESO -> {
                    root.context.getColor(R.color.on_primary) to R.drawable.bg_estado_activo
                }
                ViajeEstado.COMPLETADO -> {
                    root.context.getColor(R.color.on_primary) to R.drawable.bg_estado_completado
                }
                ViajeEstado.CANCELADO -> {
                    root.context.getColor(R.color.on_primary) to R.drawable.bg_estado_cancelado
                }
            }
            tvEstado.setTextColor(textColor)
            flEstadoContainer.setBackgroundResource(bgColorRes)

            if (viaje.tipo == TipoViaje.INMEDIATO) {
                tvFecha.text = root.context.getString(R.string.label_inmediato)
            } else if (viaje.fechaHora > 0) {
                val sdf = SimpleDateFormat("EEE, MMM d 'a las' h:mm a", Locale("es"))
                sdf.timeZone = TimeZone.getDefault()
                tvFecha.text = sdf.format(Date(viaje.fechaHora))
            } else {
                tvFecha.text = root.context.getString(R.string.msg_fecha_desconocida)
                tvFecha.visibility = View.VISIBLE
            }

            root.transitionName = "trip_${'$'}{viaje.id}"
            root.setOnClickListener { onClick(viaje, root) }
        }
    }
}

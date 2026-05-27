package com.carpoolapp.ui.mis_viajes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.carpoolapp.R
import com.carpoolapp.databinding.FragmentMisViajesBinding
import com.carpoolapp.databinding.ItemViajeBinding
import com.carpoolapp.databinding.ItemSectionHeaderBinding
import com.carpoolapp.domain.model.Viaje
import com.carpoolapp.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MisViajesFragment : BaseFragment<FragmentMisViajesBinding>() {

    private val viewModel: MisViajesViewModel by viewModels()
    private lateinit var adapter: MisViajesAdapter

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentMisViajesBinding =
        FragmentMisViajesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MisViajesAdapter { viaje ->
            findNavController().navigate(
                com.carpoolapp.R.id.action_misViajes_to_detalle,
                android.os.Bundle().apply {
                    putString("tripId", viaje.id)
                    putBoolean("esConductor", true)
                }
            )
        }
        binding.rvMisViajes.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MisViajesUiState.Loading -> {
                            binding.progressMisViajes.visibility = View.VISIBLE
                        }
                        is MisViajesUiState.Success -> {
                            binding.progressMisViajes.visibility = View.GONE
                            val items = buildItems(state)
                            adapter.submitList(items)
                            binding.tvMisViajesVacio.visibility =
                                if (items.isEmpty()) View.VISIBLE else View.GONE
                        }
                        is MisViajesUiState.Error -> {
                            binding.progressMisViajes.visibility = View.GONE
                            binding.tvMisViajesVacio.text = state.mensaje
                            binding.tvMisViajesVacio.visibility = View.VISIBLE
                            // Allow tapping the empty view to seed a request for quick debug
                            binding.tvMisViajesVacio.setOnClickListener {
                                val uid = FirebaseAuth.getInstance().currentUser?.uid
                                if (uid != null) {
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        try {
                                            com.carpoolapp.data.seed.DataSeeder(
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            ).seedRequestForUser(uid)
                                        } catch (_: Exception) {}
                                        viewModel.cargar()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Force reload when fragment becomes visible again (e.g., after publishing a trip)
        viewModel.cargar()
        viewModel.crearCanalNotificacion()
    }

    private fun buildItems(state: MisViajesUiState.Success): List<MisViajeItem> {
        val items = mutableListOf<MisViajeItem>()
        if (state.comoConductor.isNotEmpty()) {
            items.add(MisViajeItem.Header("Como conductor"))
            items.addAll(state.comoConductor.map { MisViajeItem.ViajeItem(it) })
        }
        if (state.comoPasajero.isNotEmpty()) {
            items.add(MisViajeItem.Header("Como pasajero"))
            items.addAll(state.comoPasajero.map { MisViajeItem.ViajeItem(it) })
        }
        return items
    }
}

sealed class MisViajeItem {
    data class Header(val title: String) : MisViajeItem()
    data class ViajeItem(val viaje: Viaje) : MisViajeItem()
}

class MisViajesAdapter(
    private val onClick: (Viaje) -> Unit
) : ListAdapter<MisViajeItem, RecyclerView.ViewHolder>(MisViajeDiffCallback) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1

        private val MisViajeDiffCallback = object : DiffUtil.ItemCallback<MisViajeItem>() {
            override fun areItemsTheSame(a: MisViajeItem, b: MisViajeItem): Boolean = when {
                a is MisViajeItem.Header && b is MisViajeItem.Header -> a.title == b.title
                a is MisViajeItem.ViajeItem && b is MisViajeItem.ViajeItem -> a.viaje.id == b.viaje.id
                else -> false
            }
            override fun areContentsTheSame(a: MisViajeItem, b: MisViajeItem): Boolean = a == b
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is MisViajeItem.Header -> TYPE_HEADER
        is MisViajeItem.ViajeItem -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemSectionHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemViajeBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ViajeViewHolder(binding)
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MisViajeItem.Header -> {
                (holder as HeaderViewHolder).bind(item.title)
            }
            is MisViajeItem.ViajeItem -> {
                (holder as ViajeViewHolder).bind(item.viaje)
            }
        }
    }

    class HeaderViewHolder(private val binding: ItemSectionHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.tvSectionTitle.text = title
        }
    }

    inner class ViajeViewHolder(private val binding: ItemViajeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(viaje: Viaje) {
            binding.apply {
                tvConductor.text = viaje.conductorNombre
                tvOrigen.text = viaje.origen
                tvDestino.text = viaje.destino
                tvAsientos.text = "${viaje.asientosDisponibles} asientos disponibles"
                tvEstado.text = viaje.estado.name
                
                val (textColor, bgColorRes) = when (viaje.estado) {
                    com.carpoolapp.domain.model.ViajeEstado.PROGRAMADO -> {
                        root.context.getColor(R.color.on_primary) to R.drawable.bg_estado_programado
                    }
                    com.carpoolapp.domain.model.ViajeEstado.ACTIVO, com.carpoolapp.domain.model.ViajeEstado.EN_PROGRESO -> {
                        root.context.getColor(R.color.on_secondary) to R.drawable.bg_estado_activo
                    }
                    com.carpoolapp.domain.model.ViajeEstado.COMPLETADO -> {
                        root.context.getColor(R.color.on_primary) to R.drawable.bg_estado_completado
                    }
                    com.carpoolapp.domain.model.ViajeEstado.CANCELADO -> {
                        root.context.getColor(R.color.on_primary) to R.drawable.bg_estado_cancelado
                    }
                }
                
                tvEstado.setTextColor(textColor)
                flEstadoContainer.setBackgroundResource(bgColorRes)
                
                if (viaje.tipo == com.carpoolapp.domain.model.TipoViaje.INMEDIATO) {
                    tvFecha.text = root.context.getString(com.carpoolapp.R.string.label_inmediato)
                } else if (viaje.fechaHora > 0) {
                    val sdf = java.text.SimpleDateFormat("EEE, MMM d 'a las' h:mm a", java.util.Locale("es"))
                    sdf.timeZone = java.util.TimeZone.getDefault()
                    tvFecha.text = sdf.format(java.util.Date(viaje.fechaHora))
                } else {
                    tvFecha.text = root.context.getString(com.carpoolapp.R.string.msg_fecha_desconocida)
                    tvFecha.visibility = View.VISIBLE
                }
                root.setOnClickListener { onClick(viaje) }
            }
        }
    }
}

package com.carpoolapp.ui.detalle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.transition.MaterialContainerTransform
import android.graphics.Color
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.carpoolapp.databinding.FragmentViajeDetalleBinding
import com.carpoolapp.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ViajeDetalleFragment : BaseFragment<FragmentViajeDetalleBinding>() {

    private val viewModel: ViajeDetalleViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentViajeDetalleBinding =
        FragmentViajeDetalleBinding.inflate(inflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure shared element container transform
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = com.carpoolapp.R.id.nav_host_fragment
            // Slightly longer enter for a pleasant reveal
            duration = 350L
            scrimColor = Color.parseColor("#22000000")
            setAllContainerColors(requireContext().getColor(com.carpoolapp.R.color.surface))
        }
        sharedElementReturnTransition = MaterialContainerTransform().apply {
            drawingViewId = com.carpoolapp.R.id.nav_host_fragment
            // Faster return
            duration = 220L
            scrimColor = Color.parseColor("#22000000")
            setAllContainerColors(requireContext().getColor(com.carpoolapp.R.color.surface))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ensure root has same transitionName as provided by the adapter
        val tripId = arguments?.getString("tripId")
        if (!tripId.isNullOrEmpty()) {
            ViewCompat.setTransitionName(binding.root, "trip_${'$'}{tripId}")
        }

        binding.btnSolicitar.setOnClickListener {
            viewModel.enviarSolicitud()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DetalleUiState.Success -> {
                            val viaje = state.viaje
                            val initials = viaje.conductorNombre.split(" ")
                                .joinToString("") { it.take(1).uppercase() }
                                .take(2)
                            binding.ivAvatar.text = initials.ifEmpty { "?" }
                            binding.tvConductor.text = viaje.conductorNombre
                            binding.tvOrigen.text = viaje.origen
                            binding.tvDestino.text = viaje.destino
                            if (viaje.tipo == com.carpoolapp.domain.model.TipoViaje.INMEDIATO) {
                                binding.tvFecha.text = requireContext().getString(com.carpoolapp.R.string.label_inmediato)
                            } else {
                                val sdf = SimpleDateFormat("EEE, d MMM 'a las' h:mm a", Locale("es"))
                                sdf.timeZone = java.util.TimeZone.getDefault()
                                binding.tvFecha.text = sdf.format(Date(viaje.fechaHora))
                            }
                            binding.tvAsientos.text = "${viaje.asientosDisponibles} asientos disponibles"
                            binding.tvEstado.text = viaje.estado.name
                            val estadoColor = when (viaje.estado) {
                                com.carpoolapp.domain.model.ViajeEstado.PROGRAMADO -> com.carpoolapp.R.color.estado_programado
                                com.carpoolapp.domain.model.ViajeEstado.ACTIVO -> com.carpoolapp.R.color.estado_activo
                                com.carpoolapp.domain.model.ViajeEstado.COMPLETADO -> com.carpoolapp.R.color.estado_completado
                                com.carpoolapp.domain.model.ViajeEstado.CANCELADO -> com.carpoolapp.R.color.estado_cancelado
                            }
                            binding.tvEstado.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), estadoColor))
                            binding.tvEstado.background.setTint(androidx.core.content.ContextCompat.getColor(requireContext(), estadoColor))
                            binding.btnSolicitar.visibility = View.VISIBLE
                        }
                        is DetalleUiState.EnviandoSolicitud -> {
                            binding.btnSolicitar.isEnabled = false
                            binding.btnSolicitar.text = "Enviando…"
                        }
                        is DetalleUiState.SolicitudExitosa -> {
                            binding.btnSolicitar.text = "Solicitud enviada"
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

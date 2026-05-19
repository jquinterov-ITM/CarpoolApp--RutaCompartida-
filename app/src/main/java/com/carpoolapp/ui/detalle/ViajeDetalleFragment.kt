package com.carpoolapp.ui.detalle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSolicitar.setOnClickListener {
            viewModel.enviarSolicitud()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DetalleUiState.Success -> {
                            val viaje = state.viaje
                            binding.tvConductor.text = viaje.conductorNombre
                            binding.tvRuta.text = "${viaje.origen} → ${viaje.destino}"
                            binding.tvFecha.text = SimpleDateFormat(
                                "dd/MM/yyyy HH:mm", Locale.getDefault()
                            ).format(Date(viaje.fechaHora))
                            binding.tvAsientos.text = "${viaje.asientosDisponibles} asientos disponibles"
                            binding.tvEstado.text = viaje.estado.name
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

package com.carpoolapp.ui.detalle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialContainerTransform
import android.graphics.Color
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.carpoolapp.databinding.FragmentViajeDetalleBinding
import com.carpoolapp.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth

@AndroidEntryPoint
class ViajeDetalleFragment : BaseFragment<FragmentViajeDetalleBinding>() {

    private val viewModel: ViajeDetalleViewModel by viewModels()
    private var solicitudesAdapter: SolicitudesDetalleAdapter? = null
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentViajeDetalleBinding =
        FragmentViajeDetalleBinding.inflate(inflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = com.carpoolapp.R.id.nav_host_fragment
            duration = 350L
            scrimColor = Color.parseColor("#22000000")
            setAllContainerColors(requireContext().getColor(com.carpoolapp.R.color.surface))
        }
        sharedElementReturnTransition = MaterialContainerTransform().apply {
            drawingViewId = com.carpoolapp.R.id.nav_host_fragment
            duration = 220L
            scrimColor = Color.parseColor("#22000000")
            setAllContainerColors(requireContext().getColor(com.carpoolapp.R.color.surface))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tripId = arguments?.getString("tripId")
        if (!tripId.isNullOrEmpty()) {
            ViewCompat.setTransitionName(binding.root, "trip_$tripId")
        }

        binding.btnSolicitar.setOnClickListener {
            viewModel.enviarSolicitud()
        }

        binding.btnFinalizar.setOnClickListener {
            viewModel.finalizarViaje()
        }

        binding.btnCancelar.setOnClickListener {
            viewModel.cancelarViaje()
        }

        binding.btnCancelarSolicitud.setOnClickListener {
            viewModel.cancelarSolicitud()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DetalleUiState.Success -> {
                            val viaje = state.viaje
                            actualizarUI(viaje, state.esConductor, state.yaSolicito, state.solicitudes)
                        }
                        is DetalleUiState.EnviandoSolicitud -> {
                            binding.btnSolicitar.isEnabled = false
                            binding.btnSolicitar.text = "Enviando…"
                        }
                        is DetalleUiState.SolicitudExitosa -> {
                            Toast.makeText(requireContext(), "Solicitud enviada correctamente", Toast.LENGTH_SHORT).show()
                            // Regresar al menú principal después de 1 segundo
                            binding.root.postDelayed({
                                try {
                                    this@ViajeDetalleFragment.findNavController().popBackStack()
                                } catch (e: Exception) {
                                    android.util.Log.e("ViajeDetalleFragment", "Error al navegar: ${e.message}")
                                }
                            }, 1000)
                        }
                        is DetalleUiState.Finalizando -> {
                            binding.btnFinalizar.isEnabled = false
                            binding.btnFinalizar.text = "Finalizando…"
                        }
                        is DetalleUiState.Cancelando -> {
                            binding.btnCancelar.isEnabled = false
                            binding.btnCancelar.text = "Cancelando…"
                        }
                        is DetalleUiState.Error -> {
                            Toast.makeText(requireContext(), state.mensaje, Toast.LENGTH_SHORT).show()
                            android.util.Log.e("ViajeDetalleFragment", "Error: ${state.mensaje}")
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun actualizarUI(
        viaje: com.carpoolapp.domain.model.Viaje,
        esConductor: Boolean,
        yaSolicito: Boolean,
        solicitudes: List<com.carpoolapp.domain.model.Solicitud>
    ) {
        binding.btnFinalizar.isEnabled = true
        binding.btnFinalizar.text = "Finalizar viaje"
        binding.btnCancelar.isEnabled = true
        binding.btnCancelar.text = "Cancelar viaje"
        binding.btnSolicitar.isEnabled = true
        binding.btnSolicitar.text = getString(com.carpoolapp.R.string.btn_solicitar)
        
        val initials = viaje.conductorNombre.split(" ")
            .joinToString("") { it.take(1).uppercase() }
            .take(2)
        binding.ivAvatar.contentDescription = initials.ifEmpty { "?" }
        binding.tvConductor.text = viaje.conductorNombre
        binding.tvOrigen.text = viaje.origen
        binding.tvDestino.text = viaje.destino
        
        if (viaje.tipo == com.carpoolapp.domain.model.TipoViaje.INMEDIATO) {
            binding.tvFecha.text = requireContext().getString(com.carpoolapp.R.string.label_inmediato)
        } else if (viaje.fechaHora > 0) {
            val sdf = SimpleDateFormat("EEE, d MMM 'a las' h:mm a", Locale("es"))
            sdf.timeZone = java.util.TimeZone.getDefault()
            binding.tvFecha.text = sdf.format(Date(viaje.fechaHora))
        }
        
        binding.tvAsientos.text = "${viaje.asientosDisponibles}/${viaje.asientosTotales} asientos disponibles"
        
        val vehiculoInfo = viaje.vehiculoConductor?.let { v ->
            listOfNotNull(
                v.placa?.uppercase(),
                v.marca?.takeIf { it.isNotEmpty() }?.let { "$it ${v.modelo}" },
                v.color?.takeIf { it.isNotEmpty() }
            ).joinToString(" - ")
        } ?: ""
        
        if (vehiculoInfo.isNotEmpty()) {
            binding.tvVehiculo.text = vehiculoInfo
            binding.labelVehiculo.visibility = View.VISIBLE
            binding.tvVehiculo.visibility = View.VISIBLE
        } else {
            binding.labelVehiculo.visibility = View.GONE
            binding.tvVehiculo.visibility = View.GONE
        }
        
        val estadoText = when (viaje.estado) {
            com.carpoolapp.domain.model.ViajeEstado.ACTIVO -> "ACTIVO"
            com.carpoolapp.domain.model.ViajeEstado.EN_PROGRESO -> "EN PROGRESO"
            com.carpoolapp.domain.model.ViajeEstado.PROGRAMADO -> "PROGRAMADO"
            com.carpoolapp.domain.model.ViajeEstado.COMPLETADO -> "COMPLETADO"
            com.carpoolapp.domain.model.ViajeEstado.CANCELADO -> "CANCELADO"
        }
        binding.tvEstado.text = estadoText
        
        val estadoColor = when (viaje.estado) {
            com.carpoolapp.domain.model.ViajeEstado.PROGRAMADO -> com.carpoolapp.R.color.estado_programado
            com.carpoolapp.domain.model.ViajeEstado.ACTIVO, com.carpoolapp.domain.model.ViajeEstado.EN_PROGRESO -> com.carpoolapp.R.color.estado_activo
            com.carpoolapp.domain.model.ViajeEstado.COMPLETADO -> com.carpoolapp.R.color.estado_completado
            com.carpoolapp.domain.model.ViajeEstado.CANCELADO -> com.carpoolapp.R.color.estado_cancelado
        }
        binding.tvEstado.background.setTint(androidx.core.content.ContextCompat.getColor(requireContext(), estadoColor))
        
        if (viaje.descripcion.isNotEmpty()) {
            binding.tvDescripcion.text = viaje.descripcion
            binding.cardDescripcion.visibility = View.VISIBLE
        } else {
            binding.cardDescripcion.visibility = View.GONE
        }

        if (esConductor) {
            binding.btnSolicitar.visibility = View.GONE
            
            if (viaje.estado != com.carpoolapp.domain.model.ViajeEstado.COMPLETADO && 
                viaje.estado != com.carpoolapp.domain.model.ViajeEstado.CANCELADO) {
                binding.btnFinalizar.visibility = View.VISIBLE
                binding.btnCancelar.visibility = View.VISIBLE
            } else {
                binding.btnFinalizar.visibility = View.GONE
                binding.btnCancelar.visibility = View.GONE
            }
            
            binding.solicitudesCard.visibility = View.VISIBLE
            if (solicitudesAdapter == null) {
                solicitudesAdapter = SolicitudesDetalleAdapter(
                    onAceptar = { solicitud -> viewModel.aceptarSolicitud(solicitud.id) },
                    onRechazar = { solicitud -> viewModel.rechazarSolicitud(solicitud.id) }
                )
                binding.rvSolicitudes.layoutManager = LinearLayoutManager(requireContext())
                binding.rvSolicitudes.adapter = solicitudesAdapter
            }
            // Solo mostrar solicitudes PENDIENTES (las aceptadas van arriba)
            solicitudesAdapter?.submitList(solicitudes.filter { 
                it.estado == com.carpoolapp.domain.model.SolicitudEstado.PENDIENTE 
            })
        } else {
            // Es pasajero - verificar si ya solicitó
            val solicitudPendiente = solicitudes.any { 
                it.pasajeroId == auth.currentUser?.uid && 
                it.estado == com.carpoolapp.domain.model.SolicitudEstado.PENDIENTE 
            }
            
            if (yaSolicito || solicitudPendiente || viaje.estado == com.carpoolapp.domain.model.ViajeEstado.COMPLETADO) {
                binding.btnSolicitar.visibility = View.GONE
            } else {
                binding.btnSolicitar.visibility = View.VISIBLE
            }
            
            binding.btnFinalizar.visibility = View.GONE
            binding.btnCancelar.visibility = View.GONE
            binding.solicitudesCard.visibility = View.GONE
            
            if (yaSolicito || solicitudPendiente) {
                binding.tvEstadoSolicitud.visibility = View.VISIBLE
                val estadoSolicitud = solicitudes.find { it.pasajeroId == auth.currentUser?.uid }
                binding.tvEstadoSolicitud.text = when (estadoSolicitud?.estado) {
                    com.carpoolapp.domain.model.SolicitudEstado.ACEPTADA -> "✅ Solicitud aceptada"
                    com.carpoolapp.domain.model.SolicitudEstado.RECHAZADA -> "❌ Solicitud rechazada"
                    com.carpoolapp.domain.model.SolicitudEstado.CANCELADA -> "Solicitud cancelada"
                    else -> "Has solicitado este viaje"
                }
                binding.btnCancelarSolicitud.visibility = if (estadoSolicitud?.estado == com.carpoolapp.domain.model.SolicitudEstado.PENDIENTE) View.VISIBLE else View.GONE
            } else {
                binding.tvEstadoSolicitud.visibility = View.GONE
                binding.btnCancelarSolicitud.visibility = View.GONE
            }
        }
    }
}

class SolicitudesDetalleAdapter(
    private val onAceptar: (com.carpoolapp.domain.model.Solicitud) -> Unit,
    private val onRechazar: (com.carpoolapp.domain.model.Solicitud) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<SolicitudesDetalleAdapter.ViewHolder>() {

    private val solicitudes = mutableListOf<com.carpoolapp.domain.model.Solicitud>()

    class ViewHolder(val binding: com.carpoolapp.databinding.ItemSolicitudBinding) : 
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
        val binding = com.carpoolapp.databinding.ItemSolicitudBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val solicitud = solicitudes[position]
        android.util.Log.d("SolicitudesAdapter", "Binding solicitud: ${solicitud.id}, pasajero: ${solicitud.pasajeroNombre}, estado: ${solicitud.estado}")
        holder.binding.apply {
            tvPasajero.text = solicitud.pasajeroNombre
            tvEstadoSolicitud.text = solicitud.estado.name
            tvCalificacionPasajero.text = "⭐ ${solicitud.pasajeroCalificacion}"
            btnAceptar.setOnClickListener { 
                android.util.Log.d("SolicitudesAdapter", "Click en aceptar para solicitud: ${solicitud.id}")
                onAceptar(solicitud) 
            }
            btnRechazar.setOnClickListener { 
                android.util.Log.d("SolicitudesAdapter", "Click en rechazar para solicitud: ${solicitud.id}")
                onRechazar(solicitud) 
            }
        }
    }

    override fun getItemCount() = solicitudes.size

    fun submitList(nuevaLista: List<com.carpoolapp.domain.model.Solicitud>) {
        solicitudes.clear()
        solicitudes.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}

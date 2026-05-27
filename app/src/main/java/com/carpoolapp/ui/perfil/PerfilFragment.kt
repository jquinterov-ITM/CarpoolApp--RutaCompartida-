package com.carpoolapp.ui.perfil

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.carpoolapp.databinding.FragmentPerfilBinding
import com.carpoolapp.domain.model.MarcaVehiculo
import com.carpoolapp.domain.model.COLORES_VEHICULO
import com.carpoolapp.domain.model.Vehiculo
import com.carpoolapp.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PerfilFragment : BaseFragment<FragmentPerfilBinding>() {

    private val viewModel: PerfilViewModel by viewModels()
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val inputStream = requireContext().contentResolver.openInputStream(it)
            val bytes = inputStream?.readBytes()
            if (bytes != null) {
                val fotoUrl = "data:image/jpeg;base64,${android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)}"
                viewModel.actualizarFotoUrl(fotoUrl)
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentPerfilBinding = FragmentPerfilBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnEditarFoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnGuardarVehiculo.setOnClickListener {
            val marca = binding.marcaInput.text.toString().trim()
            val modelo = binding.modeloInput.text.toString().trim()
            val anoStr = binding.anoInput.text.toString().trim()
            val color = binding.colorInput.text.toString().trim()
            val placa = binding.placaInput.text.toString().trim()

            if (marca.isEmpty()) {
                Toast.makeText(requireContext(), "Selecciona una marca", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (modelo.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa el modelo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (anoStr.isEmpty()) {
                Toast.makeText(requireContext(), "Selecciona el año", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (color.isEmpty()) {
                Toast.makeText(requireContext(), "Selecciona el color", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (placa.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa la placa", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ano = anoStr.toIntOrNull() ?: 0
            val vehiculo = Vehiculo(marca, modelo, ano, color, placa, null)
            viewModel.actualizarVehiculo(vehiculo)
        }

        val marcas = MarcaVehiculo.entries.map { it.displayName }.toList()
        val adapterMarca = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, marcas)
        binding.marcaInput.setAdapter(adapterMarca)

        val anios = (2010..2026).map { it.toString() }.reversed()
        val adapterAnio = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, anios)
        binding.anoInput.setAdapter(adapterAnio)

        val adapterColor = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, COLORES_VEHICULO)
        binding.colorInput.setAdapter(adapterColor)

        binding.tipoToggle.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val esConductor = checkedId == com.carpoolapp.R.id.btn_conductor
                viewModel.toggleEsConductor(esConductor)
            }
        }

        binding.btnCerrarSesion.setOnClickListener {
            viewModel.cerrarSesion()
            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
            requireActivity().recreate()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PerfilUiState.Success -> {
                            val u = state.usuario
                            val initials = u.nombre.split(" ")
                                .joinToString("") { it.take(1).uppercase() }
                                .take(2)
                            
                            binding.avatar.contentDescription = initials.ifEmpty { "?" }
                            binding.tvNombre.text = u.nombre
                            binding.tvEmail.text = u.email
                            
                            if (!u.fotoUrl.isNullOrBlank() && !u.fotoUrl.startsWith("data:")) {
                                binding.avatar.load(u.fotoUrl) {
                                    crossfade(true)
                                    placeholder(com.carpoolapp.R.drawable.ic_person_grey_24dp)
                                    error(com.carpoolapp.R.drawable.ic_person_grey_24dp)
                                }
                            }
                            
                            binding.ratingBar.rating = u.calificacion.toFloat()
                            binding.tvCalificacion.text = "${u.calificacion} / 5.0 (${u.viajesCompletados} viajes)"
                            binding.tvViajesCompletados.text = "Viajes completados: ${u.viajesCompletados}"
                            
                            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
                            binding.tvVersion.text = "Versión ${packageInfo.versionName}"
                            
                            binding.tipoToggle.check(
                                if (u.esConductor) com.carpoolapp.R.id.btn_conductor
                                else com.carpoolapp.R.id.btn_pasajero
                            )
                            
                            val buttonConductor = binding.btnConductor
                            val buttonPasajero = binding.btnPasajero
                            
                            if (u.esConductor) {
                                buttonConductor.setBackgroundColor(requireContext().getColor(com.carpoolapp.R.color.primary))
                                buttonConductor.setTextColor(requireContext().getColor(com.carpoolapp.R.color.on_primary))
                                buttonPasajero.setBackgroundColor(Color.TRANSPARENT)
                                buttonPasajero.setTextColor(requireContext().getColor(com.carpoolapp.R.color.text_primary))
                                
                                binding.vehiculoCard.visibility = View.VISIBLE
                                
                                u.vehiculo?.let { vehiculo ->
                                    binding.marcaInput.setText(vehiculo.marca, false)
                                    binding.modeloInput.setText(vehiculo.modelo)
                                    binding.anoInput.setText(vehiculo.ano.toString(), false)
                                    binding.colorInput.setText(vehiculo.color, false)
                                    binding.placaInput.setText(vehiculo.placa)
                                }
                            } else {
                                buttonPasajero.setBackgroundColor(requireContext().getColor(com.carpoolapp.R.color.primary))
                                buttonPasajero.setTextColor(requireContext().getColor(com.carpoolapp.R.color.on_primary))
                                buttonConductor.setBackgroundColor(Color.TRANSPARENT)
                                buttonConductor.setTextColor(requireContext().getColor(com.carpoolapp.R.color.text_primary))
                                
                                binding.vehiculoCard.visibility = View.GONE
                            }
                        }
                        is PerfilUiState.Error -> {
                            binding.mensajeFeedback.text = "❌ ${state.mensaje}"
                            binding.mensajeFeedback.setTextColor(Color.parseColor("#C62828"))
                            binding.mensajeFeedback.setBackgroundResource(com.carpoolapp.R.drawable.feedback_background_error)
                            binding.mensajeFeedback.visibility = View.VISIBLE
                        }
                        is PerfilUiState.VehiculoActualizado -> {
                            mostrarFeedback("✅ Vehículo actualizado correctamente", true)
                        }
                        is PerfilUiState.FotoActualizada -> {
                            mostrarFeedback("✅ Foto de perfil actualizada", true)
                        }
                        is PerfilUiState.ConductorActualizado -> {
                            mostrarFeedback("✅ Tipo de usuario actualizado", true)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun mostrarFeedback(mensaje: String, exito: Boolean) {
        binding.mensajeFeedback.text = mensaje
        if (exito) {
            binding.mensajeFeedback.setTextColor(Color.parseColor("#2E7D32"))
            binding.mensajeFeedback.setBackgroundResource(com.carpoolapp.R.drawable.feedback_background)
        } else {
            binding.mensajeFeedback.setTextColor(Color.parseColor("#C62828"))
            binding.mensajeFeedback.setBackgroundResource(com.carpoolapp.R.drawable.feedback_background_error)
        }
        binding.mensajeFeedback.visibility = View.VISIBLE
        
        binding.root.postDelayed({
            binding.mensajeFeedback.visibility = View.GONE
        }, 3000)
    }
}

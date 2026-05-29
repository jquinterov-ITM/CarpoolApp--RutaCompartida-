package com.carpoolapp.ui.perfil

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import coil.transform.CircleCropTransformation
import com.carpoolapp.databinding.FragmentPerfilBinding
import com.carpoolapp.ui.common.BaseFragment
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@AndroidEntryPoint
class PerfilFragment : BaseFragment<FragmentPerfilBinding>() {

    private val viewModel: PerfilViewModel by viewModels()
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            procesarYSubirImagen(it)
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

        binding.tvNombre.setOnLongClickListener {
            mostrarDialogEditarNombre()
            true
        }

        binding.tipoToggle.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val esConductor = checkedId == com.carpoolapp.R.id.btn_conductor
                viewModel.toggleEsConductor(esConductor)
            }
        }

        binding.btnGuardarVehiculo.setOnClickListener {
            val vehiculo = com.carpoolapp.domain.model.Vehiculo(
                marca = binding.marcaInput.text.toString().trim(),
                modelo = binding.modeloInput.text.toString().trim(),
                ano = binding.anoInput.text.toString().toIntOrNull() ?: 0,
                color = binding.colorInput.text.toString().trim(),
                placa = binding.placaInput.text.toString().trim().uppercase()
            )
            viewModel.actualizarVehiculo(vehiculo)
        }

        binding.btnCerrarSesion.setOnClickListener {
            viewModel.cerrarSesion()
            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
            requireActivity().recreate()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observar el estado principal
                launch {
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
                                
                                android.util.Log.d("PerfilFragment", "Foto URL: ${u.fotoUrl?.take(50)}...")
                                
                                val fotoUrl = u.fotoUrl
                                if (!fotoUrl.isNullOrBlank()) {
                                    if (fotoUrl.startsWith("data:image")) {
                                        // Es una imagen base64, decodificarla directamente
                                        try {
                                            val base64String = fotoUrl.substringAfter("base64,")
                                            val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                            
                                            if (bitmap != null) {
                                                android.util.Log.d("PerfilFragment", "Bitmap decodificado: ${bitmap.width}x${bitmap.height}")
                                                // Aplicar crop circular manualmente
                                                val roundedBitmap = getRoundedBitmap(bitmap)
                                                binding.avatar.setImageBitmap(roundedBitmap)
                                            } else {
                                                android.util.Log.e("PerfilFragment", "Failed to decode bitmap")
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("PerfilFragment", "Error decodificando base64: ${e.message}", e)
                                        }
                                    } else {
                                        // Es una URL normal, usar Coil
                                        binding.avatar.load(fotoUrl) {
                                            crossfade(true)
                                            placeholder(com.carpoolapp.R.drawable.ic_person_grey_24dp)
                                            error(com.carpoolapp.R.drawable.ic_person_grey_24dp)
                                            transformations(CircleCropTransformation())
                                            listener(
                                                onStart = { android.util.Log.d("PerfilFragment", "Cargando imagen...") },
                                                onSuccess = { _, _ -> android.util.Log.d("PerfilFragment", "Imagen cargada exitosamente") },
                                                onError = { _, result -> android.util.Log.e("PerfilFragment", "Error cargando imagen: ${result.throwable}") }
                                            )
                                        }
                                    }
                                } else {
                                    android.util.Log.d("PerfilFragment", "No hay foto URL, mostrando placeholder")
                                }
                                
                                val esGoogle = auth.currentUser?.providerId == "google.com"
                                if (!esGoogle) {
                                    binding.tvNombre.setTextColor(requireContext().getColor(com.carpoolapp.R.color.primary))
                                }
                                
                                binding.ratingBar.rating = u.calificacion.toFloat()
                                binding.tvCalificacion.text = "${u.calificacion} / 5.0"
                                binding.tvViajesCompletados.text = "Viajes: ${u.viajesCompletados}"
                                binding.tvViajesConductor.text = "Como conductor: ${u.viajesComoConductor}"
                                binding.tvViajesPasajero.text = "Como pasajero: ${u.viajesComoPasajero}"
                                
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
                                    
                                    u.vehiculo?.let { v ->
                                        binding.marcaInput.setText(v.marca)
                                        binding.modeloInput.setText(v.modelo)
                                        binding.anoInput.setText(if (v.ano > 0) v.ano.toString() else "")
                                        binding.colorInput.setText(v.color)
                                        binding.placaInput.setText(v.placa)
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
                                binding.mensajeFeedback.text = " ${state.mensaje}"
                                binding.mensajeFeedback.setTextColor(Color.parseColor("#C62828"))
                                binding.mensajeFeedback.setBackgroundResource(com.carpoolapp.R.drawable.feedback_background_error)
                                binding.mensajeFeedback.visibility = View.VISIBLE
                            }
                            else -> {}
                        }
                    }
                }
                
                // Observar mensajes de feedback
                launch {
                    viewModel.feedbackMessage.collect { mensaje ->
                        if (!mensaje.isNullOrBlank()) {
                            mostrarFeedback(mensaje, mensaje.startsWith("✅"))
                        }
                    }
                }
            }
        }
    }

    private fun procesarYSubirImagen(uri: Uri) {
        try {
            android.util.Log.d("PerfilFragment", "Procesando imagen: $uri")
            
            val bitmap = requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                android.graphics.BitmapFactory.decodeStream(inputStream)
            }
            
            if (bitmap == null) {
                android.util.Log.e("PerfilFragment", "No se pudo decodificar la imagen")
                Toast.makeText(requireContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                return
            }
            
            android.util.Log.d("PerfilFragment", "Imagen original: ${bitmap.width}x${bitmap.height}")
            
            val resizedBitmap = resizeBitmap(bitmap, 512)
            val bytes = bitmapToByteArray(resizedBitmap)
            
            android.util.Log.d("PerfilFragment", "Tamaño después de redimensionar: ${bytes.size / 1024}KB")
            
            // Mostrar la imagen INMEDIATAMENTE en el avatar sin esperar Firestore
            val roundedBitmap = getRoundedBitmap(resizedBitmap)
            binding.avatar.setImageBitmap(roundedBitmap)
            android.util.Log.d("PerfilFragment", "Foto mostrada inmediatamente en el avatar")
            
            val fotoUrl = "data:image/jpeg;base64,${android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)}"
            android.util.Log.d("PerfilFragment", "Subiendo foto de ${fotoUrl.length} caracteres")
            
            viewModel.actualizarFotoUrl(fotoUrl)
        } catch (e: Exception) {
            android.util.Log.e("PerfilFragment", "Error procesando imagen: ${e.message}", e)
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height
        
        if (width > height) {
            if (width > maxSize) {
                height = (height * (maxSize.toFloat() / width)).toInt()
                width = maxSize
            }
        } else {
            if (height > maxSize) {
                width = (width * (maxSize.toFloat() / height)).toInt()
                height = maxSize
            }
        }
        
        android.util.Log.d("PerfilFragment", "Redimensionando a: ${width}x${height}")
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return stream.toByteArray()
    }

    private fun getRoundedBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }
        
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        
        val left = (bitmap.width - size) / 2f
        val top = (bitmap.height - size) / 2f
        canvas.drawBitmap(bitmap, left, top, paint)
        
        return output
    }

    private fun mostrarDialogEditarNombre() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Editar nombre")
        
        val input = android.widget.EditText(requireContext())
        input.hint = "Tu nombre"
        input.setText(binding.tvNombre.text.toString())
        builder.setView(input)
        
        builder.setPositiveButton("Guardar") { dialog, _ ->
            val nuevoNombre = input.text.toString().trim()
            if (nuevoNombre.isNotEmpty()) {
                viewModel.actualizarNombre(nuevoNombre)
            }
            dialog.dismiss()
        }
        
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
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

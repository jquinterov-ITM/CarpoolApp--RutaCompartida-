package com.carpoolapp.notifications

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.carpoolapp.MainActivity
import com.carpoolapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsuarioNotificationManager @Inject constructor(
    private val application: Application,
    private val auth: FirebaseAuth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var listener: ListenerRegistration? = null
    private val prefs: SharedPreferences by lazy {
        application.getSharedPreferences("notificaciones_usuario", Context.MODE_PRIVATE)
    }
    
    private val notificadas: MutableSet<String>
        get() = prefs.getStringSet("notificadas_usuario", emptySet())?.toMutableSet() ?: mutableSetOf()
    
    private fun agregarNotificada(id: String) {
        val current = notificadas
        current.add(id)
        prefs.edit { putStringSet("notificadas_usuario", current) }
    }
    
    fun startListening() {
        val usuarioId = auth.currentUser?.uid ?: run {
            android.util.Log.w("UsuarioNotifMgr", "No hay usuario autenticado")
            return
        }
        
        android.util.Log.d("UsuarioNotifMgr", "Iniciando escucha de notificaciones para: $usuarioId")
        crearCanalNotificacion()
        
        val firestore = FirebaseFirestore.getInstance()
        listener = firestore.collection("notifications")
            .whereEqualTo("userId", usuarioId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("UsuarioNotifMgr", "Error escuchando notificaciones", error)
                    return@addSnapshotListener
                }
                
                val notificaciones = snapshot?.documents?.map { doc ->
                    Pair(doc, doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L)
                }?.sortedByDescending { it.second }?.take(20) ?: emptyList()
                
                notificaciones.forEach { (doc, _) ->
                    val notifId = doc.id
                    val leida = doc.getBoolean("leida") ?: false
                    
                    if (!leida && !notificadas.contains(notifId)) {
                        val titulo = doc.getString("titulo") ?: "CarpoolApp"
                        val cuerpo = doc.getString("cuerpo") ?: ""
                        val tripId = doc.getString("tripId") ?: ""
                        val tipo = doc.getString("tipo") ?: "general"
                        
                        agregarNotificada(notifId)
                        doc.reference.update("leida", true)
                        mostrarNotificacion(titulo, cuerpo, tipo, tripId)
                    }
                }
            }
    }
    
    fun stopListening() {
        android.util.Log.d("UsuarioNotifMgr", "Deteniendo escucha de notificaciones")
        listener?.remove()
        listener = null
        scope.cancel()
    }
    
    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "carpool_notifications",
                "CarpoolApp",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de viajes y solicitudes"
                enableVibration(true)
            }
            val manager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun mostrarNotificacion(titulo: String, cuerpo: String, tipo: String, tripId: String) {
        val channelId = "carpool_notifications"
        val manager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(application, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (tripId.isNotEmpty()) {
                putExtra("tripId", tripId)
                putExtra("navigateToTrip", true)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            application,
            tripId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconId = when (tipo) {
            "solicitud_aceptada" -> R.drawable.ic_person_grey_24dp
            "solicitud_rechazada" -> R.drawable.ic_person_grey_24dp
            else -> R.drawable.ic_launcher_foreground
        }

        val notification = NotificationCompat.Builder(application, channelId)
            .setSmallIcon(iconId)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt() + tripId.hashCode(), notification)
        android.util.Log.d("UsuarioNotifMgr", "Notificación mostrada: $titulo - $cuerpo")
    }
}

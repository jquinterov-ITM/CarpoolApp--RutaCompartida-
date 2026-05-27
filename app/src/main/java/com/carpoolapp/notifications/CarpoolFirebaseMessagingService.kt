package com.carpoolapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.carpoolapp.MainActivity
import com.carpoolapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CarpoolFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var firestore: FirebaseFirestore

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val tipo = data["tipo"] ?: "general"
        val tripId = data["tripId"] ?: ""
        
        val titulo = message.notification?.title ?: "CarpoolApp"
        val cuerpo = message.notification?.body ?: ""
        
        mostrarNotificacion(titulo, cuerpo, tipo, tripId)
    }

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("fcmToken", token)
    }

    private fun mostrarNotificacion(titulo: String, cuerpo: String, tipo: String = "general", tripId: String = "") {
        val channelId = "carpool_notifications"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "CarpoolApp", 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de viajes y solicitudes"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (tripId.isNotEmpty()) {
                putExtra("tripId", tripId)
                putExtra("navigateToTrip", true)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 
            tripId.hashCode(), 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconId = when (tipo) {
            "solicitud_nueva" -> R.drawable.ic_person_grey_24dp
            "solicitud_aceptada" -> R.drawable.ic_person_grey_24dp
            "solicitud_rechazada" -> R.drawable.ic_person_grey_24dp
            else -> R.drawable.ic_launcher_foreground
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(iconId)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt() + tripId.hashCode(), notification)
    }
}

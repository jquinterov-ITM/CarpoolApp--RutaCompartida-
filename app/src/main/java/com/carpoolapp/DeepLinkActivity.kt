package com.carpoolapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DeepLinkActivity : AppCompatActivity() {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: Uri? = intent.data
        Log.d("DeepLinkActivity", "Deep link received: $data")

        if (data != null) {
            val code = data.getQueryParameter("code")
            val state = data.getQueryParameter("state")
            val sessionToken = data.getQueryParameter("session_token")

            Log.d("DeepLinkActivity", "OAuth params - code=$code, state=$state, session_token=$sessionToken")

            if (!code.isNullOrEmpty()) {
                // Handle OAuth callback - in production, exchange code for token
                lifecycleScope.launch {
                    try {
                        // TODO: Exchange code for Clerk JWT token
                        // For now, set a placeholder authenticated state
                        // This would call your backend endpoint to exchange the code
                        
                        Log.d("DeepLinkActivity", "OAuth code received successfully")
                        
                        // Navigate back to MainActivity with success intent
                        val mainIntent = Intent(this@DeepLinkActivity, MainActivity::class.java)
                        mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        mainIntent.putExtra("oauth_success", true)
                        startActivity(mainIntent)
                        finish()
                    } catch (e: Exception) {
                        Log.e("DeepLinkActivity", "OAuth error: ${e.message}", e)
                        val mainIntent = Intent(this@DeepLinkActivity, MainActivity::class.java)
                        mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        mainIntent.putExtra("oauth_error", e.message)
                        startActivity(mainIntent)
                        finish()
                    }
                }
            } else {
                Log.w("DeepLinkActivity", "No code in OAuth callback")
                val mainIntent = Intent(this@DeepLinkActivity, MainActivity::class.java)
                mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                mainIntent.putExtra("oauth_error", "No authorization code received")
                startActivity(mainIntent)
                finish()
            }
        } else {
            // No deep link data
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}

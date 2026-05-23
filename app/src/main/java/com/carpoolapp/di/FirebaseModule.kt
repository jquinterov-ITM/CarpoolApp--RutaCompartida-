package com.carpoolapp.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirestore(@ApplicationContext ctx: Context): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        val prefs = ctx.getSharedPreferences("carpool_prefs", Context.MODE_PRIVATE)
        // Disabled by default - enable manually if emulator is running
        val useEmulator = prefs.getBoolean("use_emulator", false)
        if (useEmulator) {
            try {
                firestore.useEmulator("localhost", 8080)
            } catch (e: IllegalStateException) {
                // Already initialized, ignore
            }
        }
        return firestore
    }

    @Provides
    @Singleton
    fun provideAuth(@ApplicationContext ctx: Context): FirebaseAuth {
        val auth = FirebaseAuth.getInstance()
        val prefs = ctx.getSharedPreferences("carpool_prefs", Context.MODE_PRIVATE)
        // Disabled by default - enable manually if emulator is running
        val useEmulator = prefs.getBoolean("use_emulator", false)
        if (useEmulator) {
            try {
                auth.useEmulator("localhost", 9099)
            } catch (e: IllegalStateException) {
                // Already initialized, ignore
            }
        }
        return auth
    }
}

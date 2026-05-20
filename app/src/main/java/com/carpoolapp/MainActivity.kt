package com.carpoolapp

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.carpoolapp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var appBarConfiguration: AppBarConfiguration? = null
    private var navControllerRef: androidx.navigation.NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navControllerRef = navController

        // Top-level destinations for bottom navigation so Up behaves correctly
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.buscarFragment,
                R.id.misViajesFragment,
                R.id.perfilFragment
            )
        )

        binding.bottomNavigation.setupWithNavController(navController)

        // Evitar recargar el fragmento al reseleccionar el mismo item
        binding.bottomNavigation.setOnItemReselectedListener { /* no-op */ }

        // Ocultar bottom navigation en pantallas sin navegación principal (ej: Auth)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.authFragment -> binding.bottomNavigation.visibility = View.GONE
                else -> binding.bottomNavigation.visibility = View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = navControllerRef
        return if (navController != null && appBarConfiguration != null) {
            navController.navigateUp(appBarConfiguration!!) || super.onSupportNavigateUp()
        } else {
            super.onSupportNavigateUp()
        }
    }
}

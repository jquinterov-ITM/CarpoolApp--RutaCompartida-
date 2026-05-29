package com.carpoolapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import com.carpoolapp.databinding.ActivityMainBinding
import com.carpoolapp.notifications.SolicitudNotificationManager
import com.carpoolapp.notifications.UsuarioNotificationManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var notificationManager: SolicitudNotificationManager

    @Inject
    lateinit var usuarioNotificationManager: UsuarioNotificationManager

    private lateinit var binding: ActivityMainBinding
    private var appBarConfiguration: AppBarConfiguration? = null
    private var navControllerRef: androidx.navigation.NavController? = null
    private val tabBackStack: ArrayDeque<Int> = ArrayDeque()
    private lateinit var navHostTagMap: Map<Int, String>
    private var currentItemIdField: Int = R.id.homeFragment
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        // Iniciar el manejador centralizado de notificaciones de solicitudes (conductor)
        notificationManager.startListening()
        
        // Iniciar el manejador de notificaciones para el usuario (pasajero)
        usuarioNotificationManager.startListening()

        // Manejar deep link desde notificación
        handleNotificationIntent(intent)

        val isAuthenticated = firebaseAuth.currentUser != null
        val useTabNavigation = isAuthenticated


        // Map bottom menu item -> navigation graph resource
        val navGraphMap = if (useTabNavigation) mapOf(
            R.id.homeFragment to R.navigation.nav_home,
            R.id.buscarFragment to R.navigation.nav_buscar,
            R.id.misViajesFragment to R.navigation.nav_mis_viajes,
            R.id.perfilFragment to R.navigation.nav_perfil
        ) else emptyMap()

        // Create NavHostFragments for each tab and attach them (hidden by default)
        val fragmentManager = supportFragmentManager
        val containerId = R.id.nav_host_fragment
        val navHostTags = mutableMapOf<Int, String>()
        
        if (useTabNavigation) {
            navGraphMap.forEach { (menuId, graphRes) ->
                val tag = "navHost_${menuId}"
                navHostTags[menuId] = tag
                var navHost = fragmentManager.findFragmentByTag(tag) as? NavHostFragment
                if (navHost == null) {
                    navHost = NavHostFragment.create(graphRes)
                    fragmentManager.beginTransaction()
                        .add(containerId, navHost, tag)
                        .hide(navHost)
                        .commitNow()
                }
            }
        } else {
            var authNavHost = fragmentManager.findFragmentByTag("navHost_auth") as? NavHostFragment
            if (authNavHost == null) {
                authNavHost = NavHostFragment.create(R.navigation.nav_graph)
                fragmentManager.beginTransaction()
                    .add(containerId, authNavHost, "navHost_auth")
                    .commitNow()
            }
        }

        // Keep track of current selected menu id (persist across rotations)
        currentItemIdField = if (useTabNavigation) {
            savedInstanceState?.getInt("currentItemId")
                ?: binding.bottomNavigation.selectedItemId.takeIf { it != View.NO_ID }
                ?: R.id.homeFragment
        } else {
            R.id.authFragment
        }

        // Expose navHostTags map to rest of class
        navHostTagMap = navHostTags

        // Listen to auth state changes and switch to auth flow when user is null
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (user == null) {
                // Defer UI fragment transactions until Activity is resumed to avoid
                // IllegalStateException "Can not perform this action after onSaveInstanceState"
                lifecycleScope.launchWhenResumed {
                    binding.bottomNavigation.visibility = View.GONE
                    val fragmentManager = supportFragmentManager
                    // hide any tab nav hosts
                    navHostTagMap.values.forEach { tag ->
                        fragmentManager.findFragmentByTag(tag)?.let { f ->
                            fragmentManager.beginTransaction().hide(f).commitAllowingStateLoss()
                        }
                    }
                    var authNavHost = fragmentManager.findFragmentByTag("navHost_auth") as? NavHostFragment
                    if (authNavHost == null) {
                        authNavHost = NavHostFragment.create(R.navigation.nav_graph)
                        fragmentManager.beginTransaction()
                            .add(R.id.nav_host_fragment, authNavHost, "navHost_auth")
                            .commitAllowingStateLoss()
                    } else {
                        fragmentManager.beginTransaction().show(authNavHost).commitAllowingStateLoss()
                    }
                    navControllerRef = authNavHost.navController
                    // navigate to authFragment if available
                    try {
                        navControllerRef?.navigate(R.id.authFragment)
                    } catch (_: Exception) { }
                }
            } else {
                // User already authenticated, tab navigation already set up in onCreate.
                // No recreate() needed - it was causing navigation issues.
                android.util.Log.d("MainActivity", "User authenticated, navigation ready")
            }
        }
        firebaseAuth.addAuthStateListener(authStateListener!!)

        if (useTabNavigation) {
            // Tab-based navigation setup
            requestNotificationPermission()
            
            val listenerMap = mutableMapOf<Int, NavController.OnDestinationChangedListener>()

            fun showTab(menuId: Int) {
                val tagToShow = navHostTags[menuId] ?: return
                fragmentManager.fragments.forEach { f ->
                    val t = f.tag
                    val transaction = fragmentManager.beginTransaction()
                    if (t == tagToShow) transaction.show(f) else transaction.hide(f)
                    transaction.commitNow()
                }
                currentItemIdField = menuId
                val navHost = fragmentManager.findFragmentByTag(tagToShow) as NavHostFragment
                navControllerRef = navHost.navController
                
                if (!listenerMap.containsKey(menuId)) {
                    val listener = NavController.OnDestinationChangedListener { _: NavController, destination: NavDestination, _: Bundle? ->
                        binding.bottomNavigation.visibility = if (destination.id == R.id.authFragment) View.GONE else View.VISIBLE
                    }
                    listenerMap[menuId] = listener
                    navHost.navController.addOnDestinationChangedListener(listener)
                }
            }

            // Initial display
            showTab(currentItemIdField)

            binding.bottomNavigation.setOnItemSelectedListener { item: MenuItem ->
                val itemId = item.itemId
                if (itemId == currentItemIdField) {
                    val currentTag = navHostTags[itemId] ?: return@setOnItemSelectedListener true
                    val nav = fragmentManager.findFragmentByTag(currentTag) as NavHostFragment
                    nav.navController.popBackStack(nav.navController.graph.startDestinationId, false)
                    return@setOnItemSelectedListener true
                }
                if (currentItemIdField != View.NO_ID) {
                    tabBackStack.addLast(currentItemIdField)
                }
                showTab(itemId)
                true
            }

            binding.bottomNavigation.setOnItemReselectedListener { /* no-op */ }
        } else {
            // Auth flow: hide bottom navigation, use main nav graph
            binding.bottomNavigation.visibility = View.GONE
            val authNavHost = fragmentManager.findFragmentByTag("navHost_auth") as? NavHostFragment
            if (authNavHost != null) {
                navControllerRef = authNavHost.navController
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = navControllerRef
        return if (navController != null && appBarConfiguration != null) {
            NavigationUI.navigateUp(navController, appBarConfiguration!!) || super.onSupportNavigateUp()
        } else {
            super.onSupportNavigateUp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authStateListener?.let { firebaseAuth.removeAuthStateListener(it) }
        notificationManager.stopListening()
        usuarioNotificationManager.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }
    
    private fun handleNotificationIntent(intent: Intent) {
        val navigateToTrip = intent.getBooleanExtra("navigateToTrip", false)
        val tripId = intent.getStringExtra("tripId")
        
        if (navigateToTrip && tripId != null) {
            lifecycleScope.launchWhenResumed {
                val currentNav = navControllerRef
                if (currentNav != null) {
                    try {
                        val action = com.carpoolapp.ui.home.HomeFragmentDirections.actionHomeToDetalle(
                            tripId = tripId
                        )
                        currentNav.navigate(action)
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Error navigating to trip: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        val currentTag = navHostTagMap[currentItemIdField]
        if (currentTag != null) {
            val fragment = supportFragmentManager.findFragmentByTag(currentTag) as? NavHostFragment
            val nav = fragment?.navController
            if (nav != null && nav.popBackStack()) {
                return
            }
        }

        if (tabBackStack.isNotEmpty()) {
            val previous = tabBackStack.removeLast()
            binding.bottomNavigation.selectedItemId = previous
            return
        }

        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentItemId", currentItemIdField)
        val arr = tabBackStack.toIntArray()
        outState.putIntArray("tabBackStack", arr)
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}

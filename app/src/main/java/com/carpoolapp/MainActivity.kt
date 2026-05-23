package com.carpoolapp

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.carpoolapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private lateinit var binding: ActivityMainBinding
    private var appBarConfiguration: AppBarConfiguration? = null
    private var navControllerRef: androidx.navigation.NavController? = null
    private val tabBackStack: ArrayDeque<Int> = ArrayDeque()
    private lateinit var navHostTagMap: Map<Int, String>
    private var currentItemIdField: Int = R.id.homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Check for OAuth success flag
        val oauthSuccess = intent.getBooleanExtra("oauth_success", false)
        
        // Check if user is authenticated
        val isAuthenticated = firebaseAuth.currentUser != null || oauthSuccess
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
                val tag = "navHost_\${menuId}"
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
            // Show auth graph instead
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

        if (useTabNavigation) {
            // Tab-based navigation setup
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

    override fun onBackPressed() {
        // Try pop current nav controller
        val currentTag = navHostTagMap[currentItemIdField]
        if (currentTag != null) {
            val fragment = supportFragmentManager.findFragmentByTag(currentTag) as? NavHostFragment
            val nav = fragment?.navController
            if (nav != null && nav.popBackStack()) {
                return
            }
        }

        // If can't pop and have previous tab, go to it
        if (tabBackStack.isNotEmpty()) {
            val previous = tabBackStack.removeLast()
            binding.bottomNavigation.selectedItemId = previous
            // showTab will be triggered by listener
            return
        }

        // Default behavior
        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentItemId", currentItemIdField)
        // save tabBackStack as int array
        val arr = tabBackStack.toIntArray()
        outState.putIntArray("tabBackStack", arr)
    }
}

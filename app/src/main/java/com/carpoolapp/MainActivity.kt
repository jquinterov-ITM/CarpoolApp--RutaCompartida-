package com.carpoolapp

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.carpoolapp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

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

        // Map bottom menu item -> navigation graph resource
        val navGraphMap = mapOf(
            R.id.homeFragment to R.navigation.nav_home,
            R.id.buscarFragment to R.navigation.nav_buscar,
            R.id.misViajesFragment to R.navigation.nav_mis_viajes,
            R.id.perfilFragment to R.navigation.nav_perfil
        )

        // Create NavHostFragments for each tab and attach them (hidden by default)
        val fragmentManager = supportFragmentManager
        val containerId = R.id.nav_host_fragment
        val navHostTags = mutableMapOf<Int, String>()
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

        // Keep track of current selected menu id (persist across rotations)
        currentItemIdField = savedInstanceState?.getInt("currentItemId")
            ?: binding.bottomNavigation.selectedItemId.takeIf { it != View.NO_ID }
            ?: R.id.homeFragment

        // expose navHostTags map to rest of class
        navHostTagMap = navHostTags

        // Show initial tab
        fun showTab(menuId: Int) {
            val tagToShow = navHostTags[menuId] ?: return
            fragmentManager.fragments.forEach { f ->
                val t = f.tag
                val transaction = fragmentManager.beginTransaction()
                if (t == tagToShow) transaction.show(f) else transaction.hide(f)
                transaction.commitNow()
            }
            // update selection
            currentItemIdField = menuId
            val navHost = fragmentManager.findFragmentByTag(tagToShow) as NavHostFragment
            navControllerRef = navHost.navController
            // attach destination listener to show/hide bottom nav for auth screens
            navHost.navController.addOnDestinationChangedListener { _, destination, _ ->
                binding.bottomNavigation.visibility = if (destination.id == R.id.authFragment) View.GONE else View.VISIBLE
            }
        }

        // Initial display
        showTab(currentItemIdField)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val id = item.itemId
            if (id == currentItemIdField) {
                // already selected, pop to start if possible
                val currentTag = navHostTags[id] ?: return@setOnItemSelectedListener true
                val nav = fragmentManager.findFragmentByTag(currentTag) as NavHostFragment
                nav.navController.popBackStack(nav.navController.graph.startDestinationId, false)
                return@setOnItemSelectedListener true
            }
            // push current to tab back stack before switching
            if (currentItemIdField != View.NO_ID) {
                tabBackStack.addLast(currentItemIdField)
            }
            showTab(id)
            true
        }

        // No-op on reselection
        binding.bottomNavigation.setOnItemReselectedListener { /* no-op */ }
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

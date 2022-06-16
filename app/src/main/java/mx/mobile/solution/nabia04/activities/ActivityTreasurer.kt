package mx.mobile.solution.nabia04.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smarttoolfactory.tutorial7_2bnv_viewpager2_complexarchitecture.viewmodel.TreasurerAppbarViewModel
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.ActivityMainBinding
import mx.mobile.solution.nabia04.databinding.ActivityTreasurerBinding
import mx.mobile.solution.nabia04.main.setupWithNavController
import mx.mobile.solution.nabia04.main.util.Event

class ActivityTreasurer : AppCompatActivity() {

    private val appbarViewModel by viewModels<TreasurerAppbarViewModel>()

    private lateinit var dataBinding: ActivityTreasurerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_treasurer)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        } // Else, need to wait for onRestoreInstanceState
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar()
    }

    private fun setupBottomNavigationBar() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)

        val navGraphIds = listOf(
            R.navigation.treasurer_nav_graph,
            R.navigation.good_standing_nav_graph,
            R.navigation.tools_nav_graph
        )

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = bottomNavigationView.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_container,
            intent = intent
        )

        // Whenever the selected controller changes, setup the action bar.
        controller.observe(this, Observer { navController ->
            val appBarConfig = AppBarConfiguration(navController.graph)
            dataBinding.toolbar.setupWithNavController(navController, appBarConfig)
        })

        appbarViewModel.currentNavController.observe(this, Observer { it ->

            it?.let { event: Event<NavController> ->
                event.getContentIfNotHandled()?.let { navController ->
                    val appBarConfig = AppBarConfiguration(navController.graph)
                    dataBinding.toolbar.setupWithNavController(navController, appBarConfig)
                }
            }
        })
    }
}

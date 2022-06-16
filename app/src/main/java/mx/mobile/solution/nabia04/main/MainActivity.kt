package mx.mobile.solution.nabia04.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smarttoolfactory.tutorial7_2bnv_viewpager2_complexarchitecture.viewmodel.MainAppbarViewModel
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.ActivityMainBinding
import mx.mobile.solution.nabia04.main.fragment.host_fragments.NoticeBoardHostFragment
import mx.mobile.solution.nabia04.main.fragment.welfare.ExcelHelper
import mx.mobile.solution.nabia04.main.util.Event
import mx.mobile.solution.nabia04.room_database.view_models.AnnLoadingStatusViewModel
import mx.mobile.solution.nabia04.room_database.view_models.AnnViewModel
import mx.mobile.solution.nabia04.room_database.view_models.DBLoadingStatusViewModel
import mx.mobile.solution.nabia04.room_database.view_models.DatabaseListViewModel
import pub.devrel.easypermissions.EasyPermissions


/*
   *** Navigation Architecture ***

     MainActivity(BottomNavigationView + ViewPager2 + Appbar + Toolbar)
        |
        |- ViewPagerContainerFragment(TabLayout + ViewPager2)
        |      |
        |      |- HomeNavHostFragment
        |      |  |- HF1 -> HF2 -> HF3
        |      |
        |      |- DashboardNavHostFragment
        |      |  |- DF1 -> DF2 -> DF3
        |      |
        |      |- NotificationHostFragment
        |      |  |- NF1 -> NF2 -> NF3
        |      |
        |      |-LoginFragment1 -> LoginFragment
        |
        |- DashboardNavHostFragment
        |   |- DF1 -> DF2 -> DF3
        |
        |- NotificationHostFragment
        |   |- NF1 -> NF2 -> NF3
 */



/**
 * This example is combination of Tutorial6-6 and Tutorial 7-1
 *
 * * First tab of the [BottomNavigationView] is [NoticeBoardHostFragment] which has
 * a [ViewPager2] that has it's own pages with each it's own back stack
 *
 * * Setting [NavController] is done both using [MainAppbarViewModel]
 * and [BottomNavigationView.setupWithNavController]
 * in the NavigationExtensions code for setting BottomNavigationView back stack
 */
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private val PERMISSION_STORAGE_REQUEST: Int = 877
    private val appbarViewModel by viewModels<MainAppbarViewModel>()

    private lateinit var dataBinding: ActivityMainBinding

    companion object {
        lateinit var annloadingStatus: AnnLoadingStatusViewModel
        lateinit var databaseLoadingStatus: DBLoadingStatusViewModel
        lateinit var annViewModel: AnnViewModel
        lateinit var databaseViewModel: DatabaseListViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        annloadingStatus = ViewModelProvider(this).get(AnnLoadingStatusViewModel::class.java)
        databaseLoadingStatus = ViewModelProvider(this).get(DBLoadingStatusViewModel::class.java)
        annViewModel = ViewModelProvider(this).get(AnnViewModel::class.java)
        databaseViewModel = ViewModelProvider(this).get(DatabaseListViewModel::class.java)

        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        } // Else, need to wait for onRestoreInstanceState
        requestPermissions()
    }

    private fun requestPermissions() {
        if (EasyPermissions.hasPermissions(this,Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)) {
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission() {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data =
                    Uri.parse(String.format("package:%s", applicationContext.packageName))
                startActivityForResult(intent, 2296)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, 2296)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }


    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        TODO("Not yet implemented")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        TODO("Not yet implemented")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar()
    }

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigationBar() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)

        val navGraphIds = listOf(
            R.navigation.notification_nav_graph,
            R.navigation.database_nav_graph,
            R.navigation.welfare_nav_graph,
            R.navigation.prof_nav_graph,
            R.navigation.gallery_nav_graph
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

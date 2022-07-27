package mx.mobile.solution.nabia04.ui.activities

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import androidx.work.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.authentication.AuthenticationActivity
import mx.mobile.solution.nabia04.data.view_models.*
import mx.mobile.solution.nabia04.databinding.ActivityMainBinding
import mx.mobile.solution.nabia04.fcm_.SendTokenToServerWorker
import mx.mobile.solution.nabia04.main.setupWithNavController
import mx.mobile.solution.nabia04.util.Event
import mx.mobile.solution.nabia04.utilities.Const
import mx.mobile.solution.nabia04.utilities.ExcelHelper
import mx.mobile.solution.nabia04.utilities.SessionManager
import mx.mobile.solution.nabia04.workManager.TokenRefreshWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject


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
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val PERMISSION_REQUEST_CODE = 9684
    private lateinit var navController: NavController
    private val RC_APP_PERM = 1244
    private val appbarViewModel by viewModels<MainAppbarViewModel>()

    private lateinit var dataBinding: ActivityMainBinding

    @Inject
    lateinit var excelHelper: ExcelHelper

    @Inject
    lateinit var sharedP: SharedPreferences

    companion object {
        lateinit var userFolioNumber: String
        lateinit var clearance: String
    }

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        userFolioNumber = sharedP.getString(SessionManager.FOLIO_NUMBER, "") ?: ""
        clearance = sharedP.getString(Const.CLEARANCE, "") ?: ""

        drawerLayout = dataBinding.drawerLayout

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        }

        lifecycleScope.launch {
            initializeExcel()
        }

        scheduleSendingTokenToServer()

        //scheduleTokenRefresh()
    }

    private fun scheduleSendingTokenToServer() {
        val myWorkRequest: WorkRequest =
            OneTimeWorkRequest.Builder(SendTokenToServerWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
        WorkManager.getInstance(applicationContext).enqueue(myWorkRequest)
    }

    private fun scheduleTokenRefresh() {
        val requestID = "scheduleTokenRefresh"
        val myWorkRequest =
            PeriodicWorkRequest.Builder(TokenRefreshWorker::class.java, 15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(requestID)
                .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(requestID, ExistingPeriodicWorkPolicy.REPLACE, myWorkRequest)
    }

    private fun checkPermission(): Boolean {
        return if (SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val result =
                ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
            val result1 =
                ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
            result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                startActivityForResult(intent, 2296)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, 2296)
            }
        } else {
            //below android 11
            ActivityCompat.requestPermissions(
                this, arrayOf(WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2296) {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Log.i("TAG", "Permission Granted")
                    excelHelper.createExcel()
                } else {
                    Log.i("TAG", "Permission not Granted")
                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (grantResults.isNotEmpty()) {
                val READ_EXTERNAL_STORAGE = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val WRITE_EXTERNAL_STORAGE = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (READ_EXTERNAL_STORAGE && WRITE_EXTERNAL_STORAGE) {
                    Log.i("TAG", "Permission Granted")
                    excelHelper.createExcel()
                } else {
                    Log.i("TAG", "Permission not Granted")
                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private suspend fun initializeExcel() {
        val hasPermission = checkPermission()
        Log.i("TAG", "hasPermission: $hasPermission")
        if (checkPermission()) {
            withContext(Dispatchers.IO) {
                excelHelper.createExcel()
            }
        } else {
            Log.i("TAG", "Requesting permissions")
            requestPermission()
        }

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

        //Setup the bottom navigation view with a list of navigation graphs
        val controller = bottomNavigationView.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_container,
            intent = intent
        )

        //Whenever the selected controller changes, setup the action bar.
        controller.observe(this) { navController ->
            val appBarConfig = AppBarConfiguration(navController.graph, drawerLayout)
            dataBinding.toolbar.setupWithNavController(navController, appBarConfig)
            NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)
        }

        appbarViewModel.currentNavController.observe(this) {
            it?.let { event: Event<NavController> ->
                event.getContentIfNotHandled()?.let { navController ->
                    val appBarConfig = AppBarConfiguration(navController.graph, drawerLayout)
                    dataBinding.toolbar.setupWithNavController(navController, appBarConfig)
                    NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.i("TAG", "onNavigationItemSelected")
        when (item.itemId) {

            R.id.treasurer -> {
                Log.i("TAG", "onNavigationItemSelected treasurer")
                val i = Intent(this, ActivityTreasurerTools::class.java)
                startActivity(i)
            }

            R.id.nav_logout -> {
                AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
                    .setMessage("LOGOUT")
                    .setTitle("Are you sure you want to logout?")
                    .setPositiveButton("YES") { dialog, _ ->
                        dialog.dismiss()
                        logoutUser()
                    }.setNegativeButton("NO") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
//
//            R.id.nav_privacy_policy -> {
//                //navController.navigate(Uri.parse("loveletter://agreement/privacy-policy"))
//            }
//
//            R.id.nav_terms_of_service -> {
//                //navController.navigate(Uri.parse("loveletter://agreement/terms-of-service"))
//            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logoutUser() {

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        );

        SessionManager.clearData()

        deleteDatabase("main_database");

        val i = Intent(this, AuthenticationActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        finishAffinity()

        startActivity(i)
    }

}

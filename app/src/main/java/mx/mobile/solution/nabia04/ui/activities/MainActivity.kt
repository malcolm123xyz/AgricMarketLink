@file:Suppress("KDocUnresolvedReference")

package mx.mobile.solution.nabia04.ui.activities

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
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
import android.widget.Toast
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
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import androidx.work.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.App
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.view_models.*
import mx.mobile.solution.nabia04.databinding.ActivityMainBinding
import mx.mobile.solution.nabia04.fcm_.SendTokenToServerWorker
import mx.mobile.solution.nabia04.utilities.Const
import mx.mobile.solution.nabia04.utilities.ExcelHelper
import mx.mobile.solution.nabia04.utilities.SessionManager
import mx.mobile.solution.nabia04.workManager.ExcelDownloadWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime


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
    private val permissionRequestCode = 9684

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var dataBinding: ActivityMainBinding

    @Inject
    lateinit var excelHelper: ExcelHelper

    @Inject
    lateinit var sharedP: SharedPreferences

    companion object {
        lateinit var userFolioNumber: String
        lateinit var clearance: String
        var startMils: Long = 0L
    }

    private lateinit var drawerLayout: DrawerLayout

    @OptIn(ExperimentalTime::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        startMils = System.currentTimeMillis()

        val m = measureTime {
            dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_container)
                    as NavHostFragment
            navController = navHostFragment.navController

            // Setup the bottom navigation view with navController
            val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
            bottomNavigationView.setupWithNavController(navController)

            drawerLayout = dataBinding.drawerLayout

            // Setup the ActionBar with navController and 3 top level destinations
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.notice_board_host_frag,
                    R.id.database_host_frag,
                    R.id.welfare_host_frag_nav,
                    R.id.fragment_main_view_nav
                ), drawerLayout
            )

            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)

            toolbar.setupWithNavController(navController, appBarConfiguration)

            userFolioNumber = sharedP.getString(SessionManager.FOLIO_NUMBER, "") ?: ""
            clearance = sharedP.getString(Const.CLEARANCE, "") ?: ""
            Log.i("TAG", "USER CLEARANCE = $clearance")

            val navigationView = findViewById<NavigationView>(R.id.nav_view)
            navigationView.setNavigationItemSelectedListener(this)

            scheduleSendingTokenToServer()

            if (checkPermission()) {
                lifecycleScope.launch {
                    initializeExcel()
                    scheduleExcelDownloader()
                }
            } else {
                requestPermission()
            }
        }

        Log.i("TAG", "Activity created in $m")
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

    private fun requestPermission() {
        Log.i("TAG", "requestPermissions")

        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data =
                    Uri.parse(String.format("package:%s", App.applicationContext().packageName))
                startActivityForResult(intent, 2296)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, 2296)
            }
        } else {
            Log.i("TAG", "requestPermissions beow android 11")
            //below android 11
            ActivityCompat.requestPermissions(
                this, arrayOf(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE),
                permissionRequestCode
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2296) {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Log.i("TAG", "Permission Granted")
                    lifecycleScope.launch {
                        initializeExcel()
                    }
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
            permissionRequestCode -> if (grantResults.isNotEmpty()) {
                val readExtStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val writeExtStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (readExtStorage && writeExtStorage) {
                    Log.i("TAG", "Permission Granted")
                    lifecycleScope.launch {
                        initializeExcel()
                    }
                } else {
                    Log.i("TAG", "Permission not Granted")
                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun initializeExcel() {
        withContext(Dispatchers.Default) {
            val n = measureTime {
                excelHelper.initialize()
            }
            Log.i("TAG", "Excel innitialization took: $n")
        }
    }

//    private fun scheduleTokenRefresh() {
//        val requestID = "scheduleTokenRefresh"
//        val myWorkRequest =
//            PeriodicWorkRequest.Builder(TokenRefreshWorker::class.java, 15, TimeUnit.MINUTES)
//                .setConstraints(
//                    Constraints.Builder()
//                        .setRequiredNetworkType(NetworkType.CONNECTED)
//                        .build()
//                )
//                .setBackoffCriteria(
//                    BackoffPolicy.LINEAR,
//                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
//                    TimeUnit.MILLISECONDS
//                )
//                .addTag(requestID)
//                .build()
//        WorkManager.getInstance(applicationContext)
//            .enqueueUniquePeriodicWork(requestID, ExistingPeriodicWorkPolicy.REPLACE, myWorkRequest)
//    }

    private fun scheduleExcelDownloader() {
        val requestID = "scheduleExcelDownloader"
        val myWorkRequest =
            PeriodicWorkRequest.Builder(ExcelDownloadWorker::class.java, 1, TimeUnit.DAYS)
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
            .enqueueUniquePeriodicWork(requestID, ExistingPeriodicWorkPolicy.KEEP, myWorkRequest)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.pro -> {

                if (isPro()) {
                    val i = Intent(this, ActivityProTools::class.java)
                    startActivity(i)
                } else {
                    Toast.makeText(this, "Only the Pro can access this section", Toast.LENGTH_SHORT)
                        .show()
                }

            }

            R.id.treasurer -> {

                if (isTreasurer()) {
                    val i = Intent(this, ActivityTreasurerTools::class.java)
                    startActivity(i)
                } else {
                    Toast.makeText(
                        this,
                        "Only the Treasurer can access this section",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }

            R.id.settings -> {
                val i = Intent(this, SettingsActivity::class.java)
                startActivity(i)
            }

            R.id.nav_about_navasco -> {
                val i = Intent(this, ActivityAboutNavasco::class.java)
                startActivity(i)
            }

            R.id.nav_about_nabia_04 -> {
                val i = Intent(this, ActivityNabia04::class.java)
                startActivity(i)
            }

            R.id.nav_about_app -> {
                val i = Intent(this, ActivityAboutApp::class.java)
                startActivity(i)
            }

        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun isTreasurer(): Boolean {
        val clearance = sharedP.getString(Const.CLEARANCE, "")
        return clearance == Const.POS_TREASURER || userFolioNumber == "13786"
    }

    private fun isPro(): Boolean {
        val clearance = sharedP.getString(Const.CLEARANCE, "")
        return clearance == Const.POS_PRO || userFolioNumber == "13786"
    }

}

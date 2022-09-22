@file:Suppress("KDocUnresolvedReference")

package mx.mobile.solution.nabia04_beta1.ui.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.databinding.ActivityMainBinding
import mx.mobile.solution.nabia04_beta1.fcm_.SendTokenToServerWorker
import mx.mobile.solution.nabia04_beta1.utilities.Const
import mx.mobile.solution.nabia04_beta1.utilities.ExcelHelper
import mx.mobile.solution.nabia04_beta1.utilities.SessionManager
import mx.mobile.solution.nabia04_beta1.workManager.ExcelDownloadWorker
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

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            initializeExcel()
        }

        startMils = System.currentTimeMillis()

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
                R.id.notice_board_host_frag, R.id.database_host_frag, R.id.welfare_host_frag_nav,
                R.id.fragment_main_view_nav
            ), drawerLayout
        )

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.setupWithNavController(navController, appBarConfiguration)

        userFolioNumber = sharedP.getString(SessionManager.FOLIO_NUMBER, "") ?: ""
        clearance = sharedP.getString(Const.CLEARANCE, "") ?: ""

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        scheduleSendingTokenToServer()

        scheduleExcelDownloader()
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

    private fun scheduleExcelDownloader() {
        val requestID = "scheduleExcelDownloader"
        val myWorkRequest =
            PeriodicWorkRequest.Builder(ExcelDownloadWorker::class.java, 7, TimeUnit.DAYS)
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

    private suspend fun initializeExcel() {
        withContext(Dispatchers.Default) {
            excelHelper.initialize()
        }
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

package mx.mobile.solution.nabia04.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import dagger.hilt.android.AndroidEntryPoint
import mx.mobile.solution.nabia04.R

@AndroidEntryPoint
class ActivityUpdateUserData : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    companion object {
        var selectedFolio: String = ""
        var userFolio: String = ""
        var newImageUri: String = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_treasurer_tools)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        selectedFolio = intent.getStringExtra("folio").toString()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nestedFragmentHolder) as NavHostFragment
        val navController = navHostFragment.navController

        // Get App Configuration from nav graph
        appBarConfiguration = AppBarConfiguration(navController.graph)

        // Handles arrow back button
        setupActionBarWithNavController(navController, appBarConfiguration)

    }

}
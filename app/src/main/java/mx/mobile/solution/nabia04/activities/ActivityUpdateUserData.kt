package mx.mobile.solution.nabia04.activities

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.PreferenceManager
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.room_database.MainDataBase
import mx.mobile.solution.nabia04.room_database.repositories.DatabaseRepository
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.SessionManager
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.DatabaseObject

class ActivityUpdateUserData : AppCompatActivity() {
    private var updateMode: DatabaseUpdateViewModel? = null
    private var sharedP: SharedPreferences? = null
    private lateinit var appBarConfiguration: AppBarConfiguration

    companion object {
        var selectedFolio: String = ""
        var userFolio: String = ""
        var newImageUri: String = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_data_edit1)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        updateMode = ViewModelProvider(this).get(DatabaseUpdateViewModel::class.java)

        sharedP = PreferenceManager.getDefaultSharedPreferences(this)
        selectedFolio = intent.getStringExtra("folio").toString()
        userFolio = sharedP?.getString(SessionManager.FOLIO_NUMBER, "").toString()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nestedFragmentHolder) as NavHostFragment
        val navController = navHostFragment.navController

        // Get App Configuration from nav graph
        appBarConfiguration = AppBarConfiguration(navController.graph)

        // Handles arrow back button
        setupActionBarWithNavController(navController, appBarConfiguration)

        getUserData()

    }

    private fun getUserData() {
        object : BackgroundTasks() {
            var thisUserData: DatabaseObject? = null
            override fun onPreExecute() {}
            override fun doInBackground() {
                val dao = MainDataBase.getDatabase(applicationContext).userDataDao()
                thisUserData = DatabaseRepository.getBackEndDataObject(dao.getUser(selectedFolio))
            }
            override fun onPostExecute() {
                updateMode?.setValue(thisUserData!!)

            }
        }.execute()
    }
}
package mx.mobile.solution.nabia04.authentication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.ActivityAuthenticationBinding
import mx.mobile.solution.nabia04.ui.activities.MainActivity
import mx.mobile.solution.nabia04.utilities.SessionManager
import javax.inject.Inject

@AndroidEntryPoint
class AuthenticationActivity : AppCompatActivity() {
    private var binding: ActivityAuthenticationBinding? = null

    @Inject
    lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (session.isLoggedIn) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding = ActivityAuthenticationBinding.inflate(
            layoutInflater
        )
        setContentView(binding!!.root)

        val navController = findNavController(this, R.id.nav_host_fragment_activity_authentication2)
        setupWithNavController(binding!!.navViewAuth, navController)
    }

}
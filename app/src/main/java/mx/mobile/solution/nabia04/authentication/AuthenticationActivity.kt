package mx.mobile.solution.nabia04.authentication

import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import mx.mobile.solution.nabia04.authentication.RegisterFragment.Listener
import android.os.Bundle
import mx.mobile.solution.nabia04.R
import android.content.Intent
import android.util.Log
import mx.mobile.solution.nabia04.databinding.ActivityAuthenticationBinding
import mx.mobile.solution.nabia04.main.MainActivity
import mx.mobile.solution.nabia04.utilities.SessionManager

class AuthenticationActivity : AppCompatActivity(), LoginFragment.Listener, Listener {
    private var binding: ActivityAuthenticationBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = SessionManager(applicationContext)
        if (session.isLoggedIn) {
            onFinished()
        }
        binding = ActivityAuthenticationBinding.inflate(
            layoutInflater
        )
        setContentView(binding!!.root)

        val navController = findNavController(this, R.id.nav_host_fragment_activity_authentication2)
        setupWithNavController(binding!!.navViewAuth, navController)
    }

    override fun onFinished() {
        Log.i("ACTIVITY ", "ON FINISH")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
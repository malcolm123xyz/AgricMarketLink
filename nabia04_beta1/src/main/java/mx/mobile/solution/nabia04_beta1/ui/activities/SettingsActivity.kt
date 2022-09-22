package mx.mobile.solution.nabia04_beta1.ui.activities

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.intro.IntroActivity
import mx.mobile.solution.nabia04_beta1.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04_beta1.utilities.MyAlertDialog
import mx.mobile.solution.nabia04_beta1.utilities.SessionManager
import mx.mobile.solution.nabia04_beta1.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.PasswordChangeTP
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ResponseString


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)

    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val logoutPref = findPreference<Preference>("logout")
            logoutPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                    .setMessage("LOGOUT")
                    .setTitle("Are you sure you want to logout?")
                    .setPositiveButton("YES") { dialog, _ ->
                        dialog.dismiss()
                        logoutUser()
                    }.setNegativeButton("NO") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
                true
            }

            val changePassPref = findPreference<Preference>("changePass")
            changePassPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                showPasswordChangDial()
                true
            }
        }

        private fun showPasswordChangDial() {
            val v: View = layoutInflater.inflate(R.layout.change_password_layout, null)
            val editOldPass = v.findViewById<EditText>(R.id.old_password)
            val editnewPass = v.findViewById<EditText>(R.id.new_password)
            val editRepeatNewPass = v.findViewById<EditText>(R.id.repeat_new_password)
            val buttChange = v.findViewById<Button>(R.id.change)
            val alert = AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setView(v)
                .setNegativeButton(
                    "CANCEL"
                ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .show()
            buttChange.setOnClickListener {
                Log.i("TAG", "Change button clicked")
                val oldP = editOldPass.text.toString()
                val newP = editnewPass.text.toString()
                val repeatP = editRepeatNewPass.text.toString()
                if (oldP.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Old password cannot be empty",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (newP.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "New password cannot be empty",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (repeatP.isEmpty() || repeatP != newP) {
                    Toast.makeText(requireContext(), "Password mismatch", Toast.LENGTH_SHORT).show()
                } else {
                    lifecycleScope.launch {
                        val pDial =
                            MyAlertDialog(
                                requireContext(),
                                "",
                                "Changing password. Please wait",
                                false
                            ).show()
                        val response = withContext(Dispatchers.IO) {
                            changePassword(userFolioNumber, oldP, newP)
                        }
                        pDial.dismiss()
                        if (response.status == Status.SUCCESS.toString()) {
                            alert.dismiss()
                            Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                            logoutUser()
                        } else {
                            Log.i("TAG", "Password changed failed, Error: ${response.message}")
                            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                                .setTitle("ERROR")
                                .setMessage(response.message)
                                .setCancelable(true)
                                .show()
                        }
                    }
                }
            }
        }

        private fun logoutUser() {

            requireActivity().window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )

            SessionManager.clearData()

            requireActivity().deleteDatabase("main_database")

            val i = Intent(requireContext(), IntroActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            requireActivity().finishAffinity()

            startActivity(i)
        }

        private fun changePassword(folio: String, oldP: String, newP: String): ResponseString {
            val t = PasswordChangeTP()
            t.folio = folio
            t.oldP = oldP
            t.newP = newP
            return endpoint.changePassword(t).execute()
        }
    }

}
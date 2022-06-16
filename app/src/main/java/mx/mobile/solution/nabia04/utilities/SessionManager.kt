package mx.mobile.solution.nabia04.utilities

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.LoginData

class SessionManager {
    // Shared Preferences
    private var pref: SharedPreferences

    // Shared pref mode
    var PRIVATE_MODE = 0
    
    constructor(context: Context) {
        pref = PreferenceManager.getDefaultSharedPreferences(context)
        editor = pref.edit()
    }

    /**
     * Create login session
     */
    fun createLoginSession(loginData: LoginData) {
        editor?.clear()
        editor?.putString(LOGIN_TOKEN, loginData.accessToken)
        editor?.putString(USER_FULL_NAME, loginData.fullName)
        editor?.putString(FOLIO_NUMBER, loginData.folioNumber)
        editor?.putString(USER_EMAIL, loginData.emailAddress)
        editor?.putString(USER_CONTACT, loginData.contact)
        editor?.putString(PASSWORK, loginData.password)
        editor?.putInt(Cons.SUSPENSION, loginData.suspended)
        editor?.putString(Cons.CLEARANCE, loginData.executivePosition)
        editor?.commit()
    }

    /**
     * Quick check for login
     */
    // Get Login State
    val isLoggedIn: Boolean
        get() = pref.getString(LOGIN_TOKEN, "") != ""

    companion object {
        private const val TAG = "SessionManager"
        const val USER_CONTACT = "contact"
        const val USER_FULL_NAME = "userFullName"
        const val USER_EMAIL = "userEmail"
        const val FOLIO_NUMBER = "folioNumber"
        const val PASSWORK = "password"
        const val ADMIN_STATUS = "adaminStatus"

        // Editor for Shared preferences
        private var editor: SharedPreferences.Editor? = null

        // Sharedpref file name
        private const val PREF_NAME = "AndroidHivePref"

        // All Shared Preferences Keys
        private const val IS_LOGIN = "IsLoggedIn"

        // User name (make variable public to access from outside)
        const val LOGIN_TOKEN = "login_token"

        // Email address (make variable public to access from outside)
        private const val KEY_EMAIL = "email"

//        fun triggerRebirth(context: Context) {
//            object : BackgroundTasks() {
//                val db = getDatabase(context)
//                override fun onPreExecute() {}
//                override fun doInBackground() {
//                    db!!.annDao()!!.nukeTable()
//                    db.userDataDao()!!.nukeTable()
//                }
//
//                override fun onPostExecute() {
//                    editor?.clear()
//                    editor?.commit()
//                    val packageManager = context.packageManager
//                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
//                    val componentName = intent!!.component
//                    val mainIntent = Intent.makeRestartActivityTask(componentName)
//                    context.startActivity(mainIntent)
//                    Runtime.getRuntime().exit(0)
//                }
//            }.execute()
//        }

        /**
         * Clear session details
         */
        fun clear() {
            // Clearing all data from Shared Preferences
            editor?.clear()
            editor?.commit()
        }
    }
}
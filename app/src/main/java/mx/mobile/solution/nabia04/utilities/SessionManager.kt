package mx.mobile.solution.nabia04.utilities

import android.content.SharedPreferences
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.LoginData
import javax.inject.Inject

class SessionManager @Inject constructor(var pref: SharedPreferences) {

    // Editor for Shared preferences

    init {
        editor = pref.edit()
    }

    fun clearData() {
        // Clearing all data from Shared Preferences
        editor?.clear()
        editor?.commit()
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
        editor?.putString(PASSWORD, loginData.password)
        editor?.putString(Cons.CLEARANCE, loginData.executivePosition)
        editor?.commit()
    }

    // Get Login State
    val isLoggedIn: Boolean
        get() = pref.getString(LOGIN_TOKEN, "") != ""

    companion object {
        const val USER_CONTACT = "contact"
        const val USER_FULL_NAME = "userFullName"
        const val USER_EMAIL = "userEmail"
        const val FOLIO_NUMBER = "folioNumber"
        const val PASSWORD = "password"
        const val LOGIN_TOKEN = "login_token"

        private var editor: SharedPreferences.Editor? = null

        fun clearData() {
            // Clearing all data from Shared Preferences
            editor?.clear()
            editor?.commit()
        }

    }
}
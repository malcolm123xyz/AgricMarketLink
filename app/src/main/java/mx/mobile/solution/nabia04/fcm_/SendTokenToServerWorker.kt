package mx.mobile.solution.nabia04.fcm_

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.preference.PreferenceManager
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import mx.mobile.solution.nabia04.data.entities.FcmToken
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04.ui.activities.endpoint
import mx.mobile.solution.nabia04.utilities.Const
import mx.mobile.solution.nabia04.utilities.RateLimiter
import mx.mobile.solution.nabia04.utilities.SessionManager
import mx.mobile.solution.nabia04.utilities.Status
import mx.mobile.solution.nabia04.workManager.MyCallback
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.RegistrationToken
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ResponseString
import java.io.IOException

class SendTokenToServerWorker(appContext: Context, workerParams: WorkerParameters) :
    ListenableWorker(appContext, workerParams) {

    private val TAG = "SendTokenWorker"

    private var sharedP: SharedPreferences

    init {
        sharedP = PreferenceManager.getDefaultSharedPreferences(appContext)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun startWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
            val callback: MyCallback = object : MyCallback {
                override fun onFailure(e: IOException?) {
                    e?.printStackTrace()
                    completer.set(Result.retry())
                }

                override fun onSuccess() {
                    completer.set(Result.success())
                }

                override fun onSaveToken(fcmToken: FcmToken) {
                    val gson = Gson()
                    val type = object : TypeToken<FcmToken?>() {}.type
                    val strObj = gson.toJson(fcmToken, type)
                    sharedP.edit().putString(Const.FCM_TOKEN, strObj).apply()
                    if (fcmToken.sent) {
                        Log.i(TAG, "TOKEN SENT TO SERVER SUCCESSFUL")
                        RateLimiter.reset(sharedP, "Token_Refresh")
                        onSuccess()
                    } else {
                        Log.i(TAG, "FAILED TO SEND TOKEN TO SERVER")
                        onFailure(null)
                    }
                }
            }

            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    sendTokenToServer(callback)
                }
            }
        }
    }

    private fun sendTokenToServer(callback: MyCallback?) {

        Log.i(TAG, "SENDING TOKEN TO SERVER...")
        val fullName = sharedP.getString(SessionManager.USER_FULL_NAME, "")
        val token = getFcmToken()

        if (token != null) {
            if (token.sent) {
                Log.i(TAG, "TOKEN ALREADY SENT, RETURNING...")
                callback?.onSuccess()
                return
            }
            try {
                val registrationToken = RegistrationToken()
                registrationToken.token = token.token
                registrationToken.tokenTimeStamp = token.time
                registrationToken.fullName = fullName
                registrationToken.folioNumber = userFolioNumber
                val response: ResponseString = endpoint.upDateToken(registrationToken).execute()
                val isSent = (response.status == Status.SUCCESS.toString())
                Log.i(TAG, "Token sent status: $isSent")
                token.sent = isSent
                callback?.onSaveToken(token)
            } catch (e: IOException) {
                Log.i(TAG, "FAILED TO SEND TOKEN TO SERVER DUE TO ERROR")
                e.printStackTrace()
                callback?.onFailure(e)
            }
        }
    }

    private fun getFcmToken(): FcmToken? {
        val strFcmToken = sharedP.getString(Const.FCM_TOKEN, "")
        if (strFcmToken!!.isEmpty()) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<FcmToken>() {}.type
        return gson.fromJson(strFcmToken, type)
    }

}

package mx.mobile.solution.nabia04.workManager

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.OnCompleteListener
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.FcmToken
import mx.mobile.solution.nabia04.ui.activities.MainActivity
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04.ui.activities.endpoint
import mx.mobile.solution.nabia04.utilities.Const
import mx.mobile.solution.nabia04.utilities.RateLimiter
import mx.mobile.solution.nabia04.utilities.SessionManager
import mx.mobile.solution.nabia04.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.RegistrationToken
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ResponseString
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TokenRefreshWorker(val appContext: Context, workerParams: WorkerParameters) :
    ListenableWorker(appContext, workerParams) {

    private val tag = "TokenRefreshWorker"

    private var sharedP: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(appContext)

    private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

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
                        Log.i(tag, "TOKEN SENT TO SERVER SUCCESSFUL")
                        RateLimiter.reset(sharedP, "Token_Refresh")
                        showTokenRefreshNotification(
                            System.currentTimeMillis(),
                            "Token Sent success"
                        )
                        onSuccess()
                    } else {
                        Log.i(tag, "FAILED TO SEND TOKEN TO SERVER")
                        showTokenRefreshNotification(
                            System.currentTimeMillis(),
                            "Sending token failed"
                        )
                        onFailure(null)
                    }
                }
            }

            if (shouldRefreshData()) {
                Log.i(tag, "shouldRefreshData() = true")

                showTokenRefreshNotification(
                    System.currentTimeMillis(),
                    "Limit reached: Initiating token request..."
                )

                Firebase.messaging.token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(tag, "Fetching FCM registration token failed", task.exception)
                        return@OnCompleteListener
                    }
                    // Get new FCM registration token
                    val token = task.result
                    val timeStamp = System.currentTimeMillis()
                    val fcmToken = FcmToken(token, timeStamp)

                    GlobalScope.launch {
                        withContext(Dispatchers.IO) {
                            sendTokenToServer(callback, fcmToken)
                        }
                    }

                })
            } else {
                Log.i(tag, "shouldRefreshData() = false")
            }

        }
    }

    private fun shouldRefreshData(): Boolean {
        val rateLimiter = RateLimiter(sharedP).shouldFetch("Token_Refresh", 2, TimeUnit.MINUTES)
        Log.i(tag, "RateLimiter = $rateLimiter")
        if (rateLimiter) {
            return true
        }
        if (getFcmToken() == null) {
            return true
        }

        return false
    }

    private fun sendTokenToServer(callback: MyCallback?, fcmToken: FcmToken) {

        Log.i(tag, "SENDING TOKEN TO SERVER...")

        try {
            val fullName = sharedP.getString(SessionManager.USER_FULL_NAME, "")
            val registrationToken = RegistrationToken()
            registrationToken.token = fcmToken.token
            registrationToken.tokenTimeStamp = fcmToken.time
            registrationToken.fullName = fullName
            registrationToken.folioNumber = userFolioNumber
            val response: ResponseString = endpoint.upDateToken(registrationToken).execute()
            val isSent = (response.status == Status.SUCCESS.toString())
            Log.i(tag, "Token sent status: $isSent")
            fcmToken.sent = isSent
            callback?.onSaveToken(fcmToken)
        } catch (e: IOException) {
            Log.i(tag, "FAILED TO SEND TOKEN TO SERVER DUE TO ERROR")
            e.printStackTrace()
            callback?.onFailure(e)
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

    private fun getNextCount(): Int {
        var curVal = sharedP.getInt("Int_Count", 0)
        curVal++
        sharedP.edit().putInt("Int_Count", curVal).apply()
        return curVal
    }

    private fun showTokenRefreshNotification(time: Long, content: String) {
        val notificationManager = (appContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager?)

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(applicationContext, Const.TOKEN_REFRESH_NOTIFY_CHANNEL)

        val intent1 = Intent(applicationContext, MainActivity::class.java)
        intent1.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        builder.setSmallIcon(R.drawable.logo)
        builder.setContentTitle("Token refresh")
        builder.setContentText("$content (${fd.format(Date(time))})")
        builder.priority = NotificationCompat.PRIORITY_DEFAULT
        builder.setAutoCancel(true)
        builder.setVibrate(longArrayOf(100, 100, 100, 100, 100))
        notificationManager?.notify(getNextCount(), builder.build())
        Log.i("MyAlarmManager", "Notification fired.")
    }

}

package mx.mobile.solution.nabia04.fcm_

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.work.*
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.authentication.AuthenticationActivity
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.entities.FcmToken
import mx.mobile.solution.nabia04.data.repositories.DBRepository
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04.utilities.Const
import mx.mobile.solution.nabia04.utilities.RateLimiter
import mx.mobile.solution.nabia04.workManager.AnnRefreshWorker
import mx.mobile.solution.nabia04.workManager.ExcelDownloadWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by MALCOLM on 11/3/2017.
 */

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    private lateinit var sharedP: SharedPreferences

    @Inject
    lateinit var repository: DBRepository

    override fun onCreate() {
        super.onCreate()
        sharedP = PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        val data = remoteMessage.data
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val type = data["NOTIFICATION_TYPE"] as String
            if (type == Const.NOTIFY_NEW_ANN) {
                RateLimiter.allow("Announcement")
                val heading = data["heading"] as String
                val t = data["annTyp"]?.toInt()
                val annType = if (t == 0) {
                    "General"
                } else {
                    "Event"
                }
                notifyNewAnn(annType, heading)
            } else if (type == Const.NOTIFY_DATABASE_UPDATE) {
                RateLimiter.allow("User_data")
            } else if (type == Const.NOTIFY_CLEARANCE) {
                if (data["folio"].equals(userFolioNumber)) {
                    val msg = data["msg"] as String
                    sharedP.edit().putString(Const.CLEARANCE, msg).apply();
                }
            } else if (type == Const.NEW_QUESTION) {
                val from = data["sendFrom"] ?: ""
                val area = data["area"] ?: ""
                var question = data["question"] ?: ""
                if (question.length > 40) {
                    question = question.substring(0, 40)
                }
                RateLimiter.allow("Questions")

                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    val user = getUser()
                    if (user?.employmentSector == area) {
                        notifyQuestionChange(
                            "New Question",
                            " from $from asked a question in $area",
                            "This question is related to your area of expertice: $question",
                            NotificationCompat.PRIORITY_MAX
                        )
                    } else {
                        notifyQuestionChange(
                            "New Question",
                            " from $from asked a question in $area", question,
                            NotificationCompat.PRIORITY_MAX
                        )
                    }
                }

            } else if (type == Const.NEW_QUESTION_REPLY) {
                RateLimiter.allow("Questions")
                val from = data["sendFrom"] ?: ""
                val area = data["area"] ?: ""
                var question = data["question"] ?: ""
                if (question.length > 40) {
                    question = question.substring(0, 40)
                }
                notifyQuestionChange(
                    "New Reply!",
                    "Someone has replied to $from question in $area",
                    question, NotificationCompat.PRIORITY_DEFAULT
                )
            } else if (type == Const.NOTIFY_QUESTION_CHANGE) {
                RateLimiter.allow("Questions")
            } else if (type == Const.NOTIFY_EXCEL_UPDATE) {
                scheduleExcelDownloader()
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
    }

    private suspend fun getUser(): EntityUserData? {
        return repository.getUser(userFolioNumber)
    }

    private fun scheduleExcelDownloader() {
        val myWorkRequest =
            OneTimeWorkRequest.Builder(ExcelDownloadWorker::class.java)
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

    private fun notifyQuestionChange(
        title: String,
        content: String,
        bitTxt: String,
        priority: Int
    ) {

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val notificationManager = (getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager?)

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, Const.GENERAL_CHANNEL_ID)

        val intent1 = Intent(this, AuthenticationActivity::class.java)
        intent1.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(this, 0, intent1, flag)
        builder.setSmallIcon(R.drawable.logo)
        builder.setContentTitle(title)
        builder.setContentText(content)
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(bitTxt))
        builder.priority = priority
        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(true)
        builder.setVibrate(longArrayOf(100, 100, 100, 100, 100))
        builder.setSound(alarmUri)
        notificationManager?.notify(index, builder.build())
    }

    private fun notifyNewAnn(type: String, heading: String) {

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val notificationManager = (getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager?)

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, Const.GENERAL_CHANNEL_ID)

        val intent1 = Intent(this, AuthenticationActivity::class.java)
        intent1.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(this, 0, intent1, flag)
        builder.setSmallIcon(R.drawable.logo)
        builder.setContentTitle("New Announcement")
        builder.setContentText("$type Announcement")
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(heading))
        builder.priority = NotificationCompat.PRIORITY_DEFAULT
        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(true)
        builder.setVibrate(longArrayOf(100, 100, 100, 100, 100))
        builder.setSound(alarmUri)
        notificationManager?.notify(index, builder.build())
    }

    private fun scheduleAnnouncementUpdate() {
        val myWorkRequest: WorkRequest =
            OneTimeWorkRequest.Builder(AnnRefreshWorker::class.java)
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

    override fun onNewToken(token: String) {
        Log.i(TAG, "New token received: $token")
        val timeStamp = System.currentTimeMillis()
        Log.i(TAG, "token timeStamp = $timeStamp")
        if (token.isNotEmpty()) {
            saveToken(token, timeStamp)
        }
    }

    private fun saveToken(token: String, timestamp: Long) {
        val fcmToken = FcmToken(token, timestamp)
        val gson = Gson()
        val type = object : TypeToken<FcmToken?>() {}.type
        val strObj = gson.toJson(fcmToken, type)
        sharedP.edit().putString(Const.FCM_TOKEN, strObj).apply()
        RateLimiter.reset("Token_Refresh")
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

    private val index: Int
        get() {
            var curVal = sharedP.getInt("Int_Count", 0)
            curVal++
            sharedP.edit().putInt("Int_Count", curVal).apply()
            return curVal
        }

    private val alarmUri: Uri?
        get() {
            var alert = RingtoneManager
                .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (alert == null) {
                alert = RingtoneManager
                    .getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            return alert
        }
}
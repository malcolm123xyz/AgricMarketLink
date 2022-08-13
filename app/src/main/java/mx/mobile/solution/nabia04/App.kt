package mx.mobile.solution.nabia04

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.cloudinary.android.MediaManager
import dagger.hilt.android.HiltAndroidApp
import mx.mobile.solution.nabia04.utilities.Const

@HiltAndroidApp
class App : Application() {

    init {
        instance = this
    }

    companion object {
        private var instance: App? = null
        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        MediaManager.init(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                (this.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager?)

            val channel1 = NotificationChannel(
                Const.BIRTHDAY_CHANNEL_ID, "Birthday Alarm Notification",
                NotificationManager.IMPORTANCE_HIGH
            )

            val channel2 = NotificationChannel(
                Const.EVENT_CHANNEL_ID, "Event Alarm Notification",
                NotificationManager.IMPORTANCE_HIGH
            )

            val channel3 = NotificationChannel(
                Const.TOKEN_REFRESH_NOTIFY_CHANNEL, "Token Refreshment Notification",
                NotificationManager.IMPORTANCE_HIGH
            )

            val channel4 = NotificationChannel(
                Const.GENERAL_CHANNEL_ID, "General Notification",
                NotificationManager.IMPORTANCE_HIGH
            )

            notificationManager?.createNotificationChannel(channel1)
            notificationManager?.createNotificationChannel(channel2)
            notificationManager?.createNotificationChannel(channel3)
            notificationManager?.createNotificationChannel(channel4)
        }
    }

    private fun createAlarmNotChannel() {
        val notificationManager =
            (this.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager?)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Const.EVENT_CHANNEL_ID, "Event Alarm Notification",
                NotificationManager.IMPORTANCE_HIGH
            )

            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createTokenRefreshNotChannel() {
        val notificationManager =
            (this.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager?)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Const.TOKEN_REFRESH_NOTIFY_CHANNEL, "Token Refreshment Notification",
                NotificationManager.IMPORTANCE_HIGH
            )

            notificationManager?.createNotificationChannel(channel)
        }
    }

}
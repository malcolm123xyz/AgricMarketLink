package mx.mobile.solution.nabia04

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.cloudinary.android.MediaManager
import dagger.hilt.android.HiltAndroidApp
import mx.mobile.solution.nabia04.utilities.Cons

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        MediaManager.init(this)
        createAlarmNotChannel()
        createBirthdayNotChannel()
    }

    private fun createBirthdayNotChannel() {
        val notificationManager =
            (this.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager?)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Cons.BIRTHDAY_CHANNEL_ID, "Birthday Alarm Notification",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createAlarmNotChannel (){
        val notificationManager =
            (this.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager?)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Cons.EVENT_CHANNEL_ID, "Event Alarm Notification",
                NotificationManager.IMPORTANCE_HIGH)

            notificationManager?.createNotificationChannel(channel)
        }
    }

}
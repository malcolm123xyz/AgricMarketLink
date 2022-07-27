package mx.mobile.solution.nabia04.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.MainDataBase
import mx.mobile.solution.nabia04.ui.activities.MainActivity
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.Const

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, i: Intent) {
        Log.i("MyAlarmManager", "Alarm Recieved>>>>>>>>>>>>>>>>>>>>>>>>")
        Log.i("MyAlarmManager", "Alarm ID: " + i.getIntExtra("alarmId", 0))
        if (Intent.ACTION_BOOT_COMPLETED == i.action) {
            Toast.makeText(context, "Boot complete recieved.... Nabia04", Toast.LENGTH_LONG).show()
            resetAlarms(context)
            Toast.makeText(context, "Alarms reset", Toast.LENGTH_LONG).show()
        } else {
            if( i.getStringExtra("alarmId").equals("Birthday")){
                showBirthdayNotification(context, i)
            }else {
                showEventNotification(context, i)
            }
        }
    }

    private fun resetAlarms (context: Context){
        object: BackgroundTasks(){
            override fun onPreExecute() {
            }

            override fun doInBackground() {
                val eventDoa = MainDataBase.getDatabase(context).annDao()
                val birthDayDoa = MainDataBase.getDatabase(context).dbDao()
                val myAlarmManager = MyAlarmManager(context)
                myAlarmManager.scheduleEventNotification(eventDoa.annList)
                myAlarmManager.scheduleBirthdayNotification(birthDayDoa.getAllUserData)
            }

            override fun onPostExecute() {
                TODO("Not yet implemented")
            }

        }.execute()
    }


    private fun showBirthdayNotification(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("itemId", 0).toInt()
        val bigText = """Today is:
             ${intent.getStringExtra("name")}:
            Birthday
            """.trimIndent()

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val notificationManager = (context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager?)

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, Const.BIRTHDAY_CHANNEL_ID)

        val intent1 = Intent(context, MainActivity::class.java)
        intent1.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(context, 0, intent1, flag)
        builder.setSmallIcon(R.drawable.logo)
        builder.setContentTitle("Birthday Reminder")
        builder.setContentText("Birthday notification!!!")
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        builder.priority = NotificationCompat.PRIORITY_DEFAULT
        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(true)
        builder.setVibrate(longArrayOf(100, 100, 100, 100, 100))
        builder.setSound(alarmUri)
        notificationManager?.notify(alarmId, builder.build())
        Log.i("MyAlarmManager", "Notification fired.")
    }


    private fun showEventNotification(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("itemId", 0).toInt()
        val type = intent.getStringExtra("type")
        val bigText = """The following Event is scheduled to come off on:
             ${intent.getStringExtra("eventDate")}:
            ${intent.getStringExtra("heading")}
            """.trimIndent()

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val notificationManager = (context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager?)

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, Const.EVENT_CHANNEL_ID)

        val intent1 = Intent(context, MainActivity::class.java)
        intent1.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(context, 0, intent1, flag)
        builder.setSmallIcon(R.drawable.logo)
        builder.setContentTitle("Event Reminder")
        builder.setContentText("$type Announcement")
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        builder.priority = NotificationCompat.PRIORITY_DEFAULT
        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(true)
        builder.setVibrate(longArrayOf(100, 100, 100, 100, 100))
        builder.setSound(alarmUri)
        notificationManager?.notify(alarmId, builder.build())
        Log.i("MyAlarmManager", "Notification fired with Notification ID: $alarmId")
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
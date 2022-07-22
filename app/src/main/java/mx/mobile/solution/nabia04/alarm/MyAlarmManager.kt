package mx.mobile.solution.nabia04.alarm

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.DatePicker
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyAlarmManager @Inject constructor(context: Context) {
    private var flag1: Int
    private var flag: Int
    private val context: Context

    private val annTypes =
        arrayListOf("SELECT EVENT", "Funeral", "Wedding", "Naming ceremony", "Birthday", "Reminder")

    fun showDateTimePicker(callback: CallBack) {
        val cal: Calendar = Calendar.getInstance()
        DatePickerDialog(
            context, fun(_: DatePicker, y: Int, m: Int, d: Int) {
                cal[Calendar.MONTH] = m
            cal[Calendar.YEAR] = y
            cal[Calendar.DAY_OF_MONTH] = d
            TimePickerDialog(
                context, { _, hourOfDay, minute ->
                    cal[Calendar.HOUR_OF_DAY] = hourOfDay
                    cal[Calendar.MINUTE] = minute
                    callback.done(cal.timeInMillis)
                }, cal[Calendar.HOUR_OF_DAY],
                cal[Calendar.MINUTE], false
            ).show()

        }, cal[Calendar.YEAR],
            cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]
        ).show()
    }

    fun showDayMonthPicker(callBack: CallBack) {
        val cal: Calendar = Calendar.getInstance()
        DatePickerDialog(
            context, { _, _, m, d ->
                cal[Calendar.MONTH] = m
                cal[Calendar.YEAR] = cal[Calendar.YEAR]
                cal[Calendar.DAY_OF_MONTH] = d
                callBack.done(cal.timeInMillis + (1000 * 60 * 2))
            }, cal[Calendar.YEAR],
            cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]
        ).show()
    }

    fun scheduleEventNotification(annDataObject: List<mx.mobile.solution.nabia04.data.entities.EntityAnnouncement>) {
        var numAlarms = 0
        for (eventItem: mx.mobile.solution.nabia04.data.entities.EntityAnnouncement in annDataObject) {
            val alarmTime = eventItem.eventDate;
            val currentTime = System.currentTimeMillis()
            if (alarmTime > currentTime) {
                val alarmId = eventItem.id.toString().substring(9).toInt()
                val intent = Intent(context, AlarmReceiver::class.java)
                intent.putExtra("itemId", eventItem.id)
                intent.putExtra("type", annTypes[eventItem.eventType])
                intent.putExtra("eventDate", fd.format(Date(eventItem.eventDate)))
                intent.putExtra("heading", getHeading(eventItem.heading))
                val pIntent = PendingIntent.getBroadcast(context, alarmId, intent, flag)
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pIntent)
                }

                Log.i(TAG, "Alarm set at: " + fd.format(Date(alarmTime)) + ", Alarm id: " + alarmId)
            }
        }
        if (numAlarms > 0) {
            val receiver = ComponentName(context, AlarmReceiver::class.java)
            context.packageManager.setComponentEnabledSetting(
                receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    fun scheduleBirthdayNotification(users: List<mx.mobile.solution.nabia04.data.entities.EntityUserData>) {
        for (user: mx.mobile.solution.nabia04.data.entities.EntityUserData in users) {
            val alarmTime = user.birthDayAlarm;
            val currentTime = System.currentTimeMillis()
            if (alarmTime > currentTime) {
                Log.i(TAG, "user name: " + user.fullName)
                val alarmId = user.folioNumber.substring(3).toInt()
                Log.i(TAG, "user folio: " + user.folioNumber)
                val intent = Intent(context, AlarmReceiver::class.java)
                intent.putExtra("itemId", user.folioNumber)
                intent.putExtra("type", "Birthday")
                intent.putExtra("name", user.fullName)
                val pIntent = PendingIntent.getBroadcast(context, alarmId, intent, flag)
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager[AlarmManager.RTC_WAKEUP, alarmTime] = pIntent
                Log.i(TAG, "Alarm set at: " + fd.format(Date(alarmTime)) + ", Alarm id: " + alarmId)
            }
        }
    }

    private fun getHeading(s: String): String {
        if (s.length > 100) {
            return s.substring(0, 100)+"..."
        }
        return s
    }

    fun cancel(ann: mx.mobile.solution.nabia04.data.entities.EntityAnnouncement) {
        val alarmId = ann.id.toString().substring(9).toInt()
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra("itemId", ann.id)
        intent.putExtra("type", annTypes[ann.eventType])
        intent.putExtra("eventDate", fd.format(Date(ann.eventDate)))
        intent.putExtra("heading", getHeading(ann.heading))
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val pendingIntent = PendingIntent.getService(context, alarmId, intent, flag)
        if (pendingIntent != null && alarmManager != null) {
            Log.i(TAG, "Alarm canceled for : " + ann.heading)
            alarmManager.cancel(pendingIntent)
        }else{
            Log.i(TAG, "Alarm not found for : " +alarmId)
        }
    }

    interface CallBack {
        fun done(alarmTime: Long)
    }

    companion object {
        private const val TAG = "MyAlarmManager"
        private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)
    }

    init {
        this.context = context
        flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        flag1 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }
}
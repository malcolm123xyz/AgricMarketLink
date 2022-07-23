package mx.mobile.solution.nabia04.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import mx.mobile.solution.nabia04.data.MainDataBase
import mx.mobile.solution.nabia04.utilities.BackgroundTasks

class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, i: Intent) {
        Log.i("MyAlarmManager", "BootComplete Recieved>>>>>>>>>>>>>>>>>>>>>>>>")
        if (Intent.ACTION_BOOT_COMPLETED == i.action) {
            Toast.makeText(context, "Boot complete recieved.... Nabia04", Toast.LENGTH_LONG).show()
            resetAlarms(context)
            Toast.makeText(context, "Alarms reset", Toast.LENGTH_LONG).show()
        }
    }

    private fun resetAlarms(context: Context) {
        object : BackgroundTasks() {
            override fun onPreExecute() {}

            override fun doInBackground() {
                val eventDoa = MainDataBase.getDatabase(context).annDao()
                val birthDayDoa = MainDataBase.getDatabase(context).dbDao()
                val myAlarmManager = MyAlarmManager(context)
                myAlarmManager.scheduleEventNotification(eventDoa.annList)
                myAlarmManager.scheduleBirthdayNotification(birthDayDoa.getAllUserData)
            }

            override fun onPostExecute() {}

        }.execute()
    }

}
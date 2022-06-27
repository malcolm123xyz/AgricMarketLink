package mx.mobile.solution.nabia04.room_database.repositories

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.preference.PreferenceManager
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.extensions.android.json.AndroidJsonFactory
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.alarm.MyAlarmManager
import mx.mobile.solution.nabia04.main.MainActivity.Companion.annViewModel
import mx.mobile.solution.nabia04.main.MainActivity.Companion.annloadingStatus
import mx.mobile.solution.nabia04.main.MainActivity.Companion.sharedP
import mx.mobile.solution.nabia04.room_database.AnnDao
import mx.mobile.solution.nabia04.room_database.MainDataBase
import mx.mobile.solution.nabia04.room_database.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.room_database.view_models.State
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.Cons
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.Announcement
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.AnnouncementResponse
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class AnnDataRepository(private val context: Context?) {
    private val alarmManager: MyAlarmManager

    private fun showErrorDialog(errMsg: String) {
        AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle)
            .setTitle("ERROR")
            .setMessage("Could not load information: $errMsg")
            .setPositiveButton(
                "OK"
            ) { dialog: DialogInterface, id: Int -> dialog.dismiss() }.show()
    }

    fun delete(ann: EntityAnnouncement) {
        object: BackgroundTasks(){
            override fun onPreExecute() {}

            override fun doInBackground() {
                dao?.delete(ann.id)
                alarmManager.cancel(ann)
                allAnnouncements?.remove(ann)
            }

            override fun onPostExecute() {
                annViewModel.setData(allAnnouncements)
            }
        }.execute()
    }

    fun getAnn(id: Long): EntityAnnouncement {
        return dao!!.getAnnouncement(id)
    }

    fun setAnnAsRead(ann: EntityAnnouncement) {
        object: BackgroundTasks(){
            override fun onPreExecute() {}

            override fun doInBackground() {
                ann.isRead = true
                dao?.insertAnnouncement(ann)
                allAnnouncements = dao?.annList
            }

            override fun onPostExecute() {
                annViewModel.setData(allAnnouncements)
            }
        }.execute()
    }

    fun reloadFromLocalDB() {
        object : BackgroundTasks() {
            override fun onPreExecute() {
                annloadingStatus.setValue(
                    State(
                        true
                    )
                )
            }

            override fun doInBackground() {
                allAnnouncements = dao?.annList
            }

            override fun onPostExecute() {
                annloadingStatus.setValue(
                    State(
                        false
                    )
                )
                annViewModel.setData(allAnnouncements)
            }
        }.execute()
    }

    fun reloadFromBackend() {
        object : BackgroundTasks() {
            private var response = AnnouncementResponse()

            override fun onPreExecute() {}

            override fun doInBackground() {
                try {
                    endpoint = endpointObject
                    response = endpoint!!.noticeBoardData.execute()
                    if (response.returnCode == 1) {
                        allAnnouncements = getAnnDataObjects(response.announcements)
                        dao?.insertAnnouncement(allAnnouncements)
                        sharedP.edit()?.putBoolean(Cons.ANN_REFRESH, false)?.apply()
                        val refreshTimeStamp = System.currentTimeMillis()
                        sharedP.edit()?.putLong(Cons.ANN_REFRESH_TIME_STAMP, refreshTimeStamp)
                            ?.apply()
                        alarmManager.scheduleEventNotification(allAnnouncements!!)
                    }
                } catch (ex: IOException) {
                    if (ex is SocketTimeoutException ||
                        ex is SSLHandshakeException ||
                        ex is UnknownHostException
                    ) {
                        response.returnCode = 0
                        response.response = "Cause: NO INTERNET CONNECTION"
                    } else { response.returnCode = 0
                        response.response = "UNKNOWN ERROR"
                    }
                    ex.printStackTrace()
                }
            }

            override fun onPostExecute() {
                annloadingStatus.setValue(
                    State(
                        false
                    )
                )
                when (response.returnCode) {
                    1 -> {
                        annViewModel.setData(allAnnouncements)
                    }
                    else -> {
                        showErrorDialog(response.response)
                    }
                }
            }
        }.execute()
    }

    fun loadData(){
        object: BackgroundTasks(){
            var b: Boolean = false
            override fun onPreExecute() {}

            override fun doInBackground() {
                b = dao?.tableCount()!! < 1
            }

            override fun onPostExecute() {
                if (b){
                    reloadFromBackend()
                }else {
                    reloadFromLocalDB()
                }
            }
        }.execute()
    }

    companion object {
        private const val TAG = "AnnDataRepository"
        private var allAnnouncements: MutableList<EntityAnnouncement>? = null
        private var dao: AnnDao? = null
        private var endpoint: MainEndpoint? = null

        @Volatile
        private var INSTANCE: AnnDataRepository? = null
        fun getInstance(context: Context?): AnnDataRepository? {
            if (INSTANCE == null) {
                synchronized(AnnDataRepository::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = AnnDataRepository(context)
                    }
                }
            }
            return INSTANCE
        }

        fun getAnnDataObjects(list: List<Announcement>): MutableList<EntityAnnouncement>? {
            val entityAnn: MutableList<EntityAnnouncement> = ArrayList()
            for (ann in list) {
                val entity = EntityAnnouncement()
                entity.id = ann.id
                entity.heading = ann.heading
                entity.message = ann.message
                entity.type = ann.type
                entity.imageUri = ann.imageUri
                entity.eventDate = ann.eventDate
                entity.priority = ann.priority
                entity.rowNum = ann.rowNum
                entity.venue = ann.venue
                entity.isAboutWho = ann.isAboutWho
                entity.isArelative = ann.arelative
                entityAnn.add(entity)
            }
            return entityAnn
        }

        val endpointObject: MainEndpoint?
            get() {
                if (endpoint == null) {
                    val builder = MainEndpoint.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        AndroidJsonFactory(), null
                    )
                        .setRootUrl(Cons.ROOT_URL)
                    endpoint = builder.build()
                }
                return endpoint
            }
    }

    init {
        dao = MainDataBase.getDatabase(context).annDao()
        sharedP = PreferenceManager.getDefaultSharedPreferences(
            context
        )
        alarmManager = MyAlarmManager(context!!)
    }
}
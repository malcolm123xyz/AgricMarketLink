package mx.mobile.solution.nabia04.room_database.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mx.mobile.solution.nabia04.main.MainActivity.Companion.endpoint
import mx.mobile.solution.nabia04.main.MainActivity.Companion.sharedP
import mx.mobile.solution.nabia04.room_database.AnnDao
import mx.mobile.solution.nabia04.room_database.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.Cons
import mx.mobile.solution.nabia04.utilities.temporal.Resource
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.Announcement
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.AnnouncementResponse
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

class AnnRepository @Inject constructor(private var dao: AnnDao) {

    fun loadAnn(): LiveData<Resource<List<EntityAnnouncement>>> {
        val result = MutableLiveData<Resource<List<EntityAnnouncement>>>()

        result.postValue(Resource.loading(null))

        object : BackgroundTasks() {
            var response = AnnouncementResponse()
            lateinit var allAnnouncements: List<EntityAnnouncement>
            var errorMsg = ""
            override fun onPreExecute() {}

            override fun doInBackground() {
                try {

                    val annList = dao.annList
                    if (annList.size > 0) {
                        allAnnouncements = annList
                        return
                    }

                    response = endpoint!!.noticeBoardData.execute()
                    if (response.announcements != null) {
                        allAnnouncements = getAnnDataObjects(response.announcements).toList()
                        dao.insertAnnouncement(allAnnouncements)
                        sharedP.edit()?.putBoolean(Cons.ANN_REFRESH, false)?.apply()
                        sharedP.edit()
                            ?.putLong(Cons.ANN_REFRESH_TIME_STAMP, System.currentTimeMillis())
                            ?.apply()
                        //alarmManager.scheduleEventNotification(allAnnouncements)
                    }
                } catch (ex: IOException) {
                    response.returnCode = Cons.UNKNOWN_ERROR_CODE
                    if (ex is SocketTimeoutException || ex is SSLHandshakeException || ex is UnknownHostException) {
                        response.response = "Cause: NO INTERNET CONNECTION"
                    } else {
                        response.response = ex.localizedMessage
                    }
                    ex.printStackTrace()
                }
            }

            override fun onPostExecute() {
                when (response.returnCode) {
                    1 -> {
                        result.postValue(Resource.success(allAnnouncements))
                    }
                    else -> {
                        result.postValue(Resource.error(response.response, null))
                    }
                }
            }
        }.execute()

        return result
    }

    fun getAnnDataObjects(list: List<Announcement>): MutableList<EntityAnnouncement> {
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

}
package mx.mobile.solution.nabia04_beta1.data.repositories

import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04_beta1.alarm.MyAlarmManager
import mx.mobile.solution.nabia04_beta1.data.dao.AnnDao
import mx.mobile.solution.nabia04_beta1.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04_beta1.ui.activities.endpoint
import mx.mobile.solution.nabia04_beta1.utilities.RateLimiter
import mx.mobile.solution.nabia04_beta1.utilities.Response
import mx.mobile.solution.nabia04_beta1.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.Announcement
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLHandshakeException

@Singleton
class AnnRepository @Inject constructor(
    var dao: AnnDao,
    var sharedP: SharedPreferences,
    var alarmManager: MyAlarmManager
) {

    suspend fun refreshDB(): Response<List<EntityAnnouncement>> {
        return withContext(Dispatchers.IO) {
            refresh()
        }
    }

    suspend fun fetchAnn(): Response<List<EntityAnnouncement>> {
        return withContext(Dispatchers.IO) {
            fetch()
        }
    }

    suspend fun deleteFrmServer(id: Long): Int {
        return withContext(Dispatchers.IO) {
            doDeleteFrmServer(id)
        }
    }

    suspend fun getAnn(id: Long): EntityAnnouncement? {
        return dao.getAnnouncement(id)
    }

    private fun doDeleteFrmServer(id: Long): Int {

        try {
            val ret = endpoint.deleteFromServer(id).execute()
            if (ret != null) {
                return 1
            }
            return -1
        } catch (ex: IOException) {
            ex.printStackTrace()
            return 0
        }
    }

    private fun refresh(): Response<List<EntityAnnouncement>> {
        val erMsg: String
        try {
            val backendResponse = endpoint.noticeBoardData.execute()
            if (backendResponse.status == Status.SUCCESS.toString()) {
                val allAnnouncements = getAnnDataObjects(backendResponse.data).toList()
                dao.insertAnnouncement(allAnnouncements)
                RateLimiter.reset("Announcement")
                alarmManager.scheduleEventNotification(allAnnouncements)
                return Response.success(allAnnouncements)
            } else {
                return Response.error(backendResponse.message, null)
            }

        } catch (ex: IOException) {
            erMsg = if (ex is SocketTimeoutException ||
                ex is SSLHandshakeException ||
                ex is UnknownHostException
            ) {
                "Cause: NO INTERNET CONNECTION"
            } else {
                ex.localizedMessage ?: ""
            }
            ex.printStackTrace()
        }
        return Response.error(erMsg, null)
    }

    private fun fetch(): Response<List<EntityAnnouncement>> {
        val erMsg: String

        val annList = dao.annList

        if (!shouldFetch(annList)) {
            return Response.success(annList)
        }

        try {
            val backendResponse = endpoint.noticeBoardData.execute()
            return if (backendResponse.status == Status.SUCCESS.toString()) {
                val allAnnouncements = getAnnDataObjects(backendResponse.data).toList()
                dao.insertAnnouncement(allAnnouncements)
                RateLimiter.reset("Announcement")
                alarmManager.scheduleEventNotification(allAnnouncements)
                Response.success(allAnnouncements)
            } else {
                Response.error(backendResponse.message, null)
            }

        } catch (ex: IOException) {
            erMsg = if (ex is SocketTimeoutException ||
                ex is SSLHandshakeException ||
                ex is UnknownHostException
            ) {
                "Cause: NO INTERNET CONNECTION"
            } else {
                ex.localizedMessage ?: ""
            }
            ex.printStackTrace()
        }
        return Response.error(erMsg, annList)
    }

    private fun getAnnDataObjects(list: List<Announcement>): MutableList<EntityAnnouncement> {
        val entityAnn: MutableList<EntityAnnouncement> = ArrayList()
        for (ann in list) {
            val entity =
                EntityAnnouncement()
            entity.id = ann.id
            entity.heading = ann.heading
            entity.message = ann.message
            entity.annType = ann.annType
            entity.eventType = ann.eventType
            entity.imageUri = ann.imageUri
            entity.eventDate = ann.eventDate
            entity.priority = ann.priority
            entity.rowNum = ann.rowNum
            entity.venue = ann.venue
            entityAnn.add(entity)
        }
        return entityAnn
    }

    suspend fun setAnnRead(ann: EntityAnnouncement) {
        dao.updateAnnouncement(ann)
    }

    suspend fun delete(announcement: EntityAnnouncement): Int {
        return dao.delete(announcement)
    }

    private fun shouldFetch(data: List<EntityAnnouncement>): Boolean {
        if (RateLimiter.shouldFetch("Announcement", 12, TimeUnit.HOURS)) {
            Log.i("TAG", "Time limit reached, ShouldFetch Announcement data")
            return true
        }

        if (data.isEmpty()) {
            Log.i("TAG", "Data is empty, ShouldFetch Announcement data")
            return true
        }

        Log.i("TAG", "Don't fetch new Announcement data")
        return false
    }

}
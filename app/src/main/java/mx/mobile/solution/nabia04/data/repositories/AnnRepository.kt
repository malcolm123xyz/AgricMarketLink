package mx.mobile.solution.nabia04.data.repositories

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.data.dao.AnnDao
import mx.mobile.solution.nabia04.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.utilities.Cons
import mx.mobile.solution.nabia04.utilities.Resource
import mx.mobile.solution.nabia04.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.Announcement
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLHandshakeException

@Singleton
class AnnRepository @Inject constructor(
    var dao: AnnDao, var endpoint: MainEndpoint,
    var sharedP: SharedPreferences
) {

    suspend fun refreshDB(): Resource<List<EntityAnnouncement>> {
        return withContext(Dispatchers.IO) {
            refresh()
        }
    }

    suspend fun fetchAnn(): Resource<List<EntityAnnouncement>> {
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

    private fun refresh(): Resource<List<EntityAnnouncement>> {
        var erMsg = ""
        try {
            val backendResponse = endpoint.noticeBoardData.execute()
            if (backendResponse.status == Status.SUCCESS.toString()) {
                val allAnnouncements = getAnnDataObjects(backendResponse.data).toList()
                dao.insertAnnouncement(allAnnouncements)
                sharedP.edit()?.putBoolean(Cons.ANN_REFRESH, false)?.apply()
                sharedP.edit()
                    ?.putLong(Cons.ANN_REFRESH_TIME_STAMP, System.currentTimeMillis())
                    ?.apply()
                //alarmManager.scheduleEventNotification(allAnnouncements)
                return Resource.success(allAnnouncements)
            } else {
                return Resource.error(backendResponse.message, null)
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
        return Resource.error(erMsg, null)
    }

    private fun fetch(): Resource<List<EntityAnnouncement>> {
        var erMsg = ""
        val annList = dao.annList
        if (annList.isNotEmpty()) {
            return Resource.success(annList)
        }
        try {
            val backendResponse = endpoint.noticeBoardData.execute()
            if (backendResponse.status == Status.SUCCESS.toString()) {
                val allAnnouncements = getAnnDataObjects(backendResponse.data).toList()
                dao.insertAnnouncement(allAnnouncements)
                sharedP.edit()?.putBoolean(Cons.ANN_REFRESH, false)?.apply()
                sharedP.edit()
                    ?.putLong(Cons.ANN_REFRESH_TIME_STAMP, System.currentTimeMillis())
                    ?.apply()
                //alarmManager.scheduleEventNotification(allAnnouncements)
                return Resource.success(allAnnouncements)
            } else {
                return Resource.error(backendResponse.message, null)
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
        return Resource.error(erMsg, null)
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
            entity.isAboutWho = ann.isAboutWho
            entity.isArelative = ann.arelative
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

}
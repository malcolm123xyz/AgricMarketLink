package mx.mobile.solution.nabia04.data.repositories

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.data.dao.DBdao
import mx.mobile.solution.nabia04.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.utilities.Cons
import mx.mobile.solution.nabia04.utilities.Resource
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.DatabaseObject
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLHandshakeException

@Singleton
class DBRepository @Inject constructor(
    var dao: DBdao,
    var endpoint: MainEndpoint,
    var sharedP: SharedPreferences
) {

    val result = MutableLiveData<Resource<List<EntityUserData>>>()


    suspend fun fetchUserData(): Resource<List<EntityUserData>> {
        return withContext(Dispatchers.IO) {
            fetch()
        }
    }

    suspend fun refreshDB(): Resource<List<EntityUserData>> {
        return withContext(Dispatchers.IO) {
            refresh()
        }
    }

    suspend fun deleteFrmServer(id: Long): Int {
        return withContext(Dispatchers.IO) {
            doDeleteFrmServer(id)
        }
    }

    suspend fun getUser(folio: String): EntityUserData? {
        return dao.userData(folio)
    }

    fun getEntity(list: List<DatabaseObject>): List<EntityUserData> {
        val entityUserDataList: MutableList<EntityUserData> =
            ArrayList()
        for (obj in list) {
            val u =
                EntityUserData()
            u.birthDayAlarm = obj.birthDayAlarm
            u.className = obj.className
            u.contact = obj.contact
            u.courseStudied = obj.courseStudied
            u.districtOfResidence = obj.districtOfResidence
            u.email = obj.email
            u.folioNumber = obj.folioNumber
            u.homeTown = obj.homeTown
            u.house = obj.house
            u.imageId = obj.imageId
            u.imageUri = obj.imageUri
            u.nickName = obj.nickName
            u.jobDescription = obj.jobDescription
            u.specificOrg = obj.specificOrg
            u.employmentStatus = obj.employmentStatus
            u.employmentSector = obj.employmentSector
            u.nameOfEstablishment = obj.nameOfEstablishment
            u.establishmentRegion = obj.establishmentRegion
            u.establishmentDist = obj.establishmentDist
            u.positionHeld = obj.positionHeld
            u.regionOfResidence = obj.regionOfResidence
            u.sex = obj.sex
            u.fullName = obj.fullName
            u.survivingStatus = obj.survivingStatus
            u.dateDeparted = obj.dateDeparted
            u.biography = obj.biography
            u.tribute = obj.tributes
            entityUserDataList.add(u)
        }
        return entityUserDataList
    }

    suspend fun setAnnRead(ann: EntityAnnouncement) {
        dao.updateAnnouncement(ann)
    }

    suspend fun delete(announcement: EntityAnnouncement): Int {
        return dao.delete(announcement)
    }

    suspend fun setUserClearance(folio: String, clearance: String): Resource<List<EntityUserData>> {
        return withContext(Dispatchers.IO) {
            doSetClearance(folio, clearance)
        }

    }

    private fun doSetClearance(folio: String, clearance: String): Resource<List<EntityUserData>> {
        var erMsg = ""
        try {
            val response = endpoint.setUserClearance(folio, clearance)?.execute()
            if (response?.returnCode == 1) {
                return Resource.success(null)
            }
            return Resource.error(response?.response.toString(), null)
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

    private fun refresh(): Resource<List<EntityUserData>> {
        var erMsg = ""
        try {
            val backendResponse = endpoint.members.execute()
            Log.i("TAG", "backendResponse: ${backendResponse.returnCode}")
            if (backendResponse.returnCode == 1) {
                if (backendResponse.databaseObj != null) {
                    val allData = getEntity(backendResponse.databaseObj).toList()
                    dao.insert(allData)
                    sharedP.edit()?.putBoolean(Cons.DATABASE_REFRESH, false)?.apply()
                    //alarmManager.scheduleBirthdayNotification(allUserData!!)
                    return Resource.success(allData)
                }
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

    private fun fetch(): Resource<List<EntityUserData>> {
        var erMsg = ""
        val list = dao.getAllUserData
        if (list.isNotEmpty()) {
            return Resource.success(list)
        }
        try {
            val backendResponse = endpoint.members.execute()
            if (backendResponse.returnCode == 1) {
                if (backendResponse.databaseObj != null) {
                    val allData = getEntity(backendResponse.databaseObj).toList()
                    dao.insert(allData)
                    sharedP.edit()?.putBoolean(Cons.DATABASE_REFRESH, false)?.apply()
                    //alarmManager.scheduleBirthdayNotification(allUserData!!)
                    return Resource.success(allData)
                }
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

    private fun doSetDeceased(
        date: String,
        folio: String,
        status: Int
    ): Resource<List<EntityUserData>> {
        var erMsg = ""
        try {
            val response = endpoint.setDeceaseStatus(date, folio, status)?.execute()
            if (response?.returnCode == 1) {
                return Resource.success(null)
            }
            return Resource.error(response?.returnMsg.toString(), null)
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

    private fun doSetDeleteUser(folio: String): Resource<List<EntityUserData>> {
        var erMsg = ""
        try {
            val response = endpoint.deleteUser(folio)?.execute()
            if (response?.returnCode == 1) {
                return Resource.success(null)
            }
            return Resource.error(response?.returnMsg.toString(), null)
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

    private fun doSetBiography(biography: String, folio: String): Resource<List<EntityUserData>> {
        var erMsg = ""
        try {
            val response = endpoint.deleteUser(folio)?.execute()
            if (response?.returnCode == 1) {
                return Resource.success(null)
            }
            return Resource.error(response?.returnMsg.toString(), null)
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

    private fun doSendTribute(folio: String, tribute: String): Resource<List<EntityUserData>> {
        val erMsg = ""
        try {
            val response = endpoint.addTribute(folio, tribute)?.execute()
            if (response?.returnCode == 1) {
                return Resource.success(null)
            }
            return Resource.error(response?.response.toString(), null)
        } catch (ex: IOException) {
            ex.printStackTrace()
            if (ex is SocketTimeoutException || ex is SSLHandshakeException || ex is UnknownHostException) {
                Response().setResponse("Cause: NO INTERNET CONNECTION").setReturnCode(0)
            } else {
                Response().setResponse("UNKNOWN ERROR").setReturnCode(0)
            }
        }
        return Resource.error(erMsg, null)
    }

    suspend fun setDeceaseStatus(
        folio: String,
        date: String,
        status: Int
    ): Resource<List<EntityUserData>> {
        return withContext(Dispatchers.IO) {
            doSetDeceased(folio, date, status)
        }
    }

    suspend fun deleteUser(folio: String): Resource<List<EntityUserData>> {
        return withContext(Dispatchers.IO) {
            doSetDeleteUser(folio)
        }
    }

    suspend fun setBiography(
        biography: String,
        selectedFolio: String
    ): Resource<List<EntityUserData>> {
        return withContext(Dispatchers.IO) {
            doSetBiography(biography, selectedFolio)
        }
    }

    suspend fun sendTribute(
        selectedFolio: String,
        tribute: String
    ): Resource<List<EntityUserData>> {
        return withContext(Dispatchers.IO) {
            doSendTribute(selectedFolio, tribute)
        }
    }

}
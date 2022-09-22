package mx.mobile.solution.nabia04_beta1.data.repositories

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04_beta1.alarm.MyAlarmManager
import mx.mobile.solution.nabia04_beta1.data.dao.DBdao
import mx.mobile.solution.nabia04_beta1.data.entities.EntityUserData
import mx.mobile.solution.nabia04_beta1.ui.activities.ActivityUpdateUserData
import mx.mobile.solution.nabia04_beta1.ui.activities.ActivityUpdateUserData.Companion.selectedFolio
import mx.mobile.solution.nabia04_beta1.ui.activities.endpoint
import mx.mobile.solution.nabia04_beta1.utilities.RateLimiter
import mx.mobile.solution.nabia04_beta1.utilities.Response
import mx.mobile.solution.nabia04_beta1.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLHandshakeException

@Singleton
class DBRepository @Inject constructor(
    var dao: DBdao,
    var sharedP: SharedPreferences,
    var alarmManager: MyAlarmManager
) {

    suspend fun fetchUserData(): Response<List<EntityUserData>> {
        return withContext(Dispatchers.IO) {
            fetch()
        }
    }

    suspend fun fetchUserNames(): List<EntityUserData>? {
        return withContext(Dispatchers.IO) {
            return@withContext fetch().data
        }

    }

    suspend fun refreshDB(): Response<List<EntityUserData>> {
        return withContext(Dispatchers.IO) {
            refresh()
        }
    }

    suspend fun upDateUserData(
        data: MutableLiveData<Response<String>>,
        userData: EntityUserData,
        newImageUri: String
    ) {

        return withContext(Dispatchers.IO) {
            if (newImageUri.isEmpty()) {
                sendToDataStore(data, userData)
            } else {
                sendImageFirst(data, userData)
            }
        }
    }

    private fun sendImageFirst(
        data: MutableLiveData<Response<String>>,
        userData: EntityUserData,
    ) {
        data.postValue(Response.loading("Sending Image to server..."))

        val storage = Firebase.storage
        val storageRef = storage.reference
        val userIconRef = storageRef.child("database/$selectedFolio.jpg")
        val stream = FileInputStream(File(ActivityUpdateUserData.newImageUri))
        val uploadTask = userIconRef.putStream(stream)
        uploadTask.addOnFailureListener {
            it.printStackTrace()
        }.addOnSuccessListener { taskSnapshot ->
            Log.i("TAG", "Image uploaded: $taskSnapshot")
        }

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            userIconRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                Log.i("TAG", "downloadUri: $downloadUri")
                userData.imageUri = downloadUri.toString()
                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch { sendToDataStore(data, userData) }
            } else {
                data.postValue(Response.error("Failed to send Image. Please try again", ""))
                task.exception?.printStackTrace()
            }
        }
    }

    private fun sendToDataStore(
        data: MutableLiveData<Response<String>>,
        userData: EntityUserData
    ) {
        var erMsg = ""
        try {
            data.postValue(Response.loading("Sending data to server..."))
            val response = if (selectedFolio.isEmpty()) {
                endpoint.addNewMember(getBackendModel(userData)).execute()
            } else {
                endpoint.insertDataModel(getBackendModel(userData)).execute()
            }
            if (response?.status == Status.SUCCESS.toString()) {
                data.postValue(Response.success("Done"))
            } else {
                data.postValue(response?.message?.let { Response.error(it, "") })
            }

        } catch (ex: IOException) {
            erMsg = if (ex is SocketTimeoutException || ex is SSLHandshakeException ||
                ex is UnknownHostException
            ) {
                "Cause: NO INTERNET CONNECTION"
            } else {
                ex.localizedMessage ?: ""
            }
            ex.printStackTrace()
            data.postValue(Response.error(erMsg, ""))
        }
    }


    suspend fun getUser(folio: String): EntityUserData? {
        return dao.userData(folio)
    }

    private fun getEntity(list: List<DatabaseObject>): List<EntityUserData> {
        val entityUserDataList: MutableList<EntityUserData> =
            ArrayList()
        for (obj in list) {
            val u =
                EntityUserData()
            u.birthDayAlarm = obj.birthDayAlarm ?: 0L
            u.className = obj.className ?: ""
            u.contact = obj.contact ?: ""
            u.courseStudied = obj.courseStudied ?: ""
            u.districtOfResidence = obj.districtOfResidence ?: ""
            u.email = obj.email ?: ""
            u.folioNumber = obj.folioNumber ?: ""
            u.homeTown = obj.homeTown ?: ""
            u.house = obj.house ?: ""
            u.imageId = obj.imageId ?: ""
            u.imageUri = obj.imageUri ?: ""
            u.nickName = obj.nickName ?: ""
            u.jobDescription = obj.jobDescription ?: ""
            u.specificOrg = obj.specificOrg ?: ""
            u.employmentStatus = obj.employmentStatus ?: ""
            u.employmentSector = obj.employmentSector ?: ""
            u.nameOfEstablishment = obj.nameOfEstablishment ?: ""
            u.establishmentRegion = obj.establishmentRegion ?: ""
            u.establishmentDist = obj.establishmentDist ?: ""
            u.positionHeld = obj.positionHeld ?: ""
            u.regionOfResidence = obj.regionOfResidence ?: ""
            u.sex = obj.sex ?: ""
            u.fullName = obj.fullName ?: ""
            u.survivingStatus = obj.survivingStatus ?: 0
            u.dateDeparted = obj.dateDeparted ?: ""
            u.biography = obj.biography ?: ""
            u.tributes = obj.tributes ?: ""
            entityUserDataList.add(u)
        }
        return entityUserDataList
    }

    suspend fun setUserClearance(folio: String, clearance: String): Response<List<EntityUserData>> {
        return withContext(Dispatchers.IO) {
            doSetClearance(folio, clearance)
        }

    }

    private fun doSetClearance(folio: String, clearance: String): Response<List<EntityUserData>> {
        val erMsg: String
        try {
            val cl = ClearanceTP().setFolio(folio).setPosition(clearance)
            val response = endpoint.setUserClearance(cl)?.execute()
            if (response?.status == Status.SUCCESS.toString()) {
                return Response.success(null)
            }
            return Response.error(response?.message.toString(), null)
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

    private fun refresh(): Response<List<EntityUserData>> {
        val erMsg: String
        try {
            val response = endpoint.members.execute()
            return if (response?.status == Status.SUCCESS.toString()) {
                val allData = getEntity(response.data).toList()
                dao.insert(allData)
                RateLimiter.reset("User_data")
                alarmManager.scheduleBirthdayNotification(allData)
                Response.success(allData)
            } else {
                Response.error(response.message, null)
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

    private fun fetch(): Response<List<EntityUserData>> {
        var erMsg = ""

        val list = dao.getAllUserData

        if (!shouldFetch(list)) {
            return Response.success(list)
        }

        try {

            val response = endpoint.members.execute()
            return if (response?.status == Status.SUCCESS.toString()) {
                val allData = getEntity(response.data).toList()
                dao.insert(allData)
                RateLimiter.reset("User_data")
                alarmManager.scheduleBirthdayNotification(allData)
                Response.success(allData)
            } else {
                Response.error(response.message, null)
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
        return Response.error(erMsg, list)
    }


    private fun doSetDeceased(
        folio: String,
        date: String,
        status: Int
    ): Response<List<EntityUserData>> {
        var erMsg = ""
        try {
            val deceaseStatus = DeceaseStatusTP().setDate(date).setFolio(folio).setStatus(status)
            Log.i("TAG", "Deceased folio num: = $folio")
            val response = endpoint.setDeceaseStatus(deceaseStatus)?.execute()
            if (response?.status == Status.SUCCESS.toString()) {
                return Response.success(null)
            }
            return Response.error(response?.message.toString(), null)
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

    private fun doSetDeleteUser(folio: String): Response<List<EntityUserData>> {
        var erMsg = ""
        try {
            val response = endpoint.deleteUser(folio)?.execute()
            if (response?.status == Status.SUCCESS.toString()) {
                return Response.success(null)
            }
            return Response.error(response?.message.toString(), null)
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

    private fun doSetBiography(biography: String, folio: String): Response<List<EntityUserData>> {
        var erMsg = ""
        try {
            val bio = BiographyTP().setFolio(folio).setBio(biography)
            val response = endpoint.setBiography(bio)?.execute()
            if (response?.status == Status.SUCCESS.toString()) {
                return Response.success(null)
            }
            return Response.error(response?.message.toString(), null)
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

    private fun doSendTribute(folio: String, tribute: String): Response<List<EntityUserData>> {
        var erMsg = ""
        try {
            val tributeTP = TributeTP().setFolio(folio).setMsg(tribute)
            val response = endpoint.addTribute(tributeTP)?.execute()
            if (response?.status == Status.SUCCESS.toString()) {
                return Response.success(null)
            }
            return Response.error(response?.message.toString(), null)
        } catch (ex: IOException) {
            ex.printStackTrace()
            erMsg =
                if (ex is SocketTimeoutException || ex is SSLHandshakeException || ex is UnknownHostException) {
                    "Cause: NO INTERNET CONNECTION"
                } else {
                    "UNKNOWN ERROR"
                }
        }
        return Response.error(erMsg, null)
    }

    suspend fun setDeceaseStatus(
        folio: String,
        date: String,
        status: Int
    ): Response<List<EntityUserData>> {
        return withContext(Dispatchers.IO) {
            doSetDeceased(folio, date, status)
        }
    }

    suspend fun deleteUser(folio: String): Response<List<EntityUserData>> {
        return withContext(Dispatchers.IO) {
            doSetDeleteUser(folio)
        }
    }

    suspend fun setBiography(
        biography: String,
        selectedFolio: String
    ): Response<List<EntityUserData>> {
        return withContext(Dispatchers.IO) {
            doSetBiography(biography, selectedFolio)
        }
    }

    suspend fun sendTribute(
        selectedFolio: String,
        tribute: String
    ): Response<List<EntityUserData>> {
        return withContext(Dispatchers.IO) {
            doSendTribute(selectedFolio, tribute)
        }
    }

    private fun shouldFetch(data: List<EntityUserData>): Boolean {
        if (RateLimiter.shouldFetch("User_data", 12, TimeUnit.HOURS)) {
            Log.i("TAG", "Time limit reached, ShouldFetch User_data data")
            return true
        }

        if (data.isEmpty()) {
            Log.i("TAG", "Data is empty, ShouldFetch User_data data")
            return true
        }

        Log.i("TAG", "Don't fetch new User_data data")
        return false
    }

    private fun getBackendModel(obj: EntityUserData?): DatabaseObject {
        val u = DatabaseObject()
        if (obj != null) {
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
            u.tributes = obj.tributes
        }
        return u
    }
}
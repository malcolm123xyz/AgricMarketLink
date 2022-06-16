package mx.mobile.solution.nabia04.room_database.repositories

import android.app.AlertDialog
import android.content.Context
import mx.mobile.solution.nabia04.room_database.entities.EntityUserData
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.main.MainActivity
import mx.mobile.solution.nabia04.room_database.view_models.LoadingStatus
import mx.mobile.solution.nabia04.utilities.Cons
import mx.mobile.solution.nabia04.R
import android.content.DialogInterface
import mx.mobile.solution.nabia04.room_database.UserDataDao
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import kotlin.jvm.Volatile
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.extensions.android.json.AndroidJsonFactory
import mx.mobile.solution.nabia04.alarm.MyAlarmManager
import mx.mobile.solution.nabia04.main.MainActivity.Companion.databaseViewModel
import mx.mobile.solution.nabia04.room_database.MainDataBase
import mx.mobile.solution.nabia04.utilities.SessionManager
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.DatabaseObject
import java.io.IOException
import java.lang.NullPointerException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*
import javax.net.ssl.SSLHandshakeException
import kotlin.collections.ArrayList

class DatabaseRepository(private val context: Context) {
    var birthDayPerson: EntityUserData? = null
    private val userFolio: String?
    private val token: String?
    val filterString = "none"
    val filterType = 0
    private val TAG = "DatabaseRepository"
    private var allUserData: List<EntityUserData>? = null
    private var dao: UserDataDao? = null
    private var sharedP: SharedPreferences? = null
    private var endpoint: MainEndpoint? = null

    val hometownDistrict: MutableList<String> = ArrayList()
    val hometownReg: MutableList<String> = ArrayList()
    val employmentSector: MutableList<String> = ArrayList()
    val specificOrg: MutableList<String> = ArrayList()
    val workRegion: MutableList<String> = ArrayList()
    val workDistrict: MutableList<String> = ArrayList()

    companion object {
        @Volatile
        var INSTANCE: DatabaseRepository? = null
        fun getInstance(context: Context): DatabaseRepository? {
            if (INSTANCE == null) {
                synchronized(DatabaseRepository::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = DatabaseRepository(context)
                    }
                }
            }
            return INSTANCE
        }

        fun getBackEndDataObject(obj: EntityUserData?): DatabaseObject {
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
                u.tributes = obj.tribute
                return u
            }
            return u
        }
    }

    init {
        dao = MainDataBase.getDatabase(context).userDataDao()
        sharedP = PreferenceManager.getDefaultSharedPreferences(context)
        userFolio = sharedP?.getString(SessionManager.FOLIO_NUMBER, "")
        token = sharedP?.getString(SessionManager.LOGIN_TOKEN, "")
    }

    fun getAllData(): List<EntityUserData>{
        if(allUserData == null){
            return ArrayList()
        }
        return allUserData!!
    }

    fun filterData() {
        if (allUserData != null) {
            for (user in allUserData!!) {
                if (!user.districtOfResidence.isNullOrEmpty() && !hometownDistrict.contains(user.districtOfResidence)){
                    hometownDistrict.add(user.districtOfResidence)
                }
                if (!user.regionOfResidence.isNullOrEmpty() && !hometownReg.contains(user.regionOfResidence)){
                    hometownReg.add(user.regionOfResidence) }
                if (!user.employmentSector.isNullOrEmpty() && !employmentSector.contains(user.employmentSector)){
                    employmentSector.add(user.employmentSector) }
                if (!user.specificOrg.isNullOrEmpty() && !specificOrg.contains(user.specificOrg)){
                    specificOrg.add(user.specificOrg) }
                if (!user.establishmentRegion.isNullOrEmpty() && !workRegion.contains(user.establishmentRegion)){
                    workRegion.add(user.establishmentRegion) }
                if (!user.establishmentDist.isNullOrEmpty() && !workDistrict.contains(user.establishmentDist)){
                    workDistrict.add(user.establishmentDist) }
            }

        }
    }

    fun insert(userDataList: EntityUserData?) {
        Thread { dao?.insertUserData(userDataList) }.start()
    }

    fun delete(id: String?) {
        dao?.deleteThisUser(id)
        allUserData = dao?.usersDataList
    }

    fun getUserData(id: String?): EntityUserData? {
        return dao?.getUser(id)
    }

    fun refreshDatabase(refresh: Boolean) {
        object : BackgroundTasks() {
            var errCode = 1
            var errMsg = ""
            override fun onPreExecute() {
                MainActivity.databaseLoadingStatus.setValue(LoadingStatus(true))
            }

            override fun doInBackground() {
                Log.i(TAG, "doInBackground().......")
                allUserData = dao?.usersDataList
                if (refresh) {
                    allUserData = ArrayList()
                }
                if (allUserData!!.size < 1) {
                    try {
                        endpoint = endpointObject
                        val response = endpoint!!.getMembers(token).execute()
                        errCode = response.returnCode
                        errMsg = response.response
                        Log.i(TAG, "External error code = $errCode")
                        if (response.returnCode == 1) {
                            allUserData = getEntity(response.databaseObj)
                            dao?.insertUserData(allUserData)
                            sharedP?.edit()?.putBoolean(Cons.DATABASE_REFRESH, false)?.apply()
                            MyAlarmManager(context).scheduleBirthdayNotification(allUserData!!)
                        }
                    } catch (ex: IOException) {
                        if (ex is SocketTimeoutException ||
                            ex is SSLHandshakeException ||
                            ex is UnknownHostException
                        ) {
                            errCode = 0
                            errMsg = "Cause: NO INTERNET CONNECTION"
                        } else {
                            errCode = 0
                            errMsg = "UNKNOWN ERROR"
                        }
                        ex.printStackTrace()
                        ex.printStackTrace()
                    }
                }
                filterData()
            }

            override fun onPostExecute() {
                MainActivity.databaseLoadingStatus.setValue(LoadingStatus(false))
                if (errCode == 1) {
                    databaseViewModel.setData(allUserData)
                } else if (errCode == Cons.NOT_LOGGED_IN) {
                    showLogoutErrorDialog(errMsg)
                } else {
                    showErrorDialog("Error while reading from the database: $errMsg")
                }
            }
        }.execute()
    }

    private fun showErrorDialog(errMsg: String) {
        AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle)
            .setTitle("ERROR")
            .setMessage(errMsg)
            .setPositiveButton(
                "OK"
            ) { dialog: DialogInterface, id: Int -> dialog.dismiss() }.show()
    }

    private fun showLogoutErrorDialog(errMsg: String) {
        AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle)
            .setTitle("ERROR")
            .setMessage("$errMsg\n\nDo you want to logout and login?")
            .setPositiveButton(
                "YES"
            ) { dialog: DialogInterface, id: Int -> dialog.dismiss() }.setNegativeButton(
                "NO"
            ) { dialog: DialogInterface, id: Int -> dialog.dismiss() }.show()
    }

    fun getEntity(list: List<DatabaseObject>): List<EntityUserData> {
        val entityUserDataList: MutableList<EntityUserData> = ArrayList()
        for (obj in list) {
            val u = EntityUserData()
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

    fun filter (query: String) {
        Log.i(TAG, "Query string: $query")
        val queryValue = query.lowercase(Locale.getDefault())
        object: BackgroundTasks(){
            var filteredList: MutableList<EntityUserData> = ArrayList()

            override fun onPreExecute() {
            }

            override fun doInBackground() {
                if(queryValue.isNotEmpty()){
                    for (model in allUserData!!) {
                        try {
                            val nameSearch =
                                model.fullName.lowercase(Locale.getDefault()).contains(queryValue) ||
                                        model.nickName.lowercase(Locale.getDefault())
                                            .contains(queryValue) ||
                                        model.folioNumber.lowercase(Locale.getDefault())
                                            .contains(queryValue) ||
                                        model.house.lowercase(Locale.getDefault())
                                            .contains(queryValue) ||
                                        model.className.lowercase(Locale.getDefault())
                                            .contains(queryValue) ||
                                        model.courseStudied.lowercase(Locale.getDefault())
                                            .contains(queryValue) ||
                                        model.homeTown.lowercase(Locale.getDefault())
                                            .contains(queryValue) ||
                                        model.districtOfResidence.lowercase(Locale.getDefault())
                                            .contains(queryValue) ||
                                        model.regionOfResidence.lowercase(Locale.getDefault())
                                            .contains(queryValue) ||
                                        model.employmentSector.lowercase(Locale.getDefault())
                                            .contains(queryValue)||
                                        model.jobDescription.lowercase(Locale.getDefault())
                                            .contains(queryValue)||
                                        model.specificOrg.lowercase(Locale.getDefault())
                                            .contains(queryValue)||
                                        model.nameOfEstablishment.lowercase(Locale.getDefault())
                                            .contains(queryValue)||
                                        model.establishmentRegion.lowercase(Locale.getDefault())
                                            .contains(queryValue)||
                                        model.establishmentDist.lowercase(Locale.getDefault())
                                            .contains(queryValue)
                            if (nameSearch) {
                                Log.i(TAG, "Query Hit")
                                filteredList.add(model)
                            }
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }
                }else{
                    allUserData?.let { filteredList.addAll(it) }
                }
            }

            override fun onPostExecute() {

                databaseViewModel.setData(filteredList)
            }

        }.execute()
    }

        val endpointObject: MainEndpoint?
            get() {
                if (endpoint == null) {
                    val builder = MainEndpoint.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        AndroidJsonFactory(),
                        null
                    )
                        .setRootUrl(Cons.ROOT_URL)
                    endpoint = builder.build()
                }
                return endpoint
            }


}
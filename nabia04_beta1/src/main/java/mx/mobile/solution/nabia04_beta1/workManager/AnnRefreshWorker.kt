package mx.mobile.solution.nabia04_beta1.workManager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.preference.PreferenceManager
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import mx.mobile.solution.nabia04_beta1.alarm.MyAlarmManager
import mx.mobile.solution.nabia04_beta1.data.MainDataBase
import mx.mobile.solution.nabia04_beta1.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04_beta1.data.entities.FcmToken
import mx.mobile.solution.nabia04_beta1.ui.activities.endpoint
import mx.mobile.solution.nabia04_beta1.utilities.RateLimiter
import mx.mobile.solution.nabia04_beta1.utilities.Response
import mx.mobile.solution.nabia04_beta1.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.Announcement
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class AnnRefreshWorker(appContext: Context, workerParams: WorkerParameters) :
    ListenableWorker(appContext, workerParams) {

    private val TAG = "AnnRefreshWorker"

    private var sharedP: SharedPreferences

    init {
        sharedP = PreferenceManager.getDefaultSharedPreferences(appContext)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun startWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
            val callback: MyCallback = object : MyCallback {
                override fun onFailure(e: IOException?) {
                    e?.printStackTrace()
                    completer.set(Result.retry())
                }

                override fun onSuccess() {
                    completer.set(Result.success())
                }

                override fun onSaveToken(fcmToken: FcmToken) {

                }
            }

            GlobalScope.launch {

                withContext(Dispatchers.IO) {
                    refresh()
                    val response = refresh()
                    if (response.status == Status.SUCCESS) {
                        Log.i(TAG, "DONE")
                        callback.onSuccess()
                    }
                }
            }
        }
    }

    private fun refresh(): Response<List<EntityAnnouncement>> {
        val erMsg: String
        try {
            val backendResponse = endpoint.noticeBoardData.execute()
            if (backendResponse.status == Status.SUCCESS.toString()) {
                val allAnnouncements = getAnnDataObjects(backendResponse.data).toList()
                val dao = MainDataBase.getDatabase(applicationContext).annDao()
                dao.insertAnnouncement(allAnnouncements)
                RateLimiter.reset("Announcement")
                MyAlarmManager(applicationContext).scheduleEventNotification(allAnnouncements)
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

}

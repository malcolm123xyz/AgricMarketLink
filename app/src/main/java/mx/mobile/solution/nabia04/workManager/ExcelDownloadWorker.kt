package mx.mobile.solution.nabia04.workManager

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import mx.mobile.solution.nabia04.App
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.FcmToken
import mx.mobile.solution.nabia04.ui.activities.MainActivity
import mx.mobile.solution.nabia04.utilities.Const
import mx.mobile.solution.nabia04.utilities.RateLimiter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ExcelDownloadWorker(val appContext: Context, workerParams: WorkerParameters) :
    ListenableWorker(appContext, workerParams) {

    private val tag = "ExcelDownloadWorker"

    private var sharedP: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(appContext)

    private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

    @OptIn(DelicateCoroutinesApi::class)
    override fun startWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
            val callback: MyCallback = object : MyCallback {
                override fun onFailure(e: IOException?) {
                    e?.printStackTrace()
                    completer.set(Result.retry())
                }

                override fun onSuccess() {
                    RateLimiter.reset("excelFile")
                    completer.set(Result.success())
                }

                override fun onSaveToken(fcmToken: FcmToken) {

                }
            }

            if (shouldFetch(getExcelFile())) {
                GlobalScope.launch {
                    withContext(Dispatchers.IO) {
                        downloader(callback)
                    }
                }
            }
        }
    }

    private fun shouldFetch(excelFile: File?): Boolean {
        if (RateLimiter.shouldFetch("excelFile", 1, TimeUnit.MINUTES)) {
            Log.i("TAG", "Time limit reached, ShouldFetch Excel doc")
            return true
        }

        if (excelFile == null) {
            Log.i("TAG", "Excel file is null, ShouldFetch Excel doc")
            return true
        }

        Log.i("TAG", "Don't fetch Excel doc")
        return false
    }

    private fun getExcelFile(): File? {
        val duesDir = File(App.applicationContext().filesDir, "Dues")
        duesDir.let {
            if (it.exists()) {
                return File(duesDir, "Nabiadues.xlsx")
            }
        }
        return null
    }

    private fun downloader(callback: MyCallback) {
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(Const.EXCEL_URL)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.doOutput = false
            urlConnection.connect()
            val duesDir = File(appContext.filesDir, "Dues")
            if (!duesDir.exists()) {
                duesDir.mkdirs()
            }
            val excelFile = File(duesDir, "Nabiadues.xlsx")
            if (!excelFile.exists()) {
                excelFile.createNewFile()
            }
            val inputStream: InputStream = urlConnection.inputStream
            val totalSize: Int = urlConnection.contentLength
            val outPut = FileOutputStream(excelFile)
            var downloadedSize = 0
            val buffer = ByteArray(2024)
            var bufferLength = 0
            while (inputStream.read(buffer).also { bufferLength = it } > 0) {
                outPut.write(buffer, 0, bufferLength)
                downloadedSize += bufferLength
                Log.e(
                    "Progress:",
                    "downloadedSize:" + Math.abs(downloadedSize * 100 / totalSize)
                )
            }
            outPut.close()
            showNotification(System.currentTimeMillis(), "Excel downloaded")
            callback.onSuccess()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("checkException:-", "" + e)
            callback.onFailure(e)
        }
    }

    private fun showNotification(time: Long, content: String) {
        val notificationManager = (appContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager?)

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(applicationContext, Const.TOKEN_REFRESH_NOTIFY_CHANNEL)

        val intent1 = Intent(applicationContext, MainActivity::class.java)
        intent1.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        builder.setSmallIcon(R.drawable.logo)
        builder.setContentTitle("Excel download")
        builder.setContentText("$content (${fd.format(Date(time))})")
        builder.priority = NotificationCompat.PRIORITY_DEFAULT
        builder.setAutoCancel(true)
        builder.setVibrate(longArrayOf(100, 100, 100, 100, 100))
        notificationManager?.notify(index, builder.build())
    }

    private val index: Int
        get() {
            var curVal = sharedP.getInt("Int_Count", 0)
            curVal++
            sharedP.edit().putInt("Int_Count", curVal).apply()
            return curVal
        }

}

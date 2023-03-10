package mx.mobile.solution.nabia04_beta1.workManager

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import mx.mobile.solution.nabia04_beta1.App
import mx.mobile.solution.nabia04_beta1.data.entities.FcmToken
import mx.mobile.solution.nabia04_beta1.utilities.Const
import mx.mobile.solution.nabia04_beta1.utilities.RateLimiter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class ExcelDownloadWorker(val appContext: Context, workerParams: WorkerParameters) :
    ListenableWorker(appContext, workerParams) {

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
            return true
        }

        if (excelFile == null) {
            return true
        }

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
        val urlConnection: HttpURLConnection?
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
            var bufferLength: Int
            while (inputStream.read(buffer).also { bufferLength = it } > 0) {
                outPut.write(buffer, 0, bufferLength)
                downloadedSize += bufferLength
                Log.e(
                    "Progress:",
                    "downloadedSize:" + abs(downloadedSize * 100 / totalSize)
                )
            }
            outPut.close()
            callback.onSuccess()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("checkException:-", "" + e)
            callback.onFailure(e)
        }
    }

}

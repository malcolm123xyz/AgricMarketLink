package mx.mobile.solution.nabia04_beta1.utilities

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class BackgroundTasks {
    private val executors: ExecutorService = Executors.newSingleThreadExecutor()
    private fun startBackground() {
        onPreExecute()
        executors.execute {
            doInBackground()
            Handler(Looper.getMainLooper()).post { onPostExecute() }
        }
    }

    fun execute() {
        startBackground()
    }

    abstract fun onPreExecute()
    abstract fun doInBackground()
    abstract fun onPostExecute()

}
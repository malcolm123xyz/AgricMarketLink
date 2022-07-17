package mx.mobile.solution.nabia04.data.view_models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.utilities.Resource
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ContributionData
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

@HiltViewModel
class NetworkViewModel @Inject constructor(val endpoint: MainEndpoint) : ViewModel() {

    private var data: MutableLiveData<Resource<String>> = MutableLiveData()

    fun getListenableData(
        contData: ContributionData,
        imageUri: String
    ): LiveData<Resource<String>> {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                sendPicture(contData, imageUri)
            }
        }

        return data
    }

    private fun sendPicture(contData: ContributionData, imageUri: String) {
        if (imageUri.isEmpty()) {
            sendToBackend(contData)
            return
        }

        data.postValue(Resource.loading("Sending picture..."))

        val id = System.currentTimeMillis().toString()
        MediaManager.get().upload(imageUri)
            .option("resource_type", "auto")
            .unsigned("my_preset")
            .option("public_id", "Nabia04/contribution/$id")
            .option("cloud_name", "callmanager")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    Log.i("TAG", "Sending image started...")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    Log.i("TAG", "onSuccess called")
                    contData.imageUri = resultData["secure_url"].toString()
                    viewModelScope.launch {
                        withContext(Dispatchers.IO) {
                            sendToBackend(contData)
                        }
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    data.postValue(Resource.error("Failed to upload picture. Please try again", ""))
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    private fun sendToBackend(contribution: ContributionData) {
        data.postValue(Resource.loading("Sending to Server"))
        try {
            val response = endpoint.setContRequest(contribution).execute()
            if (response.returnCode == 1) {
                data.postValue(Resource.success(""))
            } else {
                val err = response.returnMsg ?: "Unknown error"
                data.postValue(Resource.error(err, ""))
            }
        } catch (ex: IOException) {
            val erMsg = if (ex is SocketTimeoutException || ex is SSLHandshakeException ||
                ex is UnknownHostException
            ) {
                "Cause: NO INTERNET CONNECTION"
            } else {
                ex.localizedMessage ?: ""
            }
            ex.printStackTrace()
            data.postValue(Resource.error(erMsg, ""))
        }
    }
}
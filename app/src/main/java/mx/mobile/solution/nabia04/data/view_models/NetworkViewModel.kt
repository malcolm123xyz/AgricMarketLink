package mx.mobile.solution.nabia04.data.view_models

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.repositories.DBRepository
import mx.mobile.solution.nabia04.utilities.Response
import mx.mobile.solution.nabia04.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ContributionData
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

@HiltViewModel
class NetworkViewModel @Inject constructor(
    val endpoint: MainEndpoint,
    val sharedP: SharedPreferences,
    val repository: DBRepository
) : ViewModel() {

    private var data: MutableLiveData<Response<String>> = MutableLiveData()

    fun sendContribution(userData: ContributionData, imageUri: String): LiveData<Response<String>> {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                sendPicture(userData, imageUri)
            }
        }
        return data
    }

    fun publishExcel(sRef: String, excelUri: String): LiveData<Response<String>> {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                publish(sRef, excelUri)
            }
        }
        return data
    }

    fun upDateUserData(userData: EntityUserData, newImageUri: String): LiveData<Response<String>> {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.upDateUserData(data, userData, newImageUri)
            }
        }
        return data
    }

    private fun publish(sRef: String, excelUri: String) {
        data.postValue(Response.loading("Publishing excel... Please wait."))
        val storage = Firebase.storage
        val storageRef = storage.reference

        //val userIconRef = storageRef.child("dues/Nabiadues.xlsx")
        val userIconRef = storageRef.child(sRef)
        val stream = FileInputStream(File(excelUri))
        val uploadTask = userIconRef.putStream(stream)
        uploadTask.addOnFailureListener {
            data.postValue(
                Response.error(
                    ("Failed to publish: ${it.localizedMessage ?: "Unknown error"}"), null
                )
            )
        }.addOnSuccessListener { taskSnapshot ->

            viewModelScope.launch {
                notifyExcelPublish()
            }
            data.postValue(Response.success(null))
        }

    }

    private suspend fun notifyExcelPublish() {
        withContext(Dispatchers.IO) {
            endpoint.notifyExcelPublish().execute()
        }
    }

    private fun sendPicture(contData: ContributionData, imageUri: String) {
        if (imageUri.isEmpty()) {
            sendToBackend(contData)
            return
        }

        data.postValue(Response.loading("Sending picture..."))

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
                    data.postValue(Response.error("Failed to upload picture. Please try again", ""))
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    private fun sendToBackend(contribution: ContributionData) {
        data.postValue(Response.loading("Sending to Server"))
        try {
            val response = endpoint.setContRequest(contribution).execute()
            if (response.status == Status.SUCCESS.toString()) {
                data.postValue(Response.success(""))
            } else {
                val err = response.message ?: "Unknown error"
                data.postValue(Response.error(err, ""))
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
            data.postValue(Response.error(erMsg, ""))
        }
    }
}
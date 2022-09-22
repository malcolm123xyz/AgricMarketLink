package mx.mobile.solution.nabia04_beta1.data.view_models

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04_beta1.data.entities.EntityUserData
import mx.mobile.solution.nabia04_beta1.data.repositories.DBRepository
import mx.mobile.solution.nabia04_beta1.ui.activities.endpoint
import mx.mobile.solution.nabia04_beta1.utilities.Response
import mx.mobile.solution.nabia04_beta1.utilities.Status
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
                withContext(Dispatchers.IO) {
                    notifyExcelPublish()
                }
            }
            data.postValue(Response.success(null))
        }

    }

    private fun notifyExcelPublish() {
        endpoint.notifyExcelPublish().execute()
    }

    private fun sendPicture(contData: ContributionData, imageUri: String) {
        if (imageUri.isEmpty()) {
            sendToBackend(contData)
            return
        }

        data.postValue(Response.loading("Sending picture..."))

        val id = System.currentTimeMillis().toString()

        val storage = Firebase.storage
        val storageRef = storage.reference
        val userIconRef = storageRef.child("contribution/$id")
        val stream = FileInputStream(File(imageUri))
        val uploadTask = userIconRef.putStream(stream)
        uploadTask.addOnFailureListener {
            data.postValue(Response.error("Failed to upload picture. Please try again", ""))
            it.printStackTrace()
        }.continueWithTask { task ->
            if (!task.isSuccessful) {
                data.postValue(Response.error("Failed to upload picture. Please try again", ""))
                task.exception?.let { throw it }
            }
            userIconRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                contData.imageUri = task.result.toString()
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        sendToBackend(contData)
                    }
                }
            } else {
                data.postValue(Response.error("Failed to upload picture. Please try again", ""))
                task.exception?.printStackTrace()
            }
        }

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
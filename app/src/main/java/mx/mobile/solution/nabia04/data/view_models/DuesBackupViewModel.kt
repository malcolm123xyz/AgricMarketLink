package mx.mobile.solution.nabia04.data.view_models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.data.dao.DuesBackupDao
import mx.mobile.solution.nabia04.data.entities.EntityDuesBackup
import mx.mobile.solution.nabia04.utilities.ExcelHelper
import mx.mobile.solution.nabia04.utilities.Response
import mx.mobile.solution.nabia04.utilities.Status
import javax.inject.Inject

@HiltViewModel
class DuesBackupViewModel @Inject constructor(
    val excelHelper: ExcelHelper,
    val duesBackupDao: DuesBackupDao
) : ViewModel() {

    private var data: MutableLiveData<Response<List<EntityDuesBackup>>> = MutableLiveData()

    fun fetchBackups(): LiveData<Response<List<EntityDuesBackup>>> {
        viewModelScope.launch {
            data.postValue(Response.loading(null))
            val response = withContext(Dispatchers.IO) {
                getBackups()
            }
            if (response.status == Status.SUCCESS) {
                data.postValue(Response.success(response.data))
            } else {
                data.postValue(Response.error("${response.message}", null))
            }
        }
        return data
    }

    private fun getBackups(): Response<List<EntityDuesBackup>> {
        var response: List<EntityDuesBackup>? = null
        try {
            response = duesBackupDao.getBackups
            Log.i("TAG", "BACKUPS: $response")
        } catch (e: Exception) {
            e.localizedMessage?.let { Response.error(it, null) }
        }
        return if (response.isNullOrEmpty()) {
            Response.error("Backups empty", null)
        } else {
            Response.success(response)

        }
    }
}
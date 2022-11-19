package mx.mobile.solution.nabia04_beta1.data.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04_beta1.data.entities.EntityUserData
import mx.mobile.solution.nabia04_beta1.data.repositories.DBRepository
import mx.mobile.solution.nabia04_beta1.utilities.Response
import mx.mobile.solution.nabia04_beta1.utilities.Status
import javax.inject.Inject

@HiltViewModel
class DBViewModel @Inject constructor(var repository: DBRepository) : ViewModel() {
    private var data: MutableLiveData<Response<List<EntityUserData>>> = MutableLiveData()

    fun fetchUserDataList(): LiveData<Response<List<EntityUserData>>> {
        viewModelScope.launch {
            data.postValue(Response.loading(null))
            val response = repository.fetchUserData()
            when (response.status) {
                Status.SUCCESS -> {
                    val list = response.data?.toMutableList()
                    list?.sortWith { obj1: EntityUserData, obj2: EntityUserData ->
                        obj1.fullName.compareTo(obj2.fullName)
                    }
                    data.postValue(Response.success(list))
                }
                else -> {
                    val e = response.message ?: ""
                    data.postValue(Response.error(e, response.data))
                }
            }
        }
        return data
    }

    suspend fun getUser(folio: String): EntityUserData? {
        return repository.getUser(folio)
    }

    fun refreshDB(): LiveData<Response<List<EntityUserData>>> {
        viewModelScope.launch {
            data.postValue(Response.loading(null))
            val response = repository.refreshDB()
            when (response.status) {
                Status.SUCCESS -> {
                    data.postValue(Response.success(response.data))
                }
                else -> {
                    val e = response.message ?: ""
                    data.postValue(Response.error(e, null))
                }
            }
        }
        return data
    }

    suspend fun getList(): List<EntityUserData>? {
        return repository.fetchUserData().data
    }

    suspend fun getUserNames(): List<EntityUserData>? {
        return repository.fetchUserNames()
    }

    suspend fun setUserClearance(folio: String, clearance: String): Response<List<EntityUserData>> {
        return repository.setUserClearance(folio, clearance)
    }

    suspend fun setDeceaseStatus(
        folio: String,
        date: String,
        status: Int
    ): Response<List<EntityUserData>> {
        return repository.setDeceaseStatus(folio, date, status)
    }

    suspend fun deleteUser(folio: String): Response<List<EntityUserData>> {
        return repository.deleteUser(folio)
    }

    suspend fun setBiography(
        biography: String,
        selectedFolio: String
    ): Response<List<EntityUserData>> {
        return repository.setBiography(biography, selectedFolio)
    }

    suspend fun sendTribute(
        selectedFolio: String,
        tribute: String
    ): Response<List<EntityUserData>> {
        return repository.sendTribute(selectedFolio, tribute)
    }

}
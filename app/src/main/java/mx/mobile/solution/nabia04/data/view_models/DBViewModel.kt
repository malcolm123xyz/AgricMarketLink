package mx.mobile.solution.nabia04.data.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.repositories.DBRepository
import mx.mobile.solution.nabia04.utilities.Resource
import mx.mobile.solution.nabia04.utilities.Status
import javax.inject.Inject

@HiltViewModel
class DBViewModel @Inject constructor(var repository: DBRepository) : ViewModel() {
    private var data: MutableLiveData<Resource<List<EntityUserData>>> = MutableLiveData()

    fun fetchUserDataList(): LiveData<Resource<List<EntityUserData>>> {
        viewModelScope.launch {
            data.postValue(Resource.loading(null))
            val response = repository.fetchUserData()
            when (response.status) {
                Status.SUCCESS -> {
                    data.postValue(Resource.success(response.data))
                }
                else -> {
                    val e = response.message ?: ""
                    data.postValue(Resource.error(e, null))
                }
            }
        }
        return data
    }


    suspend fun getUser(folio: String): EntityUserData? {
        return repository.getUser(folio)
    }

    fun refreshDB(): LiveData<Resource<List<EntityUserData>>> {
        viewModelScope.launch {
            data.postValue(Resource.loading(null))
            val response = repository.refreshDB()
            when (response.status) {
                Status.SUCCESS -> {
                    data.postValue(Resource.success(response.data))
                }
                else -> {
                    val e = response.message ?: ""
                    data.postValue(Resource.error(e, null))
                }
            }
        }
        return data
    }

    suspend fun getFilterData(): List<EntityUserData>? {
        return repository.fetchUserData().data
    }

    suspend fun getList(): List<EntityUserData>? {
        return repository.fetchUserData().data
    }

    suspend fun setUserClearance(folio: String, clearance: String): Resource<List<EntityUserData>> {
        return repository.setUserClearance(folio, clearance)
    }

    suspend fun setDeceaseStatus(
        folio: String,
        date: String,
        status: Int
    ): Resource<List<EntityUserData>> {
        return repository.setDeceaseStatus(folio, date, status)
    }

    suspend fun deleteUser(folio: String): Resource<List<EntityUserData>> {
        return repository.deleteUser(folio)
    }

    suspend fun setBiography(
        biography: String,
        selectedFolio: String
    ): Resource<List<EntityUserData>> {
        return repository.setBiography(biography, selectedFolio);
    }

    suspend fun sendTribute(
        selectedFolio: String,
        tribute: String
    ): Resource<List<EntityUserData>> {
        return repository.sendTribute(selectedFolio, tribute)
    }

}
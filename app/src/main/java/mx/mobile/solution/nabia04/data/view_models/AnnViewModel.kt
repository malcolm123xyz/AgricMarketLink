package mx.mobile.solution.nabia04.data.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.data.repositories.AnnRepository
import mx.mobile.solution.nabia04.utilities.Resource
import mx.mobile.solution.nabia04.utilities.Status
import javax.inject.Inject

@HiltViewModel
class AnnViewModel @Inject constructor(var repository: AnnRepository) : ViewModel() {

    private var data: MutableLiveData<Resource<List<EntityAnnouncement>>> = MutableLiveData()

    fun fetchAnn(): LiveData<Resource<List<EntityAnnouncement>>> {
        viewModelScope.launch {
            data.postValue(Resource.loading(null))
            val response = repository.fetchAnn()
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


    suspend fun getAnn(id: Long): EntityAnnouncement? {
        return repository.getAnn(id)
    }

    suspend fun getSingleData(id: Long): EntityAnnouncement? {
        val list = data.value?.data ?: ArrayList()
        for (ann in list) {
            if (ann.id == id) {
                return ann
            }
        }
        return null
    }

    fun refreshDB(): LiveData<Resource<List<EntityAnnouncement>>> {
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

    suspend fun setAnnAsRead(announcement: EntityAnnouncement) {
        repository.setAnnRead(announcement)
    }

    suspend fun delete(announcement: EntityAnnouncement): Int {
        return repository.delete(announcement)
    }

    suspend fun deleteFrmServer(id: Long): Int {
        return repository.deleteFrmServer(id)
    }

}
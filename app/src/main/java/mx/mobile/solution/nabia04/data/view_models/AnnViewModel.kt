package mx.mobile.solution.nabia04.data.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.data.repositories.AnnRepository
import mx.mobile.solution.nabia04.utilities.Response
import mx.mobile.solution.nabia04.utilities.Status
import javax.inject.Inject

@HiltViewModel
class AnnViewModel @Inject constructor(var repository: AnnRepository) : ViewModel() {

    private var genData: MutableLiveData<Response<List<EntityAnnouncement>>> = MutableLiveData()

    fun fetchAnn(): LiveData<Response<List<EntityAnnouncement>>> {
        viewModelScope.launch {
            genData.postValue(Response.loading(null))
            val response = repository.fetchAnn()
            when (response.status) {
                Status.SUCCESS -> {
                    val announcements = response.data?.toMutableList()
                    announcements?.sortWith { obj1: EntityAnnouncement, obj2: EntityAnnouncement ->
                        obj2.id.compareTo(obj1.id)
                    }
                    genData.postValue(Response.success(announcements))
                }
                else -> {
                    val e = response.message ?: ""
                    genData.postValue(Response.error(e, response.data))
                }
            }
        }
        return genData
    }

    suspend fun getAnn(id: Long): EntityAnnouncement? {
        return repository.getAnn(id)
    }

    fun refreshDB(): LiveData<Response<List<EntityAnnouncement>>> {
        viewModelScope.launch {
            genData.postValue(Response.loading(null))
            val response = repository.refreshDB()
            when (response.status) {
                Status.SUCCESS -> {
                    val announcements = response.data?.toMutableList()
                    announcements?.sortWith { obj1: EntityAnnouncement, obj2: EntityAnnouncement ->
                        obj2.id.compareTo(obj1.id)
                    }
                    genData.postValue(Response.success(announcements))
                }
                else -> {
                    val e = response.message ?: ""
                    genData.postValue(Response.error(e, null))
                }
            }
        }
        return genData
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
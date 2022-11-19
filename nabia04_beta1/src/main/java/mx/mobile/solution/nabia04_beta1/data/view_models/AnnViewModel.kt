package mx.mobile.solution.nabia04_beta1.data.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04_beta1.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04_beta1.data.repositories.AnnRepository
import mx.mobile.solution.nabia04_beta1.utilities.Response
import javax.inject.Inject

@HiltViewModel
class AnnViewModel @Inject constructor(var repository: AnnRepository) : ViewModel() {

    private var data: MutableLiveData<Response<List<EntityAnnouncement>>> = MutableLiveData()

    fun fetchAnn(): LiveData<Response<List<EntityAnnouncement>>> {
        viewModelScope.launch {
            data.postValue(Response.loading(null))
            data.postValue(repository.fetchAnn())
        }
        return data
    }

    suspend fun getAnn(id: Long): EntityAnnouncement? {
        return repository.getAnn(id)
    }

    fun refreshDB() {
        viewModelScope.launch {
            data.postValue(Response.loading(null))
            data.postValue(repository.refreshDB())
        }
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
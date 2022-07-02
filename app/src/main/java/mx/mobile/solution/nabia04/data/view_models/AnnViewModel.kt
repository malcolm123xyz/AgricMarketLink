package mx.mobile.solution.nabia04.data.view_models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mx.mobile.solution.nabia04.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.data.repositories.AnnRepository
import mx.mobile.solution.nabia04.utilities.Resource
import javax.inject.Inject

@HiltViewModel
class AnnViewModel @Inject constructor(var repository: AnnRepository) : ViewModel() {
    lateinit var data: LiveData<Resource<List<EntityAnnouncement>>>
    fun fetchAnn(): LiveData<Resource<List<EntityAnnouncement>>> {
        data = repository.loadAnn()
        return data
    }

    fun refreshDB(): LiveData<Resource<List<EntityAnnouncement>>> {
        Log.i("TAG", "AnnViewModel Refresh")
        data = repository.refreshDB()
        return data
    }

}
package mx.mobile.solution.nabia04.main.data.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import mx.mobile.solution.nabia04.main.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.main.data.repositories.AnnRepository
import mx.mobile.solution.nabia04.utilities.Resource
import javax.inject.Inject

class AnnViewModel1 @Inject constructor(val repository: AnnRepository) : ViewModel() {


    lateinit var data: LiveData<Resource<List<EntityAnnouncement>>>
    fun fetchAnn(): LiveData<Resource<List<EntityAnnouncement>>> {
        data = repository.loadAnn()
        return data
    }

}
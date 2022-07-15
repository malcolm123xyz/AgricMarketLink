package mx.mobile.solution.nabia04.data.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.repositories.DBRepository
import javax.inject.Inject

@HiltViewModel
class DBUpdateViewModel @Inject constructor(var repository: DBRepository) : ViewModel() {
    private var data: MutableLiveData<EntityUserData> = MutableLiveData()

    fun getDataToObserve(folio: String): LiveData<EntityUserData> {
        if (data.value?.folioNumber != null) {
            return data
        }
        viewModelScope.launch {
            data.postValue(repository.getUser(folio))
        }
        return data
    }

    fun postData(user: EntityUserData) {
        data.postValue(user)
    }

    fun getDataObj(): EntityUserData? {
        return data.value
    }

}
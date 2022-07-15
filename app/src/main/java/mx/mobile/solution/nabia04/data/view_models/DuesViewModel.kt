package mx.mobile.solution.nabia04.data.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04.data.entities.EntityDues
import mx.mobile.solution.nabia04.utilities.ExcelHelper
import mx.mobile.solution.nabia04.utilities.Resource
import javax.inject.Inject

@HiltViewModel
class DuesViewModel @Inject constructor(val excelHelper: ExcelHelper) : ViewModel() {

    private var data: MutableLiveData<Resource<List<EntityDues>>> = MutableLiveData()

    fun fetchAnn(): LiveData<Resource<List<EntityDues>>> {
        viewModelScope.launch {
            data.postValue(Resource.loading(null))
            val response = excelHelper.getDues()
            if (response.isNotEmpty()) {
                data.postValue(Resource.success(response))
            } else {
                data.postValue(Resource.error("Data not found", null))
            }
        }
        return data
    }

}
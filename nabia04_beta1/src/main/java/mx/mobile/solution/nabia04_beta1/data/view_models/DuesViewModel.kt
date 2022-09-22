package mx.mobile.solution.nabia04_beta1.data.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04_beta1.data.entities.EntityDues
import mx.mobile.solution.nabia04_beta1.utilities.ExcelHelper
import mx.mobile.solution.nabia04_beta1.utilities.Response
import javax.inject.Inject

@HiltViewModel
class DuesViewModel @Inject constructor(val excelHelper: ExcelHelper) : ViewModel() {

    private var data: MutableLiveData<Response<List<EntityDues>>> = MutableLiveData()

    fun fetchDues(): LiveData<Response<List<EntityDues>>> {
        viewModelScope.launch {
            data.postValue(Response.loading(null))
            val response = excelHelper.getDues()
            if (response.isNotEmpty()) {
                data.postValue(Response.success(response))
            } else {
                data.postValue(Response.error("Data not found", null))
            }
        }
        return data
    }

    fun notifyFileChange() {
        data.postValue(Response.emitEvent("MODIFIED"))
    }

}
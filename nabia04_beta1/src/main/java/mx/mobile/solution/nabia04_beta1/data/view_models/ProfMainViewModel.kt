package mx.mobile.solution.nabia04_beta1.data.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04_beta1.data.entities.EntityQuestion
import mx.mobile.solution.nabia04_beta1.data.repositories.ProfMainViewRepository
import mx.mobile.solution.nabia04_beta1.utilities.Response
import mx.mobile.solution.nabia04_beta1.utilities.Status
import javax.inject.Inject

@HiltViewModel
class ProfMainViewModel @Inject constructor(var repository: ProfMainViewRepository) : ViewModel() {
    private var data: MutableLiveData<Response<List<EntityQuestion>>> = MutableLiveData()

    fun fetchQuestions(): LiveData<Response<List<EntityQuestion>>> {
        viewModelScope.launch {
            data.postValue(Response.loading(null))
            val response = repository.fetchQuestions()
            when (response.status) {
                Status.SUCCESS -> {
                    val list = response.data?.toMutableList()
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

    suspend fun getUser(folio: String): EntityQuestion? {
        return repository.getQuestion(folio)
    }

    fun refreshDB() {
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
    }

    fun reload() {
        viewModelScope.launch {
            data.postValue(Response.loading(null))
            val response = repository.fetchQuestions()
            when (response.status) {
                Status.SUCCESS -> {
                    val list = response.data?.toMutableList()
                    data.postValue(Response.success(list))
                }
                else -> {
                    val e = response.message ?: ""
                    data.postValue(Response.error(e, response.data))
                }
            }
        }
    }

}
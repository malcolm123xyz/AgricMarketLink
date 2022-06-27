package mx.mobile.solution.nabia04.main.ui.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.DatabaseObject

class DatabaseUpdateViewModel : ViewModel() {
    private val userdata: MutableLiveData<DatabaseObject> = MutableLiveData()

    fun setValue(value: DatabaseObject) {
        userdata.value = value
    }

    fun getValue (): LiveData<DatabaseObject?> {
        return userdata
    }

}
package mx.mobile.solution.nabia04.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import mx.mobile.solution.nabia04.room_database.entities.EntityUserData
import androidx.lifecycle.LiveData
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
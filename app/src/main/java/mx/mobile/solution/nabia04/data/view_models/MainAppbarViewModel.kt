package mx.mobile.solution.nabia04.data.view_models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import mx.mobile.solution.nabia04.util.Event

class MainAppbarViewModel : ViewModel() {
    val currentNavController = MutableLiveData<Event<NavController>>()
}
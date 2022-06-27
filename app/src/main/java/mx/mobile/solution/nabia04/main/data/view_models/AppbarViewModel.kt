package mx.mobile.solution.nabia04.core.old_package.database.view_models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import mx.mobile.solution.nabia04.utilities.Event

class AppbarViewModel : ViewModel() {
    val currentNavController = MutableLiveData<Event<NavController>>()
}
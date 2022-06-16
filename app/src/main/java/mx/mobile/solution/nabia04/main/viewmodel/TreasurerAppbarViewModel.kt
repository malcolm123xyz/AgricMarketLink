package com.smarttoolfactory.tutorial7_2bnv_viewpager2_complexarchitecture.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import mx.mobile.solution.nabia04.main.util.Event

class TreasurerAppbarViewModel : ViewModel() {
    val currentNavController = MutableLiveData<Event<NavController>>()
}
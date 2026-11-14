package com.nexus.planner.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.planner.data.local.entity.FrictionLogEntity
import com.nexus.planner.data.local.entity.HabitEntity
import com.nexus.planner.data.repository.NexusRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(repository: NexusRepository) : ViewModel() {

    val habits: StateFlow<List<HabitEntity>> = repository.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val frictions: StateFlow<List<FrictionLogEntity>> = repository.getAllFrictions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
}

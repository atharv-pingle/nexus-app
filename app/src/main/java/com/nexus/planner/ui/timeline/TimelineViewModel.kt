package com.nexus.planner.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.planner.data.local.entity.HabitEntity
import com.nexus.planner.data.local.entity.ScheduleBlockEntity
import com.nexus.planner.data.repository.NexusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TimelineViewModel(private val repository: NexusRepository) : ViewModel() {

    private val todayIso = LocalDate.now().toString()

    val schedule: StateFlow<List<ScheduleBlockEntity>> = repository.getTodaySchedule(todayIso)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val habits: StateFlow<List<HabitEntity>> = repository.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    init {
        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty(todayIso)
        }
    }

    fun addDelay(minutes: Int) {
        viewModelScope.launch {
            repository.addDelayToRemainingDay(todayIso, minutes)
            _statusMessage.value = "+${minutes}m delay absorbed. Schedule updated."
        }
    }

    fun completeMicroAction(blockId: Long) {
        viewModelScope.launch {
            repository.updateBlockStatus(blockId, "COMPLETED")
            _statusMessage.value = "Quest complete. Momentum locked."
        }
    }

    fun applyGentleSwap(blockId: Long, counterAction: String) {
        viewModelScope.launch {
            repository.updateBlockStatus(blockId, "COMPLETED")
            _statusMessage.value = "⚡ Streak preserved! Completed: $counterAction"
        }
    }

    fun skipBlock(blockId: Long) {
        viewModelScope.launch {
            repository.updateBlockStatus(blockId, "SKIPPED")
            _statusMessage.value = "Task skipped. Time restored to buffer."
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}

package com.nexus.planner.ui.architect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.planner.data.repository.NexusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ArchitectViewModel(private val repository: NexusRepository) : ViewModel() {

    private val todayIso = LocalDate.now().toString()
    private val tomorrowIso = LocalDate.now().plusDays(1).toString()

    val selectedEnergy = MutableStateFlow("MEDIUM")
    val selectedFrictions = MutableStateFlow<Set<String>>(emptySet())
    val priorityGoal = MutableStateFlow("Validate SaaS problem in developer communities")

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _generationResult = MutableStateFlow<String?>(null)
    val generationResult: StateFlow<String?> = _generationResult

    fun selectEnergy(level: String) {
        selectedEnergy.value = level
    }

    fun toggleFriction(tag: String) {
        val current = selectedFrictions.value.toMutableSet()
        if (current.contains(tag)) current.remove(tag) else current.add(tag)
        selectedFrictions.value = current
    }

    fun setPriorityGoal(goal: String) {
        priorityGoal.value = goal
    }

    fun generateTomorrowPlan() {
        viewModelScope.launch {
            _isGenerating.value = true
            val result = repository.logFrictionAndDebrief(
                dateIso = todayIso,
                energyLevel = selectedEnergy.value,
                frictionTags = selectedFrictions.value.toList(),
                priorityGoal = priorityGoal.value,
                targetDateIso = tomorrowIso
            )
            _isGenerating.value = false
            _generationResult.value = result.getOrNull() ?: "Generated tomorrow's battle plan with local intelligence."
        }
    }
}

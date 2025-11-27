package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.gdacs.Disaster
import com.example.myapplication.gdacs.DisasterType
import com.example.myapplication.gdacs.GdacsApi
import com.example.myapplication.gdacs.toDisaster
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DisasterState(
    val disasters: List<Disaster> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTypes: Set<DisasterType> = DisasterType.values().toSet(),
    val nearbyDisasters: List<Disaster> = emptyList()
)

class DisasterViewModel : ViewModel() {
    private val _state = MutableStateFlow(DisasterState())
    val state: StateFlow<DisasterState> = _state.asStateFlow()

    private val proximityThresholdKm = 500.0 // Alert if disaster within 500km

    init {
        loadDisasters()
    }

    fun loadDisasters() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                // Get all red and orange alerts (severe and moderate) for all disaster types
                val response = GdacsApi.service.getDisasters(
                    alertLevel = "red;orange;green",
                    eventList = "EQ;TC;FL;VO;DR;WF" // All disaster types
                )
                val disasters = response.features.map { it.toDisaster() }
                _state.value = _state.value.copy(
                    disasters = disasters,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    disasters = emptyList(),
                    isLoading = false,
                    error = "Failed to load disasters: ${e.message}"
                )
            }
        }
    }

    fun updateNearbyDisasters(userLat: Double, userLon: Double) {
        val nearby = _state.value.disasters.filter { disaster ->
            disaster.getDistanceFrom(userLat, userLon) <= proximityThresholdKm
        }.sortedBy { it.getDistanceFrom(userLat, userLon) }

        _state.value = _state.value.copy(nearbyDisasters = nearby)
    }

    fun toggleDisasterType(type: DisasterType) {
        val currentTypes = _state.value.selectedTypes.toMutableSet()
        if (currentTypes.contains(type)) {
            currentTypes.remove(type)
        } else {
            currentTypes.add(type)
        }
        _state.value = _state.value.copy(selectedTypes = currentTypes)
    }

    fun getFilteredDisasters(): List<Disaster> {
        return _state.value.disasters.filter { disaster ->
            _state.value.selectedTypes.contains(disaster.type)
        }
    }

    fun getDisastersByType(type: DisasterType): List<Disaster> {
        return _state.value.disasters.filter { it.type == type }
    }

    fun getDisasterCount(): Int = _state.value.disasters.size

    fun getSevereDisasterCount(): Int =
        _state.value.disasters.count { it.severity.displayName == "Severe" }
}

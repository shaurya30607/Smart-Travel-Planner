package com.example.smarttravelplanner.ui.screens.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.UserPreferencesRepository
import com.example.smarttravelplanner.domain.model.UserPreferences
import com.example.smarttravelplanner.domain.usecase.GenerateTripUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val generateTripUseCase: GenerateTripUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlanUiState>(PlanUiState.Idle)
    val uiState = _uiState.asStateFlow()

    /** User's AI and trip settings loaded from DataStore */
    val userPreferences = userPreferencesRepository.userPreferences
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    fun generateItinerary(
        destination: String,
        days: Int,
        budget: Double,
        interests: List<String>,
        travelStyle: String = "balanced",
        startDate: LocalDate? = null,
        notes: String = "",
        adults: Int = 1,
        kids: Int = 0
    ) {
        viewModelScope.launch {
            val prefs = userPreferences.value
            val isCloud = !prefs.aiProvider.equals("LOCAL", ignoreCase = true)
            val model = if (isCloud) {
                prefs.cloudModel.ifBlank { "gemini-3.1-flash-lite" }
            } else {
                prefs.ollamaModel.ifBlank { "qwen3.5:4b" }
            }

            val progressMessage = if (isCloud) {
                if (prefs.enableWebGrounding) {
                    "Planning with Gemini & checking real-time travel data..."
                } else {
                    "Planning your itinerary with Gemini AI ($model)..."
                }
            } else {
                "Planning with local AI ($model)..."
            }

            _uiState.value = PlanUiState.GeneratingItinerary(progressMessage)

            generateTripUseCase(
                destination  = destination,
                days         = days,
                budget       = budget,
                interests    = interests,
                travelStyle  = travelStyle,
                startDate    = startDate,
                notes        = notes,
                model        = model,
                adults       = adults,
                kids         = kids
            ).onSuccess { tripId ->
                _uiState.value = PlanUiState.ItineraryGenerated(tripId)
            }.onFailure { e ->
                _uiState.value = PlanUiState.Error(
                    e.message ?: "Failed to generate itinerary. Please check your AI settings and network connection."
                )
            }
        }
    }

    fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch {
            userPreferencesRepository.updatePreferences(transform)
        }
    }

    fun resetState() {
        _uiState.value = PlanUiState.Idle
    }
}

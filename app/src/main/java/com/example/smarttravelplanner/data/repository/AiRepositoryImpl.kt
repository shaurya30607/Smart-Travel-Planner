package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.remote.ai.CloudGeminiEngine
import com.example.smarttravelplanner.data.remote.ai.LocalOllamaEngine
import com.example.smarttravelplanner.data.remote.dto.GeneratedItineraryDto
import com.example.smarttravelplanner.domain.model.DayWeather
import com.example.smarttravelplanner.domain.model.UserTravelPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modular AI repository that delegates itinerary generation and chat requests
 * dynamically to either [CloudGeminiEngine] or [LocalOllamaEngine] based on
 * the user's preferences.
 */
@Singleton
class AiRepositoryImpl @Inject constructor(
    private val cloudGeminiEngine: CloudGeminiEngine,
    private val localOllamaEngine: LocalOllamaEngine,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) : AiRepository {

    override suspend fun generateItinerary(
        destination: String,
        days: Int,
        budget: Double,
        interests: List<String>,
        travelStyle: String,
        startDate: String,
        notes: String,
        model: String,
        weatherForecast: List<DayWeather>?,
        adults: Int,
        kids: Int,
        userPreferences: UserTravelPreferences?
    ): Result<GeneratedItineraryDto> {
        val prefs = userPreferencesRepository.userPreferences.first()
        val resolvedTravelPrefs = userPreferences ?: runCatching {
            val email = authRepository.currentUserEmail.first()?.trim()?.lowercase()
            if (!email.isNullOrBlank()) {
                userPreferencesRepository.getTravelPreferencesOnce(email)
            } else null
        }.getOrNull()

        return if (prefs.aiProvider.equals("LOCAL", ignoreCase = true)) {
            val resolvedModel = if (model.isNotBlank() && !model.startsWith("gemini")) {
                model
            } else {
                prefs.ollamaModel.ifBlank { "qwen3.5:4b" }
            }
            localOllamaEngine.generateItinerary(
                destination        = destination,
                days               = days,
                budget             = budget,
                interests          = interests,
                travelStyle        = travelStyle,
                startDate          = startDate,
                notes              = notes,
                model              = resolvedModel,
                weatherForecast    = weatherForecast,
                enableWebGrounding = false,
                adults             = adults,
                kids               = kids,
                userPreferences    = resolvedTravelPrefs
            )
        } else {
            // Cloud Gemini (Default: gemini-3.1-flash-lite; gemini-3.8-flash if manually chosen)
            val resolvedModel = if (model.isNotBlank() && model.startsWith("gemini")) {
                model
            } else {
                prefs.cloudModel.ifBlank { "gemini-3.1-flash-lite" }
            }
            cloudGeminiEngine.generateItinerary(
                destination        = destination,
                days               = days,
                budget             = budget,
                interests          = interests,
                travelStyle        = travelStyle,
                startDate          = startDate,
                notes              = notes,
                model              = resolvedModel,
                weatherForecast    = weatherForecast,
                enableWebGrounding = prefs.enableWebGrounding,
                adults             = adults,
                kids               = kids,
                userPreferences    = resolvedTravelPrefs
            )
        }
    }

    override suspend fun chat(userMessage: String, model: String): Result<String> {
        val prefs = userPreferencesRepository.userPreferences.first()

        return if (prefs.aiProvider.equals("LOCAL", ignoreCase = true)) {
            val resolvedModel = if (model.isNotBlank() && !model.startsWith("gemini")) {
                model
            } else {
                prefs.ollamaModel.ifBlank { "qwen3.5:4b" }
            }
            localOllamaEngine.chat(userMessage, resolvedModel)
        } else {
            val resolvedModel = if (model.isNotBlank() && model.startsWith("gemini")) {
                model
            } else {
                prefs.cloudModel.ifBlank { "gemini-3.1-flash-lite" }
            }
            cloudGeminiEngine.chat(userMessage, resolvedModel)
        }
    }

    override suspend fun regenerateActivity(
        destination: String,
        dayNumber: Int,
        currentActivity: com.example.smarttravelplanner.domain.model.Activity,
        otherActivities: List<com.example.smarttravelplanner.domain.model.Activity>,
        instructions: String,
        model: String,
        tripInterests: List<String>,
        userPreferences: UserTravelPreferences?
    ): Result<com.example.smarttravelplanner.data.remote.dto.ActivityDto> {
        val prefs = userPreferencesRepository.userPreferences.first()
        val resolvedTravelPrefs = userPreferences ?: runCatching {
            val email = authRepository.currentUserEmail.first()?.trim()?.lowercase()
            if (!email.isNullOrBlank()) {
                userPreferencesRepository.getTravelPreferencesOnce(email)
            } else null
        }.getOrNull()

        return if (prefs.aiProvider.equals("LOCAL", ignoreCase = true)) {
            val resolvedModel = if (model.isNotBlank() && !model.startsWith("gemini")) {
                model
            } else {
                prefs.ollamaModel.ifBlank { "qwen3.5:4b" }
            }
            localOllamaEngine.regenerateActivity(
                destination = destination,
                dayNumber = dayNumber,
                currentActivity = currentActivity,
                otherActivities = otherActivities,
                instructions = instructions,
                model = resolvedModel,
                tripInterests = tripInterests,
                userPreferences = resolvedTravelPrefs
            )
        } else {
            val resolvedModel = if (model.isNotBlank() && model.startsWith("gemini")) {
                model
            } else {
                prefs.cloudModel.ifBlank { "gemini-3.1-flash-lite" }
            }
            cloudGeminiEngine.regenerateActivity(
                destination = destination,
                dayNumber = dayNumber,
                currentActivity = currentActivity,
                otherActivities = otherActivities,
                instructions = instructions,
                model = resolvedModel,
                tripInterests = tripInterests,
                userPreferences = resolvedTravelPrefs
            )
        }
    }
}


package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.remote.ai.CloudGeminiEngine
import com.example.smarttravelplanner.data.remote.ai.LocalOllamaEngine
import com.example.smarttravelplanner.data.remote.dto.GeneratedItineraryDto
import com.example.smarttravelplanner.domain.model.DayWeather
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
    private val userPreferencesRepository: UserPreferencesRepository
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
        kids: Int
    ): Result<GeneratedItineraryDto> {
        val prefs = userPreferencesRepository.userPreferences.first()

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
                kids               = kids
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
                kids               = kids
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
}

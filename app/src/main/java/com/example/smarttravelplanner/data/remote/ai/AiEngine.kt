package com.example.smarttravelplanner.data.remote.ai

import com.example.smarttravelplanner.data.remote.dto.GeneratedItineraryDto
import com.example.smarttravelplanner.domain.model.DayWeather
import com.example.smarttravelplanner.domain.model.UserTravelPreferences

/**
 * Common abstraction for AI itinerary generation engines (Local Ollama vs Cloud Gemini).
 */
interface AiEngine {
    val providerType: String

    suspend fun generateItinerary(
        destination: String,
        days: Int,
        budget: Double,
        interests: List<String>,
        travelStyle: String = "balanced",
        startDate: String = "",
        notes: String = "",
        model: String,
        weatherForecast: List<DayWeather>? = null,
        enableWebGrounding: Boolean = false,
        adults: Int = 1,
        kids: Int = 0,
        userPreferences: UserTravelPreferences? = null,
        /**
         * Pre-formatted passages from the curated knowledge base (TripWeaver RAG).
         * Empty string means no retrieval happened and the prompt is unchanged.
         */
        retrievedContext: String = ""
    ): Result<GeneratedItineraryDto>

    suspend fun chat(userMessage: String, model: String): Result<String>

    suspend fun regenerateActivity(
        destination: String,
        dayNumber: Int,
        currentActivity: com.example.smarttravelplanner.domain.model.Activity,
        otherActivities: List<com.example.smarttravelplanner.domain.model.Activity>,
        instructions: String = "",
        model: String,
        tripInterests: List<String> = emptyList(),
        userPreferences: UserTravelPreferences? = null
    ): Result<com.example.smarttravelplanner.data.remote.dto.ActivityDto>
}


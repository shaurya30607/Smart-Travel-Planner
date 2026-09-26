package com.example.smarttravelplanner.data.remote.ai

import com.example.smarttravelplanner.data.remote.dto.GeneratedItineraryDto
import com.example.smarttravelplanner.domain.model.DayWeather

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
        kids: Int = 0
    ): Result<GeneratedItineraryDto>

    suspend fun chat(userMessage: String, model: String): Result<String>
}

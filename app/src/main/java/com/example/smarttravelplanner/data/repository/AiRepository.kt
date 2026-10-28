package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.remote.dto.GeneratedItineraryDto
import com.example.smarttravelplanner.domain.model.DayWeather
import com.example.smarttravelplanner.domain.model.UserTravelPreferences

interface AiRepository {
    /**
     * Generates a structured travel itinerary using Ollama or Gemini in JSON mode.
     *
     * The response is a fully parsed [GeneratedItineraryDto] with day-by-day
     * activities, place names, coordinates, timings and cost estimates.
     *
     * @param destination      Target travel destination (e.g. "Rajasthan, India")
     * @param days             Number of days for the trip
     * @param budget           Total budget in INR
     * @param interests        List of user interests (e.g. ["Heritage", "Food"])
     * @param travelStyle      Pacing preference: "relaxed" | "balanced" | "packed"
     * @param startDate        ISO-8601 start date (e.g. "2024-10-15")
     * @param notes            Additional user instructions for the AI
     * @param model            AI model name
     * @param weatherForecast  Optional forecasted weather conditions for the trip dates
     * @param userPreferences  Optional account-scoped saved travel style preferences
     */
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
        adults: Int = 1,
        kids: Int = 0,
        userPreferences: UserTravelPreferences? = null,
        /** Pre-formatted knowledge-base passages (TripWeaver RAG); empty when retrieval is off. */
        retrievedContext: String = ""
    ): Result<GeneratedItineraryDto>

    suspend fun chat(userMessage: String, model: String): Result<String>

    suspend fun regenerateActivity(
        destination: String,
        dayNumber: Int,
        currentActivity: com.example.smarttravelplanner.domain.model.Activity,
        otherActivities: List<com.example.smarttravelplanner.domain.model.Activity>,
        instructions: String = "",
        model: String = "",
        tripInterests: List<String> = emptyList(),
        userPreferences: UserTravelPreferences? = null
    ): Result<com.example.smarttravelplanner.data.remote.dto.ActivityDto>
}


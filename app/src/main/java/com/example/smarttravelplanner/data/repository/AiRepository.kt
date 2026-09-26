package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.remote.dto.GeneratedItineraryDto
import com.example.smarttravelplanner.domain.model.DayWeather

interface AiRepository {
    /**
     * Generates a structured travel itinerary using Ollama in JSON mode.
     *
     * The response is a fully parsed [GeneratedItineraryDto] with day-by-day
     * activities, place names, coordinates, timings and cost estimates.
     * In Track B, real place data from Google Places will be injected into
     * the prompt before calling Ollama, so all coordinates will be verified.
     *
     * @param destination      Target travel destination (e.g. "Rajasthan, India")
     * @param days             Number of days for the trip
     * @param budget           Total budget in INR
     * @param interests        List of user interests (e.g. ["Heritage", "Food"])
     * @param travelStyle      Pacing preference: "relaxed" | "balanced" | "packed"
     * @param startDate        ISO-8601 start date (e.g. "2024-10-15")
     * @param notes            Additional user instructions for the AI
     * @param model            Ollama model name (default "llama3.2")
     * @param weatherForecast  Optional forecasted weather conditions for the trip dates
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
        kids: Int = 0
    ): Result<GeneratedItineraryDto>

    /**
     * Sends a freeform chat message to the AI (for the AI assistant / explore screen).
     */
    suspend fun chat(userMessage: String, model: String): Result<String>
}

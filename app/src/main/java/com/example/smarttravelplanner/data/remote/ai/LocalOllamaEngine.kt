package com.example.smarttravelplanner.data.remote.ai

import com.example.smarttravelplanner.data.remote.api.OllamaApi
import com.example.smarttravelplanner.data.remote.dto.GeneratedItineraryDto
import com.example.smarttravelplanner.data.remote.dto.OllamaChatRequest
import com.example.smarttravelplanner.data.remote.dto.OllamaMessage
import com.example.smarttravelplanner.domain.model.DayWeather
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalOllamaEngine @Inject constructor(
    private val ollamaApi: OllamaApi,
    private val gson: Gson
) : AiEngine {

    override val providerType: String = "LOCAL"

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
        enableWebGrounding: Boolean,
        adults: Int,
        kids: Int
    ): Result<GeneratedItineraryDto> = withContext(Dispatchers.IO) {
        runCatching {
            val userMessage = ItineraryPromptBuilder.buildUserMessage(
                destination        = destination,
                days               = days,
                budget             = budget,
                interests          = interests,
                travelStyle        = travelStyle,
                startDate          = startDate,
                notes              = notes,
                weatherForecast    = weatherForecast,
                enableWebGrounding = false, // Ollama has no built-in Google Search grounding
                adults             = adults,
                kids               = kids
            )

            val response = ollamaApi.chat(
                OllamaChatRequest(
                    model    = model.ifBlank { "qwen3.5:4b" },
                    messages = listOf(
                        OllamaMessage(role = "system", content = ItineraryPromptBuilder.itinerarySystemPrompt),
                        OllamaMessage(role = "user",   content = userMessage)
                    ),
                    stream  = false,
                    format  = "json",
                    think   = false,
                    options = mapOf(
                        "num_ctx" to 8192,
                        "num_predict" to 4096,
                        "temperature" to 0.7
                    )
                )
            )

            val raw = (response.message?.content?.takeIf { it.isNotBlank() }
                ?: response.message?.thinking?.takeIf { it.isNotBlank() })
                ?: error("Empty response from Ollama model '$model'. Ensure Ollama is running and model is downloaded.")

            ItineraryJsonParser.parse(raw, model, gson)
        }
    }

    override suspend fun chat(userMessage: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val response = ollamaApi.chat(
                OllamaChatRequest(
                    model    = model.ifBlank { "qwen3.5:4b" },
                    messages = listOf(
                        OllamaMessage(role = "system", content = ItineraryPromptBuilder.chatSystemPrompt),
                        OllamaMessage(role = "user",   content = userMessage)
                    ),
                    stream = false,
                    think  = false
                )
            )
            val content = (response.message?.content?.takeIf { it.isNotBlank() }
                ?: response.message?.thinking?.takeIf { it.isNotBlank() })
                ?: error("Empty response from Ollama model '$model'")

            content.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
        }
    }
}

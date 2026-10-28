package com.example.smarttravelplanner.data.remote.ai

import com.example.smarttravelplanner.BuildConfig
import com.example.smarttravelplanner.data.remote.api.GeminiApi
import com.example.smarttravelplanner.data.remote.dto.*
import com.example.smarttravelplanner.data.repository.UserPreferencesRepository
import com.example.smarttravelplanner.domain.model.DayWeather
import com.example.smarttravelplanner.domain.model.UserTravelPreferences
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudGeminiEngine @Inject constructor(
    private val geminiApi: GeminiApi,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val gson: Gson
) : AiEngine {

    override val providerType: String = "CLOUD"

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
        kids: Int,
        userPreferences: UserTravelPreferences?
    ): Result<GeneratedItineraryDto> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = userPreferencesRepository.userPreferences.first()

            val apiKey = prefs.cloudApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }.trim()
            if (apiKey.isBlank()) {
                throw IllegalStateException(
                    "Gemini API key is missing. Please set your Gemini API key in Profile -> Settings, or add GEMINI_API_KEY to local.properties."
                )
            }

            // Default model is gemini-2.0-flash-lite unless explicitly specified
            val resolvedModel = model.ifBlank {
                prefs.cloudModel.ifBlank { "gemini-3.1-flash-lite" }
            }.trim()

            val isWebGroundingEnabled = enableWebGrounding || prefs.enableWebGrounding

            val userMessage = ItineraryPromptBuilder.buildUserMessage(
                destination        = destination,
                days               = days,
                budget             = budget,
                interests          = interests,
                travelStyle        = travelStyle,
                startDate          = startDate,
                notes              = notes,
                weatherForecast    = weatherForecast,
                enableWebGrounding = isWebGroundingEnabled,
                adults             = adults,
                kids               = kids,
                userPreferences    = userPreferences
            )

            val tools = if (isWebGroundingEnabled) {
                listOf(GeminiTool(googleSearch = GeminiGoogleSearch()))
            } else null

            val request = GeminiGenerateRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = ItineraryPromptBuilder.itinerarySystemPrompt))
                ),
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = userMessage))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    // Google forbids responseMimeType when Search Grounding tools are active
                    responseMimeType = if (isWebGroundingEnabled) null else "application/json",
                    temperature = 0.7
                ),
                tools = tools
            )

            var rawResponse = geminiApi.generateContent(
                model = resolvedModel,
                apiKey = apiKey,
                request = request
            )

            // If 429 (quota exceeded) occurred with Web Grounding, automatically retry without Google Search tool.
            // On unbilled/Free-Tier Gemini API keys, Grounding with Google Search has a separate 0/restricted quota.
            if (!rawResponse.isSuccessful && rawResponse.code() == 429 && isWebGroundingEnabled) {
                val fallbackRequest = request.copy(
                    tools = null,
                    generationConfig = request.generationConfig?.copy(
                        responseMimeType = "application/json"
                    )
                )
                val fallbackResponse = geminiApi.generateContent(
                    model = resolvedModel,
                    apiKey = apiKey,
                    request = fallbackRequest
                )
                if (fallbackResponse.isSuccessful) {
                    rawResponse = fallbackResponse
                }
            }

            if (!rawResponse.isSuccessful) {
                val errorBody = rawResponse.errorBody()?.string() ?: "(no error body)"
                throw IllegalStateException(
                    "Gemini API error HTTP ${rawResponse.code()} for model '$resolvedModel': $errorBody"
                )
            }
            val response = rawResponse.body()
                ?: throw IllegalStateException("Gemini returned an empty response body.")

            val candidate = response.candidates?.firstOrNull()
                ?: throw IllegalStateException("Gemini model '$resolvedModel' returned no candidate response.")

            val rawText = candidate.content?.parts?.joinToString("\n") { it.text }?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Gemini model '$resolvedModel' returned an empty content body.")

            ItineraryJsonParser.parse(rawText, resolvedModel, gson)
        }
    }

    override suspend fun chat(userMessage: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = userPreferencesRepository.userPreferences.first()
            val apiKey = prefs.cloudApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }.trim()
            if (apiKey.isBlank()) {
                throw IllegalStateException("Gemini API key is missing. Please set your API key in Settings.")
            }

            val resolvedModel = model.ifBlank {
                prefs.cloudModel.ifBlank { "gemini-3.1-flash-lite" }
            }.trim()

            val request = GeminiGenerateRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = ItineraryPromptBuilder.chatSystemPrompt))
                ),
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = userMessage))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = null,
                    temperature = 0.7
                )
            )

            val rawResponse = geminiApi.generateContent(
                model = resolvedModel,
                apiKey = apiKey,
                request = request
            )
            if (!rawResponse.isSuccessful) {
                val errorBody = rawResponse.errorBody()?.string() ?: "(no error body)"
                throw IllegalStateException(
                    "Gemini API error HTTP ${rawResponse.code()} for model '$resolvedModel': $errorBody"
                )
            }
            val response = rawResponse.body()
                ?: throw IllegalStateException("Gemini returned an empty response body.")

            val text = response.candidates?.firstOrNull()?.content?.parts?.joinToString("\n") { it.text }
                ?: throw IllegalStateException("Empty response from Gemini")

            text.trim()
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
    ): Result<ActivityDto> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = userPreferencesRepository.userPreferences.first()
            val apiKey = prefs.cloudApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }.trim()
            if (apiKey.isBlank()) {
                throw IllegalStateException("Gemini API key is missing. Please set your API key in Settings.")
            }

            val resolvedModel = model.ifBlank {
                prefs.cloudModel.ifBlank { "gemini-3.1-flash-lite" }
            }.trim()

            val userMessage = ItineraryPromptBuilder.buildActivityRegenerationMessage(
                destination = destination,
                dayNumber = dayNumber,
                currentActivity = currentActivity,
                otherActivities = otherActivities,
                instructions = instructions,
                tripInterests = tripInterests,
                userPreferences = userPreferences
            )

            val request = GeminiGenerateRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = ItineraryPromptBuilder.singleActivitySystemPrompt))
                ),
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = userMessage))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.5
                )
            )

            val rawResponse = geminiApi.generateContent(
                model = resolvedModel,
                apiKey = apiKey,
                request = request
            )
            if (!rawResponse.isSuccessful) {
                val errorBody = rawResponse.errorBody()?.string() ?: "(no error body)"
                throw IllegalStateException("Gemini error HTTP ${rawResponse.code()}: $errorBody")
            }
            val response = rawResponse.body()
                ?: throw IllegalStateException("Gemini returned an empty response body.")

            val text = response.candidates?.firstOrNull()?.content?.parts?.joinToString("\n") { it.text }
                ?: throw IllegalStateException("Empty response from Gemini")

            ItineraryJsonParser.parseSingleActivity(text, resolvedModel, gson)
        }
    }
}


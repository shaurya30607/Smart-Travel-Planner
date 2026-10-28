package com.example.smarttravelplanner.domain.usecase

import com.example.smarttravelplanner.data.remote.dto.ActivityDto
import com.example.smarttravelplanner.data.repository.AiRepository
import com.example.smarttravelplanner.data.repository.AuthRepository
import com.example.smarttravelplanner.data.repository.ItineraryRepository
import com.example.smarttravelplanner.data.repository.PhotoRepository
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.data.repository.UserPreferencesRepository
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.ActivityCategory
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import com.example.smarttravelplanner.domain.model.UserTravelPreferences
import com.example.smarttravelplanner.util.DestinationImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import com.example.smarttravelplanner.data.rag.RagSourceRegistry
import com.example.smarttravelplanner.data.remote.ai.ItineraryPromptBuilder
import com.example.smarttravelplanner.domain.model.RagContext
import javax.inject.Inject

/**
 * Orchestrates the full "Generate + Save" flow:
 *  1. Calls AiRepository.generateItinerary() → structured GeneratedItineraryDto
 *  2. Resolves cover image URL from Wikipedia if no local bundled image exists
 *  3. Saves the Trip to Room → gets a real tripId
 *  4. Maps Dto activities to domain Activity objects, assigns tripId
 *  5. Saves all Itinerary days to Room
 *  6. Returns Result<Long> (the new tripId, used for navigation to TripDetail)
 */
class GenerateTripUseCase @Inject constructor(
    /**
     * TripWeaver RAG retrieval. Optional on purpose: Hilt always supplies the real
     * retriever in the app, while existing unit tests that construct this use case
     * directly can leave it out and get exactly the pre-RAG behaviour.
     */
    private val retrieveTravelContextUseCase: RetrieveTravelContextUseCase? = null,
    private val aiRepository: AiRepository,
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository,
    private val photoRepository: PhotoRepository,
    private val getWeatherForTripUseCase: GetWeatherForTripUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        destination: String,
        days: Int,
        budget: Double,
        interests: List<String>,
        travelStyle: String,
        startDate: LocalDate?,
        notes: String,
        model: String,
        adults: Int = 1,
        kids: Int = 0,
        userPreferences: UserTravelPreferences? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {

        val startDateStr = startDate?.toString() ?: ""
        val resolvedStart = startDate ?: LocalDate.now()
        val resolvedEnd   = resolvedStart.plusDays((days - 1).toLong().coerceAtLeast(0))

        // Step 1 — Pre-fetch weather forecast if available (graceful fallback if offline/out of horizon)
        val weatherForecast = runCatching {
            getWeatherForTripUseCase.getForDestination(
                destination = destination,
                startDate   = resolvedStart,
                endDate     = resolvedEnd
            ).getOrNull()
        }.getOrNull()?.takeIf { it.isNotEmpty() }

        // Step 2 — Resolve account-scoped travel preferences for authenticated traveller
        val resolvedTravelPrefs = userPreferences ?: runCatching {
            val email = authRepository.currentUserEmail.first()?.trim()?.lowercase()
            if (!email.isNullOrBlank()) {
                userPreferencesRepository.getTravelPreferencesOnce(email)
            } else null
        }.getOrNull()

        // Step 2b — TripWeaver RAG: retrieve curated passages for this destination.
        // Returns an empty context for destinations the knowledge base does not cover,
        // in which case the prompt below is exactly what it was before RAG existed.
        val ragContext = retrieveTravelContextUseCase?.invoke(
            destination = destination,
            interests   = interests,
            notes       = notes
        ) ?: RagContext(
            destinationId        = "",
            requestedDestination = destination,
            passages             = emptyList()
        )
        val retrievedContext = ItineraryPromptBuilder.buildRetrievedContextBlock(ragContext.passages)

        // Step 3 — Generate structured weather-aware, personalized itinerary from AI
        val dto = aiRepository.generateItinerary(
            destination     = destination,
            days            = days,
            budget          = budget,
            interests       = interests,
            travelStyle     = travelStyle,
            startDate       = startDateStr,
            notes           = notes,
            model           = model,
            weatherForecast = weatherForecast,
            adults          = adults,
            kids            = kids,
            userPreferences = resolvedTravelPrefs,
            retrievedContext = retrievedContext
        ).getOrThrow()

        // Step 3 — Build Trip domain object and persist it
        val coverImageUrl = if (!DestinationImageHelper.hasLocalDrawable(destination)) {
            photoRepository.getDestinationPhotoUrl(destination).getOrDefault("")
        } else {
            ""
        }

        val trip = Trip(
            title         = "Trip to $destination",
            destination   = destination,
            startDate     = resolvedStart,
            endDate       = resolvedEnd,
            budget        = budget,
            interests     = interests,
            notes         = notes,
            coverImageUrl = coverImageUrl,
            adults        = adults,
            kids          = kids
        )
        val tripId = tripRepository.saveTrip(trip)

        // Step 3 + 4 — Map DTO → domain Itinerary and save
        val itineraries = dto.days.map { dayDto ->
            Itinerary(
                tripId    = tripId,
                dayNumber = dayDto.dayNumber,
                date      = dayDto.date.ifBlank {
                    resolvedStart.plusDays((dayDto.dayNumber - 1).toLong()).toString()
                },
                activities = dayDto.activities.mapIndexed { idx, actDto ->
                    actDto.toDomain(itineraryId = 0L, id = idx.toLong())
                }
            )
        }
        itineraryRepository.saveItineraries(itineraries)

        // Remember which passages informed this trip so the detail screen can cite them.
        RagSourceRegistry.record(tripId, ragContext)

        tripId
    }
}

    private fun ActivityDto.toDomain(itineraryId: Long, id: Long) = Activity(
        id            = id,
        itineraryId   = itineraryId,
        title         = title,
        description   = description,
        location      = address.ifBlank { placeName },
        lat           = lat.takeIf { it != 0.0 },
        lng           = lng.takeIf { it != 0.0 },
        startTime     = startTime,
        endTime       = endTime,
        category      = runCatching { ActivityCategory.valueOf(category) }
                            .getOrDefault(ActivityCategory.ATTRACTION),
        estimatedCost = estimatedCostInr,
        placeId       = placeId,
        weatherNote   = weatherNote
    )
}

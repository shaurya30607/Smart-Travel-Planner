package com.example.smarttravelplanner.domain.usecase

import com.example.smarttravelplanner.data.remote.dto.ActivityDto
import com.example.smarttravelplanner.data.remote.dto.DayDto
import com.example.smarttravelplanner.data.remote.dto.GeneratedItineraryDto
import com.example.smarttravelplanner.data.remote.dto.PlaceResultDto
import com.example.smarttravelplanner.data.repository.AiRepository
import com.example.smarttravelplanner.data.repository.AuthRepository
import com.example.smarttravelplanner.data.repository.ItineraryRepository
import com.example.smarttravelplanner.data.repository.PhotoRepository
import com.example.smarttravelplanner.data.repository.PlacesRepository
import com.example.smarttravelplanner.data.repository.TripEdit
import com.example.smarttravelplanner.data.repository.TripEditRepository
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.data.repository.UserPreferencesRepository
import com.example.smarttravelplanner.data.repository.WeatherRepository
import com.example.smarttravelplanner.domain.model.AuthResult
import com.example.smarttravelplanner.domain.model.DayWeather
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.OnboardingStatus
import com.example.smarttravelplanner.domain.model.Trip
import com.example.smarttravelplanner.domain.model.UserPreferences
import com.example.smarttravelplanner.domain.model.UserTravelPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class PreferenceDataFlowUseCaseTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    private class FakeAuthRepository(initialEmail: String? = null) : AuthRepository {
        val emailFlow = MutableStateFlow(initialEmail)
        override val isLoggedIn: Flow<Boolean> = flowOf(initialEmail != null)
        override val currentUserEmail: Flow<String?> = emailFlow

        override suspend fun login(email: String, password: String): Result<AuthResult> =
            Result.success(AuthResult(email, isNewAccount = false))

        override suspend fun sendRegistrationOtp(email: String): Result<Unit> = Result.success(Unit)

        override suspend fun verifyOtpAndCreateAccount(
            fullName: String,
            email: String,
            password: String,
            otp: String
        ): Result<AuthResult> = Result.success(AuthResult(email, isNewAccount = true))

        override suspend fun loginWithGoogle(
            email: String,
            fullName: String,
            profilePictureUrl: String?
        ): Result<AuthResult> = Result.success(AuthResult(email, isNewAccount = false))

        override suspend fun sendPasswordResetOtp(email: String): Result<Unit> = Result.success(Unit)

        override suspend fun resetPassword(email: String, otp: String, newPassword: String): Result<AuthResult> =
            Result.success(AuthResult(email, isNewAccount = false))

        override suspend fun logout(): Result<Unit> {
            emailFlow.value = null
            return Result.success(Unit)
        }
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        val preferencesByEmail = mutableMapOf<String, UserTravelPreferences>()
        var shouldThrowOnRead: Boolean = false

        override val userPreferences: Flow<UserPreferences> = flowOf(UserPreferences())
        override suspend fun update(prefs: UserPreferences) {}
        override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {}
        override suspend fun updateThemeMode(themeMode: String) {}

        override fun getTravelPreferences(email: String): Flow<UserTravelPreferences> = flowOf(
            preferencesByEmail[email.trim().lowercase()] ?: UserTravelPreferences(email = email)
        )

        override suspend fun getTravelPreferencesOnce(email: String): UserTravelPreferences {
            if (shouldThrowOnRead) throw IOException("Simulated storage read error")
            val normalized = email.trim().lowercase()
            return preferencesByEmail[normalized] ?: UserTravelPreferences(email = normalized)
        }

        override suspend fun getRawOnboardingStatus(email: String): OnboardingStatus? =
            preferencesByEmail[email.trim().lowercase()]?.onboardingStatus

        override suspend fun initializeNewAccountOnboarding(email: String) {
            val normalized = email.trim().lowercase()
            preferencesByEmail[normalized] = UserTravelPreferences(email = normalized, onboardingStatus = OnboardingStatus.NOT_STARTED)
        }

        override suspend fun updateOnboardingStatus(email: String, status: OnboardingStatus) {
            val normalized = email.trim().lowercase()
            val existing = preferencesByEmail[normalized] ?: UserTravelPreferences(email = normalized)
            preferencesByEmail[normalized] = existing.copy(onboardingStatus = status)
        }

        override suspend fun updateTravelPreferences(
            email: String,
            liked: Set<String>,
            stronglyPreferred: Set<String>,
            disliked: Set<String>,
            weights: Map<String, Float>
        ) {
            val normalized = email.trim().lowercase()
            preferencesByEmail[normalized] = UserTravelPreferences(
                email = normalized,
                onboardingStatus = OnboardingStatus.COMPLETED,
                likedInterestIds = liked,
                stronglyPreferredInterestIds = stronglyPreferred,
                dislikedInterestIds = disliked,
                preferenceWeights = weights
            )
        }

        override suspend fun migrateLegacyAccountIfNeeded(email: String): OnboardingStatus =
            OnboardingStatus.LEGACY_COMPLETED

        override suspend fun clearAccountState(email: String) {}
    }

    private class FakeAiRepository : AiRepository {
        var lastCapturedPreferences: UserTravelPreferences? = null
        var lastCapturedDestination: String? = null
        var lastCapturedDays: Int = 0

        /** TripWeaver: the retrieved knowledge-base block handed to the AI layer. */
        var lastCapturedRetrievedContext: String = ""


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
            userPreferences: UserTravelPreferences?,
            retrievedContext: String
        ): Result<GeneratedItineraryDto> {
            lastCapturedPreferences = userPreferences
            lastCapturedDestination = destination
            lastCapturedDays = days
            lastCapturedRetrievedContext = retrievedContext

            // Return a minimal valid GeneratedItineraryDto
            val dummyActivities = listOf(
                ActivityDto(
                    title = "Visit Fort",
                    placeName = "Amber Palace",
                    placeId = "p1",
                    address = "Amer, Jaipur",
                    lat = 26.9855,
                    lng = 75.8513,
                    startTime = "09:00",
                    endTime = "12:00",
                    category = "ATTRACTION",
                    description = "Historic hilltop fort with stunning courtyards.",
                    estimatedCostInr = 500.0,
                    weatherNote = null
                )
            )
            val dayDtos = (1..days).map { dayNum ->
                DayDto(
                    dayNumber = dayNum,
                    date = "2026-11-0$dayNum",
                    theme = "Heritage Exploration",
                    activities = dummyActivities
                )
            }
            return Result.success(GeneratedItineraryDto(days = dayDtos))
        }

        override suspend fun chat(userMessage: String, model: String): Result<String> = Result.success("OK")

        override suspend fun regenerateActivity(
            destination: String,
            dayNumber: Int,
            currentActivity: com.example.smarttravelplanner.domain.model.Activity,
            otherActivities: List<com.example.smarttravelplanner.domain.model.Activity>,
            instructions: String,
            model: String,
            tripInterests: List<String>,
            userPreferences: UserTravelPreferences?
        ): Result<ActivityDto> = Result.success(
            ActivityDto(
                title = "Alternative Stop",
                placeName = "City Cafe",
                placeId = "p2",
                address = "Main St",
                lat = 26.9,
                lng = 75.8,
                startTime = "14:00",
                endTime = "15:00",
                category = "FOOD",
                description = "Authentic chai stop",
                estimatedCostInr = 150.0,
                weatherNote = null
            )
        )
    }

    private class FakeTripRepository : TripRepository {
        val trips = mutableMapOf<Long, Trip>()
        private var idCounter = 1L

        override suspend fun saveTrip(trip: Trip): Long {
            val id = if (trip.id == 0L) idCounter++ else trip.id
            trips[id] = trip.copy(id = id)
            return id
        }

        override suspend fun getTripById(id: Long): Trip? = trips[id]
        override fun getAllTrips(): Flow<List<Trip>> = flowOf(trips.values.toList())
        override suspend fun updateTrip(trip: Trip) { trips[trip.id] = trip }
        override suspend fun deleteTrip(id: Long) { trips.remove(id) }
    }

    private class FakeItineraryRepository : ItineraryRepository {
        val itineraries = mutableListOf<Itinerary>()

        override suspend fun saveItineraries(itineraries: List<Itinerary>) {
            this.itineraries.addAll(itineraries)
        }

        override fun getItinerariesForTripAsFlow(tripId: Long): Flow<List<Itinerary>> =
            flowOf(itineraries.filter { it.tripId == tripId })

        override suspend fun getItinerariesForTrip(tripId: Long): List<Itinerary> =
            itineraries.filter { it.tripId == tripId }

        override suspend fun deleteItinerariesForTrip(tripId: Long) {
            itineraries.removeAll { it.tripId == tripId }
        }
    }

    private class FakePhotoRepository : PhotoRepository {
        override suspend fun getDestinationPhotoUrl(destination: String): Result<String> = Result.success("")
    }

    private class FakeTripEditRepository : TripEditRepository {
        override suspend fun saveEdits(
            trip: Trip,
            days: List<Itinerary>,
            removedDayIds: List<Long>,
            summaries: List<String>
        ): Result<Unit> = Result.success(Unit)

        override fun observeEdits(tripId: Long): Flow<List<TripEdit>> = flowOf(emptyList())
    }

    private fun createWeatherUseCase(): GetWeatherForTripUseCase {
        val weatherRepo = object : WeatherRepository {
            override suspend fun getForecast(lat: Double, lng: Double, startDate: String, endDate: String): Result<List<DayWeather>> =
                Result.success(emptyList())
        }
        val placesRepo = object : PlacesRepository {
            override suspend fun searchPlacesNearby(destination: String, latitude: Double?, longitude: Double?): Result<List<PlaceResultDto>> =
                Result.success(emptyList())
            override suspend fun geocodeDestination(destination: String): Result<Pair<Double, Double>> =
                Result.success(Pair(28.6139, 77.2090))
        }
        return GetWeatherForTripUseCase(weatherRepo, placesRepo)
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun correctAccountSpecificPreferenceRetrievalWithoutCrossAccountLeakage() = runBlocking {
        val authRepo = FakeAuthRepository("alice@example.com")
        val userPrefsRepo = FakeUserPreferencesRepository()
        val aiRepo = FakeAiRepository()

        // Configure User A's preferences
        userPrefsRepo.updateTravelPreferences(
            email = "alice@example.com",
            liked = setOf("beaches_islands"),
            stronglyPreferred = setOf("beaches_islands"),
            disliked = setOf("mountains_trekking"),
            weights = mapOf("beaches_islands" to 2.0f, "mountains_trekking" to -1.0f)
        )

        // Configure User B's preferences
        userPrefsRepo.updateTravelPreferences(
            email = "bob@example.com",
            liked = setOf("mountains_trekking"),
            stronglyPreferred = setOf("mountains_trekking"),
            disliked = setOf("beaches_islands"),
            weights = mapOf("mountains_trekking" to 2.0f, "beaches_islands" to -1.0f)
        )

        val useCase = GenerateTripUseCase(
            aiRepository = aiRepo,
            tripRepository = FakeTripRepository(),
            itineraryRepository = FakeItineraryRepository(),
            photoRepository = FakePhotoRepository(),
            getWeatherForTripUseCase = createWeatherUseCase(),
            userPreferencesRepository = userPrefsRepo,
            authRepository = authRepo
        )

        // 1. Generate trip while Alice is authenticated
        val aliceResult = useCase(
            destination = "Goa",
            days = 3,
            budget = 30000.0,
            interests = listOf("Water sports"),
            travelStyle = "relaxed",
            startDate = LocalDate.of(2026, 11, 1),
            notes = "",
            model = "gemini-3.1-flash-lite"
        )
        assertTrue(aliceResult.isSuccess)
        assertNotNull(aiRepo.lastCapturedPreferences)
        assertEquals("alice@example.com", aiRepo.lastCapturedPreferences?.email)
        assertTrue(aiRepo.lastCapturedPreferences!!.stronglyPreferredInterestIds.contains("beaches_islands"))
        assertFalse(aiRepo.lastCapturedPreferences!!.stronglyPreferredInterestIds.contains("mountains_trekking"))

        // 2. Switch authenticated user to Bob
        authRepo.emailFlow.value = "bob@example.com"
        val bobResult = useCase(
            destination = "Manali",
            days = 4,
            budget = 40000.0,
            interests = listOf("Trekking"),
            travelStyle = "packed",
            startDate = LocalDate.of(2026, 12, 1),
            notes = "",
            model = "gemini-3.1-flash-lite"
        )
        assertTrue(bobResult.isSuccess)
        assertNotNull(aiRepo.lastCapturedPreferences)
        assertEquals("bob@example.com", aiRepo.lastCapturedPreferences?.email)
        assertTrue(aiRepo.lastCapturedPreferences!!.stronglyPreferredInterestIds.contains("mountains_trekking"))
        assertFalse(aiRepo.lastCapturedPreferences!!.stronglyPreferredInterestIds.contains("beaches_islands"))
    }

    @Test
    fun mixedCaseEmailNormalizationResolvesCorrectPreferences() = runBlocking {
        val authRepo = FakeAuthRepository("Alice.Traveler@Example.COM")
        val userPrefsRepo = FakeUserPreferencesRepository()
        val aiRepo = FakeAiRepository()

        userPrefsRepo.updateTravelPreferences(
            email = "alice.traveler@example.com",
            liked = setOf("local_offbeat"),
            stronglyPreferred = setOf("local_offbeat"),
            disliked = emptySet(),
            weights = mapOf("local_offbeat" to 2.0f)
        )

        val useCase = GenerateTripUseCase(
            aiRepository = aiRepo,
            tripRepository = FakeTripRepository(),
            itineraryRepository = FakeItineraryRepository(),
            photoRepository = FakePhotoRepository(),
            getWeatherForTripUseCase = createWeatherUseCase(),
            userPreferencesRepository = userPrefsRepo,
            authRepository = authRepo
        )

        val result = useCase(
            destination = "Meghalaya",
            days = 5,
            budget = 45000.0,
            interests = listOf("Root bridges"),
            travelStyle = "balanced",
            startDate = LocalDate.of(2026, 10, 20),
            notes = "",
            model = "gemini-3.1-flash-lite"
        )
        assertTrue(result.isSuccess)
        assertEquals("alice.traveler@example.com", aiRepo.lastCapturedPreferences?.email)
        assertTrue(aiRepo.lastCapturedPreferences!!.stronglyPreferredInterestIds.contains("local_offbeat"))
    }

    @Test
    fun emptyPreferencesAndRepositoryFailuresDegradeGracefully() = runBlocking {
        val authRepo = FakeAuthRepository("unlucky@example.com")
        val userPrefsRepo = FakeUserPreferencesRepository()
        userPrefsRepo.shouldThrowOnRead = true // Simulate IO / corrupted storage failure
        val aiRepo = FakeAiRepository()

        val useCase = GenerateTripUseCase(
            aiRepository = aiRepo,
            tripRepository = FakeTripRepository(),
            itineraryRepository = FakeItineraryRepository(),
            photoRepository = FakePhotoRepository(),
            getWeatherForTripUseCase = createWeatherUseCase(),
            userPreferencesRepository = userPrefsRepo,
            authRepository = authRepo
        )

        // Trip generation should still succeed without crashing, falling back to null preferences
        val result = useCase(
            destination = "Kerala",
            days = 4,
            budget = 35000.0,
            interests = listOf("Backwaters"),
            travelStyle = "relaxed",
            startDate = LocalDate.of(2026, 11, 15),
            notes = "",
            model = "gemini-3.1-flash-lite"
        )
        assertTrue("Trip generation must succeed even if preference reading fails", result.isSuccess)
        assertNull("Preferences should degrade gracefully to null on repository failure", aiRepo.lastCapturedPreferences)
    }

    @Test
    fun unauthenticatedUserGeneratesTripWithNullPreferences() = runBlocking {
        val authRepo = FakeAuthRepository(null) // Not logged in
        val userPrefsRepo = FakeUserPreferencesRepository()
        val aiRepo = FakeAiRepository()

        val useCase = GenerateTripUseCase(
            aiRepository = aiRepo,
            tripRepository = FakeTripRepository(),
            itineraryRepository = FakeItineraryRepository(),
            photoRepository = FakePhotoRepository(),
            getWeatherForTripUseCase = createWeatherUseCase(),
            userPreferencesRepository = userPrefsRepo,
            authRepository = authRepo
        )

        val result = useCase(
            destination = "Agra",
            days = 2,
            budget = 15000.0,
            interests = listOf("Taj Mahal"),
            travelStyle = "balanced",
            startDate = LocalDate.of(2026, 12, 1),
            notes = "",
            model = "gemini-3.1-flash-lite"
        )
        assertTrue(result.isSuccess)
        assertNull(aiRepo.lastCapturedPreferences)
    }

    @Test
    fun planEmptyDaysUseCaseIncorporatesPersonalizedPreferences() = runBlocking {
        val authRepo = FakeAuthRepository("planner@example.com")
        val userPrefsRepo = FakeUserPreferencesRepository()
        val aiRepo = FakeAiRepository()
        val tripRepo = FakeTripRepository()
        val itineraryRepo = FakeItineraryRepository()

        userPrefsRepo.updateTravelPreferences(
            email = "planner@example.com",
            liked = setOf("history_architecture"),
            stronglyPreferred = setOf("history_architecture"),
            disliked = setOf("road_trips"),
            weights = mapOf("history_architecture" to 2.0f, "road_trips" to -1.0f)
        )

        // Create a 3-day trip where Day 1 and 2 have activities, and Day 3 is empty
        val tripId = tripRepo.saveTrip(
            Trip(
                title = "Trip to Delhi",
                destination = "Delhi",
                startDate = LocalDate.of(2026, 11, 1),
                endDate = LocalDate.of(2026, 11, 3),
                budget = 30000.0,
                interests = listOf("Monuments")
            )
        )

        itineraryRepo.saveItineraries(
            listOf(
                Itinerary(
                    id = 1L,
                    tripId = tripId,
                    dayNumber = 1,
                    date = "2026-11-01",
                    activities = listOf(
                        com.example.smarttravelplanner.domain.model.Activity(
                            id = 101L,
                            itineraryId = 1L,
                            title = "Red Fort Tour",
                            description = "Mughal fort complex",
                            location = "Netaji Subhash Marg",
                            startTime = "10:00",
                            endTime = "13:00",
                            category = com.example.smarttravelplanner.domain.model.ActivityCategory.ATTRACTION,
                            estimatedCost = 250.0
                        )
                    )
                ),
                Itinerary(
                    id = 2L,
                    tripId = tripId,
                    dayNumber = 2,
                    date = "2026-11-02",
                    activities = listOf(
                        com.example.smarttravelplanner.domain.model.Activity(
                            id = 102L,
                            itineraryId = 2L,
                            title = "Humayun's Tomb",
                            description = "Garden tomb monument",
                            location = "Nizamuddin East",
                            startTime = "14:00",
                            endTime = "17:00",
                            category = com.example.smarttravelplanner.domain.model.ActivityCategory.ATTRACTION,
                            estimatedCost = 250.0
                        )
                    )
                ),
                Itinerary(
                    id = 3L,
                    tripId = tripId,
                    dayNumber = 3,
                    date = "2026-11-03",
                    activities = emptyList() // Empty target day
                )
            )
        )

        val planEmptyDaysUseCase = PlanEmptyDaysUseCase(
            tripRepository = tripRepo,
            itineraryRepository = itineraryRepo,
            aiRepository = aiRepo,
            userPreferencesRepository = userPrefsRepo,
            getWeatherForTripUseCase = createWeatherUseCase(),
            tripEditRepository = FakeTripEditRepository(),
            authRepository = authRepo
        )

        val result = planEmptyDaysUseCase(tripId)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()) // 1 day planned

        // Verify that empty-day AI generation received the account preferences
        assertNotNull(aiRepo.lastCapturedPreferences)
        assertEquals("planner@example.com", aiRepo.lastCapturedPreferences?.email)
        assertTrue(aiRepo.lastCapturedPreferences!!.stronglyPreferredInterestIds.contains("history_architecture"))
        assertTrue(aiRepo.lastCapturedPreferences!!.dislikedInterestIds.contains("road_trips"))
    }
}

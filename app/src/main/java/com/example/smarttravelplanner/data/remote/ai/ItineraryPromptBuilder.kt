package com.example.smarttravelplanner.data.remote.ai

import com.example.smarttravelplanner.domain.model.DayWeather
import com.example.smarttravelplanner.domain.model.UserTravelPreferences
import com.example.smarttravelplanner.data.rag.RetrievedChunk
import com.example.smarttravelplanner.domain.util.WeatherPromptFormatter

/**
 * Shared prompt builder for itinerary generation across both Cloud and Local AI engines.
 * Ensures consistent schema compliance, weather awareness, geocoded map coordinates,
 * category-based budget allocation, and personalized travel style preferences.
 */
object ItineraryPromptBuilder {

    /**
     * Exact natural language mapping for the 12 onboarding card IDs.
     */
    val PREFERENCE_ID_TO_NATURAL_LANGUAGE: Map<String, String> = mapOf(
        "adventure" to "adventure activities",
        "relaxation" to "relaxing experiences",
        "nature_wildlife" to "nature and wildlife",
        "culture_heritage" to "culture and heritage",
        "food_cafes" to "food and cafés",
        "history_architecture" to "history and architecture",
        "shopping_city_life" to "shopping and city life",
        "photography_scenic" to "photography and scenic views",
        "road_trips" to "road trips",
        "beaches_islands" to "beaches and islands",
        "mountains_trekking" to "mountains and trekking",
        "local_offbeat" to "local and offbeat experiences"
    )

    fun formatPreferencesContext(preferences: UserTravelPreferences?): String {
        if (preferences == null) return ""

        val strongIds = (preferences.stronglyPreferredInterestIds +
            preferences.preferenceWeights.filterValues { it >= 1.5f }.keys).toSet()

        val likedIds = ((preferences.likedInterestIds - strongIds) +
            preferences.preferenceWeights.filterValues { it in 0.5f..1.4f }.keys - strongIds).toSet()

        val dislikedIds = (preferences.dislikedInterestIds +
            preferences.preferenceWeights.filterValues { it < 0f }.keys).toSet()

        if (strongIds.isEmpty() && likedIds.isEmpty() && dislikedIds.isEmpty()) {
            return ""
        }

        val strongText = strongIds.map { PREFERENCE_ID_TO_NATURAL_LANGUAGE[it] ?: it.replace('_', ' ') }
        val likedText = likedIds.map { PREFERENCE_ID_TO_NATURAL_LANGUAGE[it] ?: it.replace('_', ' ') }
        val dislikedText = dislikedIds.map { PREFERENCE_ID_TO_NATURAL_LANGUAGE[it] ?: it.replace('_', ' ') }

        val answeredIds = strongIds + likedIds + dislikedIds
        val unansweredIds = PREFERENCE_ID_TO_NATURAL_LANGUAGE.keys - answeredIds
        val unansweredText = unansweredIds.mapNotNull { PREFERENCE_ID_TO_NATURAL_LANGUAGE[it] }

        return buildString {
            appendLine()
            appendLine("TRAVELLER SAVED PREFERENCES (ONBOARDING PROFILE):")
            appendLine("The traveller has saved baseline travel style preferences. Personalize the itinerary to reflect these preferences:")
            if (strongText.isNotEmpty()) {
                appendLine("- STRONG PREFERENCES (+2.0 Weight - High Priority): Prioritize and emphasize relevant experiences featuring ${strongText.joinToString(", ")}.")
            }
            if (likedText.isNotEmpty()) {
                appendLine("- LIKES (+1.0 Weight - Favorable): Include relevant experiences featuring ${likedText.joinToString(", ")} where appropriate.")
            }
            if (dislikedText.isNotEmpty()) {
                appendLine("- DISLIKES (-1.0 Weight - De-prioritize / Soft Avoidance): Reduce the frequency of experiences featuring ${dislikedText.joinToString(", ")}. Do not treat as absolute exclusions if essential to the destination, but favor alternatives.")
            }
            if (unansweredText.isNotEmpty()) {
                appendLine("- UNANSWERED INTERESTS (Neutral baseline): ${unansweredText.joinToString(", ")} (neutral; neither favored nor avoided).")
            } else {
                appendLine("- UNANSWERED INTERESTS: Neutral baseline.")
            }
            appendLine()
            appendLine("CONSTRAINT INTEGRITY & PREFERENCE HIERARCHY:")
            appendLine("- Explicit trip parameters (destination, duration, dates, budget, traveler count, accessibility, and user special instructions) take STRICT precedence and must NEVER be overridden by these preferences.")
            appendLine("- If explicit trip interests or user special instructions request specific activities or themes, satisfy those explicit requirements first.")
        }.trimEnd()
    }

    /**
     * Extra guidance when one adult travels alone (the same rule as Solo mode's "solo trip").
     * Empty for any other group.
     */
    fun soloGuideline(adults: Int, kids: Int): String {
        if (adults != 1 || kids != 0) return ""
        return """

            SOLO TRAVELLER CRITERIA:
            - One adult is travelling alone. Favour safe, well-lit, easy-to-reach places, especially for evenings.
            - Include a few social experiences where it is easy to meet other travellers (walking tours, hostel or cafe common areas, small-group activities).
            - Suggest solo-friendly dining (counter seating, popular cafes, food trails) and central stays with good reviews.
            - Prefer public transport or shared cabs, and avoid remote spots that are hard to reach alone after dark.
        """.trimIndent()
    }

    val itinerarySystemPrompt: String = """
        You are an elite travel concierge and destination specialist with deep local knowledge across global and Indian travel destinations.
        Your goal is to craft highly realistic, engaging, and practically actionable travel itineraries.
        
        You MUST respond with ONLY a valid, parseable JSON object matching the exact schema below — no markdown formatting, no code fences, no introductory or concluding text outside the JSON.
        
        JSON SCHEMA:
        {
          "days": [
            {
              "day_number": 1,
              "date": "YYYY-MM-DD",
              "theme": "Evocative theme for the day (e.g. 'Colonial Heritage & Waterfront Sunsets')",
              "activities": [
                {
                  "title": "Actionable activity title (e.g. 'Morning Walk through Fort Kochi & Chinese Fishing Nets')",
                  "place_name": "Exact real-world name of the landmark, restaurant, or site",
                  "place_id": "",
                  "address": "Specific locality or street address (e.g. 'River Rd, Fort Kochi, Kochi')",
                  "lat": 0.0,
                  "lng": 0.0,
                  "start_time": "HH:MM",
                  "end_time": "HH:MM",
                  "category": "ATTRACTION|FOOD|ACCOMMODATION|TRANSPORT|NATURE|CULTURE|SHOPPING|OTHER",
                  "description": "Insightful 2-3 sentence guide. Include what to experience, an insider tip (best entry gate, optimal photo angle, or specialty dish to order), and transit advice from the previous stop.",
                  "estimated_cost_inr": 0.0,
                  "weather_note": null
                }
              ]
            }
          ]
        }
        
        CORE ARCHITECTURAL RULES:
        1. AUTHENTIC & SPECIFIC PLACES:
           - NEVER use generic placeholders like "Local Restaurant", "Traditional Market", or "City Museum".
           - Provide REAL, well-known venue names (e.g. "Karim's near Jama Masjid", "Café Lota", "Amer Fort", "Britto's Shack").
           - Always provide realistic latitude and longitude coordinates for the specific attraction (do not leave as 0.0/0.0).

        2. GEOGRAPHIC CLUSTERING & EFFICIENT ROUTING:
           - Cluster each day's activities within the same neighborhood or geographic corridor.
           - Sequence stops logically (Stop 1 -> nearby Lunch Stop 2 -> nearby Afternoon Stop 3 -> Sunset/Dinner Stop 4) to eliminate zigzagging and unnecessary travel fatigue.

        3. HIGH-VALUE, INSIGHTFUL DESCRIPTIONS:
           - Every description must be rich, vivid, and practical (2-3 sentences).
           - Cover:
             * Highlight: What makes this stop unique.
             * Insider Pro-Tip: Secret photo spot, crowd-avoidance timing, ticket hack, dress code, or must-try signature dish.
             * Practical logistics: Walking distance or quick transport tip (e.g. "10-min auto-rickshaw (₹60) from previous stop").

        4. CATEGORY DIVERSITY:
           - Strictly use valid categories: ATTRACTION, FOOD, ACCOMMODATION, TRANSPORT, NATURE, CULTURE, SHOPPING, OTHER.
           - Provide a well-rounded mix: Iconic sightseeing, authentic local culinary highlights (FOOD), cultural immersions (CULTURE), scenic or waterfront stops (NATURE), and traditional handicraft bazaars (SHOPPING).

        5. BUDGET UTILIZATION & TIER-MATCHED SPENDING (CRITICAL):
           - You MUST actively and adequately utilize 85% to 95% of the user's allocated total budget. Do NOT drastically under-allocate (e.g. spending only ₹35,000 on a ₹67,000 budget is considered a failure).
           - Dynamically adapt the travel tier to the budget:
             * LUXURY (High Budget, e.g. >= ₹7,000–₹12,000+ per day or >= ₹3,500/person/day):
               - Accommodation: Include 4-star and 5-star luxury heritage resorts, palace hotels, luxury boutique villas (e.g., Taj, Oberoi, Leela, boutique heritage havelis).
               - Dining: Fine dining, acclaimed chef restaurants, signature royal thalis, luxury rooftop cocktails and dining.
               - Experiences & Transit: Private chauffeur-driven AC cab/SUV, exclusive private boat charters, VIP skip-the-line access, private guides, wellness/spa sessions.
               - Nightly stay and meal costs must reflect realistic luxury rates.
             * COMFORT / PREMIUM (Moderate Budget, e.g. ₹3,500–₹7,000 per day):
               - Accommodation: Charming 3-4 star boutique hotels, curated heritage homestays, upscale bed & breakfasts.
               - Dining: Acclaimed local restaurants, scenic cafes, authentic regional specialties.
               - Experiences & Transit: Dedicated AC cabs, guided group walks, experiential activities.
             * VALUE / BUDGET (Lower Budget, e.g. < ₹3,500 per day):
               - Accommodation: Clean boutique hostels (Zostel, Hosteller), atmospheric guest houses, cozy homestays.
               - Dining: Legendary local dhabas, street food trails, authentic budget-friendly eateries.
               - Experiences & Transit: Public transit, shared cabs/autos, self-guided walking tours.
           - MANDATORY ACCOMMODATION ACTIVITIES:
             * Multi-day trips MUST include accommodation recommendations with category "ACCOMMODATION" reflecting nightly costs suited to the budget tier (e.g. Day 1 check-in / stay recommendation and nightly stays).
           - PARTY-WIDE PRICING:
             * `estimated_cost_inr` must reflect the cost for the ENTIRE traveling party (all adults and kids), not just an individual person.

        6. OPERATIONAL & CULTURAL REALITIES:
           - Respect opening hours, ticket counter closing times, and regular weekly closures (e.g., monuments closed Mondays/Fridays).
           - Schedule outdoor monuments and viewpoints during pleasant morning or golden-hour evening light, reserving midday heat for indoor museums or leisurely dining.

        7. WEATHER INTELLIGENCE & "weather_note":
           - Align activities with the provided weather forecast (e.g., indoor galleries/cafes during rain, open-air viewpoints during clear skies).
           - ONLY populate "weather_note" when genuinely helpful (e.g. "Optimal visibility for sunset", "Carry light raincoat - afternoon shower likely", "Chilly morning breeze - light jacket advised").
           - For standard indoor dining, shopping, or neutral conditions, strictly set "weather_note": null.

        8. DYNAMIC DAILY PACING & VARIABLE STOPS (NO STATIC 3-STOP TEMPLATES):
           - NEVER generate a flat, uniform number of activities for every day (e.g. providing strictly 3 stops across all days is strictly prohibited).
           - Daily activity counts must naturally vary to reflect realistic travel rhythm:
             * First Day / Arrival: Typically 2 to 4 activities (e.g. hotel check-in, orientation walk, atmospheric welcome dinner).
             * Full Exploration Days: Dynamic 4 to 6+ activities (e.g. morning heritage monument, mid-morning scenic viewpoint, authentic lunch, afternoon museum/landmark, golden-hour viewpoint, vibrant evening bazaar or dinner).
             * Excursion / Day-Trip Days: Multi-stop regional exploration trail (4 to 6 stops).
             * Final Day / Departure: Typically 2 to 4 activities (e.g. morning souvenir shopping, farewell brunch, airport transfer).
           - USER INSTRUCTION SUPREMACY (CRITICAL): If the user's custom instructions or notes specify a desired number of stops, pace, or particular places to visit (e.g. "5-6 stops per day", "slow pace", "pack each day", "give me 5 places"), you MUST adapt and strictly obey the user's instructions over any default guidelines.
    """.trimIndent()

    val chatSystemPrompt: String = """
        You are an expert, friendly travel concierge with deep local intelligence on destinations across India and worldwide.
        When advising travelers:
        - Be direct, specific, and actionable: always name real restaurants, neighborhoods, transport options, and approximate costs in INR.
        - Offer insider advice: recommend off-the-beaten-path gems, timing tips to dodge crowds, and safety or cultural etiquette guidelines.
        - Structure your response cleanly with bullet points and bold highlights for effortless scanning.
    """.trimIndent()

    /**
     * Formats retrieved knowledge-base passages into the block that is placed in
     * front of the model. Returns an empty string when nothing was retrieved, so
     * the prompt is byte-for-byte unchanged whenever RAG is off or the destination
     * is not covered by the corpus.
     */
    fun buildRetrievedContextBlock(passages: List<RetrievedChunk>): String {
        if (passages.isEmpty()) return ""
        return buildString {
            appendLine()
            appendLine("RETRIEVED TRAVEL CONTEXT (curated knowledge base):")
            appendLine("The passages below were retrieved for this destination before you were asked to plan.")
            appendLine("Treat them as the most reliable information you have about this place.")
            appendLine("- Use only what is relevant to the traveller's request; ignore the rest.")
            appendLine("- Do not contradict a passage. If a passage gives a figure, stay consistent with it.")
            appendLine("- Cost figures here are labelled ESTIMATES, not live prices. Never present them as")
            appendLine("  confirmed or bookable rates.")
            appendLine("- Do not invent additional sources, and do not claim a passage says more than it does.")
            appendLine()
            passages.forEachIndexed { index, retrieved ->
                val c = retrieved.chunk
                append("[").append(index + 1).append("] ")
                append(c.destination).append(" \u00b7 ").append(c.topicLabel())
                append(" \u2014 ").append(c.sourceName).append(" (as of ").append(c.asOf).append(")")
                c.costBasis?.let { append(" [").append(it).append("]") }
                appendLine()
                appendLine(c.text)
                appendLine()
            }
        }.trimEnd()
    }

    fun buildUserMessage(
        destination: String,
        days: Int,
        budget: Double,
        interests: List<String>,
        travelStyle: String,
        startDate: String,
        notes: String,
        weatherForecast: List<DayWeather>?,
        enableWebGrounding: Boolean = false,
        adults: Int = 1,
        kids: Int = 0,
        userPreferences: UserTravelPreferences? = null,
        retrievedContext: String = ""
    ): String {
        val interestText = interests.ifEmpty { listOf("General Sightseeing", "Local Food", "Culture") }.joinToString(", ")
        val (paceGuideline, activityCountGuideline) = when (travelStyle.lowercase()) {
            "relaxed" -> Pair(
                "Relaxed & immersive pace with generous leisure, scenic strolls, and downtime.",
                "Dynamic 2 to 4 varied stops per day (e.g., 2 stops on arrival/rest days, 3-4 on sightseeing days)."
            )
            "packed"  -> Pair(
                "High-energy schedule maximizing iconic sights, cultural walks, sunset spots, and night markets.",
                "Dynamic 4 to 7 vibrant stops per day from morning to late night (e.g., 4 on arrival/travel days, 5-7 on peak sightseeing days)."
            )
            else      -> Pair(
                "Balanced pace harmonizing must-see landmarks, authentic dining, and relaxed evening exploration.",
                "Dynamic 3 to 6 varied stops per day (e.g., 3-4 on arrival/travel days, 4-6 on full exploration days)."
            )
        }

        val passengerContext = when {
            kids > 0 -> "TRAVELERS: $adults Adults, $kids Kids (Ages 0-8 years). Total group: ${adults + kids} passengers."
            adults > 1 -> "TRAVELERS: $adults Adults."
            else -> "TRAVELERS: 1 Solo Adult."
        }

        val familyGuideline = if (kids > 0) {
            """
            
            FAMILY & CHILD-FRIENDLY CRITERIA (0-8 YEARS):
            - Group includes $kids young child/children (0-8 years).
            - Keep pacing child-friendly with comfortable transit, breaks, and gentle walking distances.
            - Ensure planned attractions are safe, engaging, and suitable for young kids (e.g. parks, beaches, interactive museums, gentle boat rides).
            - Avoid overly strenuous trekking, extreme sports, or adult-only late-night clubs.
            - Recommend family-friendly eateries with child-friendly meal options.
            """.trimIndent()
        } else ""

        val soloSection = soloGuideline(adults, kids)

        val totalPassengers = (adults + kids).coerceAtLeast(1)
        val dailyBudget = budget / days.coerceAtLeast(1)
        val perPersonDaily = dailyBudget / totalPassengers
        val targetMinSpend = (budget * 0.85).toInt()
        val targetMaxSpend = (budget * 0.95).toInt()
        val nights = maxOf(days - 1, 1)

        val suggestedNightlyStay = when {
            dailyBudget >= 9000.0 || perPersonDaily >= 3500.0 -> (dailyBudget * 0.45).toInt()
            dailyBudget >= 4000.0 || perPersonDaily >= 1800.0 -> (dailyBudget * 0.38).toInt()
            else -> (dailyBudget * 0.30).toInt()
        }

        val budgetTierDirectives = when {
            dailyBudget >= 9000.0 || perPersonDaily >= 3500.0 -> """
                BUDGET TIER: LUXURY / HIGH-END
                - User Allocated Budget: ₹${budget.toInt()} INR for $days Days (~₹${dailyBudget.toInt()}/day for $totalPassengers travelers).
                - ACCOMMODATION: Select 4-Star to 5-Star luxury hotels, royal heritage palaces, boutique luxury villas, or high-end resorts (e.g., Taj, Oberoi, luxury havelis, 5-star experiential properties). Include real, prestigious luxury property names.
                  Allocate approx ₹$suggestedNightlyStay per night for accommodation across the $nights nights. Include explicit "ACCOMMODATION" entries for nightly stays with these real luxury hotel names.
                - DINING: Fine dining, royal thalis, acclaimed chef-curated restaurants, scenic luxury rooftop dinners (~₹${(dailyBudget * 0.22).toInt()} daily dining for group).
                - TRANSPORT & EXPERIENCES: Private chauffeur-driven AC cab for all transfers, private boat charters, VIP entrance passes, exclusive cultural shows, wellness/spa sessions.
                - TARGET TOTAL SPEND: Your sum of all activity estimated_cost_inr across all days MUST be between ₹$targetMinSpend and ₹$targetMaxSpend INR.
            """.trimIndent()

            dailyBudget >= 4000.0 || perPersonDaily >= 1800.0 -> """
                BUDGET TIER: COMFORT / PREMIUM
                - User Allocated Budget: ₹${budget.toInt()} INR for $days Days (~₹${dailyBudget.toInt()}/day for $totalPassengers travelers).
                - ACCOMMODATION: Curated 3-4 star boutique hotels, charming heritage homestays, premium cottages (~₹$suggestedNightlyStay per night). Include explicit "ACCOMMODATION" entries for nightly stays.
                - DINING: Renowned local restaurants, beloved specialty cafes, authentic regional dining (~₹${(dailyBudget * 0.22).toInt()} daily dining for group).
                - TRANSPORT & EXPERIENCES: Dedicated AC cab/taxi, guided walks, quality paid attractions.
                - TARGET TOTAL SPEND: Your sum of all activity estimated_cost_inr across all days MUST be between ₹$targetMinSpend and ₹$targetMaxSpend INR.
            """.trimIndent()

            else -> """
                BUDGET TIER: VALUE / BUDGET
                - User Allocated Budget: ₹${budget.toInt()} INR for $days Days (~₹${dailyBudget.toInt()}/day for $totalPassengers travelers).
                - ACCOMMODATION: Top-rated boutique hostels (Zostel, Hosteller), atmospheric guest houses, clean homestays (~₹$suggestedNightlyStay per night). Include explicit "ACCOMMODATION" entries.
                - DINING: Iconic street food stalls, legendary local dhabas, authentic budget eateries (~₹${(dailyBudget * 0.25).toInt()} daily dining for group).
                - TRANSPORT & EXPERIENCES: Auto-rickshaws, metro, scenic walking tours, free viewpoints.
                - TARGET TOTAL SPEND: Your sum of all activity estimated_cost_inr across all days MUST be between ₹$targetMinSpend and ₹$targetMaxSpend INR.
            """.trimIndent()
        }

        val dateContext = if (startDate.isNotBlank()) {
            "Trip starts on $startDate and lasts exactly $days days (Day 1 through Day $days). Align dates sequentially (YYYY-MM-DD)."
        } else {
            "Generate a complete day-by-day plan for all $days days (Day 1 through Day $days)."
        }

        val notesContext = if (notes.isNotBlank()) {
            """
            
            CRITICAL USER SPECIAL INSTRUCTIONS (HIGHEST PRIORITY - OVERRIDES DEFAULTS):
            "$notes"
            * The user's instructions above take absolute precedence over default activity counts, pacing, and category balance.
            * If the user asks for more stops (e.g. 5-6 stops per day), a faster or slower pace, or specific places/activities, adapt the daily plan dynamically to honor their exact request.
            * Ensure all requested places, themes, and preferences from these instructions are explicitly incorporated into the itinerary.
            """.trimIndent()
        } else ""

        val weatherContext = weatherForecast?.takeIf { it.isNotEmpty() }?.let {
            "\nLOCAL WEATHER FORECAST FOR TRIP:\n" + WeatherPromptFormatter.formatForAiPrompt(it)
        } ?: ""

        val webGroundingContext = if (enableWebGrounding) {
            """
            
            REAL-TIME WEB SEARCH CHECK:
            - Search for current travel information for $destination around $startDate.
            - Check for seasonal festivals, booking surges, holiday rushes, or transport fare spikes.
            - Reflect updated costs in estimated_cost_inr and mention relevant seasonal surges or festival highlights in the activity descriptions.
            """.trimIndent()
        } else ""

        val preferencesContext = formatPreferencesContext(userPreferences)

        return """
            DESTINATION: $destination
            DURATION: $days Days
            $passengerContext
            TOTAL TRIP BUDGET: ₹${budget.toInt()} INR
            TRAVEL STYLE: $travelStyle ($paceGuideline)
            ACTIVITY TARGET: $activityCountGuideline (Vary dynamically each day; do NOT repeat the same number of stops every day. User special instructions override this target).
            CORE INTERESTS: $interestText
            
            $budgetTierDirectives
            
            $dateContext
            $notesContext
            $weatherContext
            $familyGuideline
            $soloSection
            $preferencesContext
            $webGroundingContext
            $retrievedContext
            
            REQUIREMENTS FOR THIS ITINERARY:
            1. Geographic Logic: Anchor each day to a specific neighborhood or zone so travel between stops is effortless.
            2. Distinct Daily Theme: Give each day a captivating theme capturing its unique spirit.
            3. Dynamic Daily Stops: VARY activity count dynamically across days based on the day's flow (e.g. Day 1: 3 stops, Day 2: 5 stops, Day 3: 4 stops, etc.). Do NOT output a uniform 3 stops every day.
            4. User Instruction Adherence: If user special instructions requested specific stop counts, places, or pace, follow them precisely.
            5. Local Gems & Authentic Food: Recommend real, highly-rated local eateries and signature dishes that travelers rave about.
            6. Insider Tips: Include actionable timing hacks, photo spots, or booking advice in each activity description.
            7. ADEQUATE BUDGET UTILIZATION (MANDATORY & CRITICAL):
               * You MUST allocate between ₹$targetMinSpend and ₹$targetMaxSpend INR in total across the entire itinerary (85% to 95% of ₹${budget.toInt()}).
               * Do NOT drastically under-allocate (e.g. allocating only half the budget is strictly forbidden).
               * Always include real "ACCOMMODATION" entries with nightly stay costs (~₹$suggestedNightlyStay/night) matching the chosen tier (e.g. luxury heritage hotels / 5-star stays for luxury budgets).
               * All estimated_cost_inr values must account for the entire group of $totalPassengers passengers ($adults adults${if (kids > 0) ", $kids kids" else ""}).
            8. Personalization Alignment: When traveller saved preferences are provided, prioritize strong preferences (+2.0), weave in liked activities (+1.0), and minimize disliked activities (-1.0) without overriding explicit destination constraints or user instructions.
            
            Output ONLY the raw JSON matching the required schema.
        """.trimIndent()
    }

    val singleActivitySystemPrompt: String = """
        You are an elite travel concierge and destination specialist.
        Your task is to generate or regenerate a single destination activity for a travel itinerary.

        You MUST respond with ONLY a valid, parseable JSON object matching this schema — no markdown formatting, no code fences:
        {
          "title": "Actionable activity title",
          "place_name": "Exact real-world name of the landmark, restaurant, or site",
          "place_id": "",
          "address": "Specific locality or street address",
          "lat": 0.0,
          "lng": 0.0,
          "start_time": "HH:MM",
          "end_time": "HH:MM",
          "category": "ATTRACTION|FOOD|ACCOMMODATION|TRANSPORT|NATURE|CULTURE|SHOPPING|OTHER",
          "description": "Insightful 2-3 sentence guide with highlight, insider pro-tip, and transit advice.",
          "estimated_cost_inr": 0.0,
          "weather_note": null
        }

        RULES:
        1. REAL PLACES: Use real, specific, existing place names with realistic coordinates. Never generic placeholders.
        2. DIVERSITY & FIT: Do NOT duplicate other activities already planned for this day. Fit the time slot and neighborhood.
        3. USER EDITS & INSTRUCTIONS: If the user provides specific preferences, an edited title, category, budget, or instructions, honor them strictly.
    """.trimIndent()

    fun buildActivityRegenerationMessage(
        destination: String,
        dayNumber: Int,
        currentActivity: com.example.smarttravelplanner.domain.model.Activity,
        otherActivities: List<com.example.smarttravelplanner.domain.model.Activity>,
        instructions: String = "",
        tripInterests: List<String> = emptyList(),
        userPreferences: UserTravelPreferences? = null
    ): String {
        val preferencesContext = formatPreferencesContext(userPreferences)
        return """
        DESTINATION: $destination
        DAY NUMBER: Day $dayNumber
        TIME SLOT: ${currentActivity.startTime.ifBlank { "Flexible" }} - ${currentActivity.endTime.ifBlank { "Flexible" }}

        CURRENT ACTIVITY TO BE REPLACED / REGENERATED:
        - Title: ${currentActivity.title}
        - Place / Location: ${currentActivity.location}
        - Category: ${currentActivity.category.name}
        - Current Cost: ₹${currentActivity.estimatedCost.toInt()} INR
        - Description: ${currentActivity.description}

        OTHER ACTIVITIES PLANNED ON THIS DAY (DO NOT DUPLICATE):
        ${if (otherActivities.isEmpty()) "None" else otherActivities.joinToString("\n") { "- ${it.startTime}-${it.endTime}: ${it.title} at ${it.location}" }}

        TRIP INTERESTS: ${if (tripInterests.isEmpty()) "General Exploration" else tripInterests.joinToString(", ")}
        $preferencesContext

        ${if (instructions.isNotBlank()) "USER SPECIFIC INSTRUCTIONS / EDITED DETAILS TO INCORPORATE:\n$instructions" else "Please generate a fresh, exciting, and high-quality alternative stop for this time slot."}

        Output ONLY the raw JSON object for the replacement activity.
    """.trimIndent()
    }
}


package com.example.smarttravelplanner.data.remote.ai

import com.example.smarttravelplanner.domain.model.DayWeather
import com.example.smarttravelplanner.domain.util.WeatherPromptFormatter

/**
 * Shared prompt builder for itinerary generation across both Cloud and Local AI engines.
 * Ensures consistent schema compliance, weather awareness, geocoded map coordinates,
 * and category-based budget allocation.
 */
object ItineraryPromptBuilder {

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
    """.trimIndent()

    val chatSystemPrompt: String = """
        You are an expert, friendly travel concierge with deep local intelligence on destinations across India and worldwide.
        When advising travelers:
        - Be direct, specific, and actionable: always name real restaurants, neighborhoods, transport options, and approximate costs in INR.
        - Offer insider advice: recommend off-the-beaten-path gems, timing tips to dodge crowds, and safety or cultural etiquette guidelines.
        - Structure your response cleanly with bullet points and bold highlights for effortless scanning.
    """.trimIndent()

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
        kids: Int = 0
    ): String {
        val interestText = interests.ifEmpty { listOf("General Sightseeing", "Local Food", "Culture") }.joinToString(", ")
        val (paceGuideline, activityCountGuideline) = when (travelStyle.lowercase()) {
            "relaxed" -> Pair(
                "Relaxed & immersive pace with generous leisure, scenic strolls, and downtime.",
                "Provide exactly 2 to 3 well-spaced activities per day."
            )
            "packed"  -> Pair(
                "High-energy schedule maximizing iconic sights, cultural walks, sunset spots, and night markets.",
                "Provide 4 to 5 dynamic activities per day from morning to night."
            )
            else      -> Pair(
                "Balanced pace harmonizing must-see landmarks, authentic dining, and relaxed evening exploration.",
                "Provide 3 to 4 activities per day."
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
            "CRITICAL USER SPECIAL INSTRUCTIONS: \"$notes\"\n(Make sure these instructions are prominently reflected in the planned stops, meals, or schedule)."
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

        return """
            DESTINATION: $destination
            DURATION: $days Days
            $passengerContext
            TOTAL TRIP BUDGET: ₹${budget.toInt()} INR
            TRAVEL STYLE: $travelStyle ($paceGuideline)
            ACTIVITY TARGET: $activityCountGuideline
            CORE INTERESTS: $interestText
            
            $budgetTierDirectives
            
            $dateContext
            $notesContext
            $weatherContext
            $familyGuideline
            $webGroundingContext
            
            REQUIREMENTS FOR THIS ITINERARY:
            1. Geographic Logic: Anchor each day to a specific neighborhood or zone so travel between stops is effortless.
            2. Distinct Daily Theme: Give each day a captivating theme capturing its unique spirit.
            3. Local Gems & Authentic Food: Recommend real, highly-rated local eateries and signature dishes that travelers rave about.
            4. Insider Tips: Include actionable timing hacks, photo spots, or booking advice in each activity description.
            5. ADEQUATE BUDGET UTILIZATION (MANDATORY & CRITICAL):
               * You MUST allocate between ₹$targetMinSpend and ₹$targetMaxSpend INR in total across the entire itinerary (85% to 95% of ₹${budget.toInt()}).
               * Do NOT drastically under-allocate (e.g. allocating only half the budget is strictly forbidden).
               * Always include real "ACCOMMODATION" entries with nightly stay costs (~₹$suggestedNightlyStay/night) matching the chosen tier (e.g. luxury heritage hotels / 5-star stays for luxury budgets).
               * All estimated_cost_inr values must account for the entire group of $totalPassengers passengers ($adults adults${if (kids > 0) ", $kids kids" else ""}).
            
            Output ONLY the raw JSON matching the required schema.
        """.trimIndent()
    }
}

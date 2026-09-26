package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.ExploreDestination

/**
 * Works out which months are a destination's best season from its
 * `bestSeason` text (e.g. "Best: Oct – Mar") and whether a given month is in it.
 * Pure Kotlin so it can be unit tested without Android.
 */
object DestinationSeason {

    private val monthNumbers = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
    )

    /** Matches "Oct – Mar", "oct-mar", "Sept to April" and similar. */
    private val rangePattern = Regex(
        """([A-Za-z]{3})[A-Za-z]*\s*(?:–|—|-|to)\s*([A-Za-z]{3})[A-Za-z]*"""
    )

    /**
     * Months (1 = January … 12 = December) covered by [bestSeason].
     * Ranges that cross the new year ("Oct – Mar") wrap around.
     * Returns an empty set if the text can't be understood.
     */
    fun monthsFor(bestSeason: String): Set<Int> {
        val match = rangePattern.find(bestSeason) ?: return emptySet()
        val start = monthNumbers[match.groupValues[1].lowercase()] ?: return emptySet()
        val end = monthNumbers[match.groupValues[2].lowercase()] ?: return emptySet()

        val months = mutableSetOf<Int>()
        var month = start
        while (true) {
            months += month
            if (month == end) break
            month = month % 12 + 1
        }
        return months
    }

    /** True if [month] (1–12) falls inside the destination's best season. */
    fun isInSeason(destination: ExploreDestination, month: Int): Boolean =
        month in monthsFor(destination.bestSeason)
}

package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.ExploreDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationSeasonTest {

    private fun dest(season: String) = ExploreDestination(
        name = "Test",
        stateCountry = "",
        idealDuration = "",
        bestSeason = season,
        tag = "",
        tagCategory = "",
        imageRes = 0
    )

    @Test
    fun simpleRangeWithinOneYear() {
        assertEquals(setOf(3, 4, 5, 6), DestinationSeason.monthsFor("Best: Mar – Jun"))
    }

    @Test
    fun rangeWrapsAcrossNewYear() {
        assertEquals(setOf(10, 11, 12, 1, 2, 3), DestinationSeason.monthsFor("Best: Oct – Mar"))
    }

    @Test
    fun acceptsHyphenAndFullMonthNames() {
        assertEquals(setOf(9, 10, 11, 12), DestinationSeason.monthsFor("September-December"))
        assertEquals(setOf(11, 12, 1, 2), DestinationSeason.monthsFor("Best: nov to feb"))
    }

    @Test
    fun unparseableTextGivesNoMonths() {
        assertTrue(DestinationSeason.monthsFor("Year-round").isEmpty())
        assertTrue(DestinationSeason.monthsFor("").isEmpty())
    }

    @Test
    fun isInSeasonChecksTheMonth() {
        val goa = dest("Best: Oct – Mar")
        assertTrue(DestinationSeason.isInSeason(goa, 1))   // January
        assertTrue(DestinationSeason.isInSeason(goa, 10))  // October
        assertFalse(DestinationSeason.isInSeason(goa, 7))  // July (monsoon)
    }

    @Test
    fun everyCatalogSeasonFormatIsUnderstood() {
        val formats = listOf(
            "Best: Apr – Oct", "Best: Jun – Sep", "Best: Mar – Jun", "Best: Mar – May",
            "Best: Nov – Feb", "Best: Oct – Jun", "Best: Oct – Mar", "Best: Oct – May",
            "Best: Sep – Apr", "Best: Sep – Mar", "Best: Sep – May"
        )
        formats.forEach { assertTrue("Couldn't parse '$it'", DestinationSeason.monthsFor(it).isNotEmpty()) }
    }
}

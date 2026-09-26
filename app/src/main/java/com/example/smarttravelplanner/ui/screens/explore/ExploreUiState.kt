package com.example.smarttravelplanner.ui.screens.explore

import com.example.smarttravelplanner.domain.model.ExploreDestination
import com.example.smarttravelplanner.domain.model.ExploreSortOrder
import com.example.smarttravelplanner.domain.util.DestinationFilter

/**
 * Everything the Explore screen renders, owned by [ExploreViewModel].
 *
 * A single immutable state object (rather than several `remember` variables in
 * the composable) so the filters survive recomposition and configuration changes
 * and the filtering logic can be tested without the UI.
 */
data class ExploreUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String = DestinationFilter.ALL,
    val departureCities: List<String> = emptyList(),
    val selectedCity: String = "New Delhi",
    val sortOrder: ExploreSortOrder = ExploreSortOrder.RECOMMENDED,
    /** When true, only destinations whose best season includes the current month are shown. */
    val inSeasonOnly: Boolean = false,
    /** Names of destinations that are in season this month (drives the "In season" badge). */
    val inSeasonNames: Set<String> = emptySet(),
    /** When true, only destinations on the wishlist are shown. */
    val savedOnly: Boolean = false,
    /** Names of destinations the user has hearted. */
    val savedNames: Set<String> = emptySet(),
    /** Destinations after search + category filters (and sorting) have been applied. */
    val destinations: List<ExploreDestination> = emptyList(),
    /** Straight-line km from [selectedCity] to each destination, keyed by destination name. */
    val distancesKm: Map<String, Int> = emptyMap(),
    /** Destination whose details sheet is open, or null when no sheet is shown. */
    val selectedDestination: ExploreDestination? = null
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() ||
            selectedCategory != DestinationFilter.ALL ||
            inSeasonOnly ||
            savedOnly
}

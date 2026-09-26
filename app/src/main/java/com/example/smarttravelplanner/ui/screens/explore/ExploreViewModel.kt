package com.example.smarttravelplanner.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.DestinationRepository
import com.example.smarttravelplanner.data.repository.WishlistRepository
import com.example.smarttravelplanner.domain.model.DepartureCity
import com.example.smarttravelplanner.domain.model.ExploreDestination
import com.example.smarttravelplanner.domain.model.ExploreSortOrder
import com.example.smarttravelplanner.domain.util.DestinationFilter
import com.example.smarttravelplanner.domain.util.DestinationSeason
import com.example.smarttravelplanner.domain.util.GeoDistance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val destinationRepository: DestinationRepository,
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    /** Unfiltered catalog; [ExploreUiState.destinations] is derived from this. */
    private var allDestinations: List<ExploreDestination> = emptyList()

    /** Current month (1–12), used for "In season now". */
    private val currentMonth: Int = LocalDate.now().monthValue

    /** "From" cities with coordinates; the UI only needs their names. */
    private var departureCities: List<DepartureCity> = emptyList()

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadDestinations()
        observeWishlist()
    }

    /** Keeps [ExploreUiState.savedNames] in sync with the stored wishlist. */
    private fun observeWishlist() {
        viewModelScope.launch {
            wishlistRepository.savedDestinations.collect { saved ->
                updateFilters { it.copy(savedNames = saved) }
            }
        }
    }

    fun loadDestinations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            destinationRepository.getDestinations()
                .onSuccess { destinations ->
                    allDestinations = destinations
                    departureCities = destinationRepository.getDepartureCities()
                    val cityNames = departureCities.map { it.name }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            departureCities = cityNames,
                            selectedCity = state.selectedCity
                                .takeIf { it in cityNames } ?: cityNames.firstOrNull().orEmpty()
                        ).withFilteredDestinations()
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Couldn't load destinations."
                        )
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) = updateFilters { it.copy(searchQuery = query) }

    fun onCategorySelected(categoryId: String) =
        updateFilters { it.copy(selectedCategory = categoryId) }

    fun onCitySelected(city: String) = updateFilters { it.copy(selectedCity = city) }

    fun onSortOrderSelected(order: ExploreSortOrder) = updateFilters { it.copy(sortOrder = order) }

    fun onInSeasonToggled() = updateFilters { it.copy(inSeasonOnly = !it.inSeasonOnly) }

    fun onSavedOnlyToggled() = updateFilters { it.copy(savedOnly = !it.savedOnly) }

    fun onToggleSaved(destination: ExploreDestination) {
        viewModelScope.launch { wishlistRepository.toggle(destination.name) }
    }

    fun onDestinationClick(destination: ExploreDestination) {
        _uiState.update { it.copy(selectedDestination = destination) }
    }

    fun onDetailsDismissed() {
        _uiState.update { it.copy(selectedDestination = null) }
    }

    fun clearFilters() = updateFilters {
        it.copy(
            searchQuery = "",
            selectedCategory = DestinationFilter.ALL,
            inSeasonOnly = false,
            savedOnly = false
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun updateFilters(transform: (ExploreUiState) -> ExploreUiState) {
        _uiState.update { transform(it).withFilteredDestinations() }
    }

    /** Re-derives the visible list: filter → in-season → distances from the "From" city → optional sort. */
    private fun ExploreUiState.withFilteredDestinations(): ExploreUiState {
        val inSeason = allDestinations
            .filter { DestinationSeason.isInSeason(it, currentMonth) }
            .map { it.name }
            .toSet()

        val filtered = DestinationFilter.apply(
            destinations = allDestinations,
            query = searchQuery,
            category = selectedCategory
        )
            .let { list -> if (inSeasonOnly) list.filter { it.name in inSeason } else list }
            .let { list -> if (savedOnly) list.filter { it.name in savedNames } else list }
        // this@ExploreViewModel: the ViewModel's city list (with coordinates), not ExploreUiState.departureCities (names only)
        val origin = this@ExploreViewModel.departureCities
            .firstOrNull { it.name == selectedCity }?.location
        val distances = origin?.let { GeoDistance.distancesFrom(it, allDestinations) }.orEmpty()

        val ordered = when (sortOrder) {
            ExploreSortOrder.RECOMMENDED -> filtered
            ExploreSortOrder.NEAREST     -> GeoDistance.sortByDistance(filtered, distances)
        }
        return copy(destinations = ordered, distancesKm = distances, inSeasonNames = inSeason)
    }
}

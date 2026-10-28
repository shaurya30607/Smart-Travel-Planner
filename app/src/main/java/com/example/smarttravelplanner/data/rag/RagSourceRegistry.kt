package com.example.smarttravelplanner.data.rag

import com.example.smarttravelplanner.domain.model.RagContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Remembers which knowledge-base passages were used for each generated trip, so the
 * trip detail screen can show its sources.
 *
 * Deliberately in memory only. Persisting citations would mean a new Room table and a
 * database migration, which is out of scope for this change; the trade-off is that
 * sources are shown for trips generated in the current session and quietly disappear
 * after the process is killed. The itinerary itself is unaffected either way.
 *
 * A plain Kotlin object rather than a Hilt singleton, so a Composable can observe it
 * without threading a dependency through the existing trip detail view model.
 */
object RagSourceRegistry {

    private val _contexts = MutableStateFlow<Map<Long, RagContext>>(emptyMap())
    val contexts: StateFlow<Map<Long, RagContext>> = _contexts.asStateFlow()

    fun record(tripId: Long, context: RagContext) {
        if (tripId <= 0L || context.isEmpty) return
        _contexts.value = _contexts.value + (tripId to context)
    }

    fun forTrip(tripId: Long): RagContext? = _contexts.value[tripId]

    /** Used by tests, and when demo data is reset. */
    fun clear() {
        _contexts.value = emptyMap()
    }
}

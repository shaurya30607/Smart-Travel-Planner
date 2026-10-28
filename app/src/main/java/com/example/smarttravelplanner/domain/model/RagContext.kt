package com.example.smarttravelplanner.domain.model

import com.example.smarttravelplanner.data.rag.RetrievedChunk

/**
 * The outcome of TripWeaver's retrieval step for one itinerary generation.
 *
 * Held so the app can show the traveller exactly which passages informed the plan.
 * [passages] is empty whenever the destination is not covered by the curated
 * knowledge base, in which case nothing was added to the prompt at all.
 */
data class RagContext(
    /** Knowledge-base destination id that was matched, e.g. "kerala". */
    val destinationId: String,
    /** What the traveller typed, kept for display. */
    val requestedDestination: String,
    val passages: List<RetrievedChunk>,
    /** How the passages were ranked. "LEXICAL" today; "DENSE" once embeddings are wired. */
    val retrievalMode: String = LEXICAL,
    /** Corpus-level disclaimer from the knowledge base file. */
    val notice: String = ""
) {
    val isEmpty: Boolean get() = passages.isEmpty()

    companion object {
        const val LEXICAL = "LEXICAL"
        const val DENSE = "DENSE"
    }
}

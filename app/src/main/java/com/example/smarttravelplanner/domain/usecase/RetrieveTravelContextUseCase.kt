package com.example.smarttravelplanner.domain.usecase

import com.example.smarttravelplanner.data.rag.KnowledgeBase
import com.example.smarttravelplanner.data.rag.RetrievalEngine
import com.example.smarttravelplanner.domain.model.RagContext
import javax.inject.Inject

/**
 * The retrieval step of TripWeaver's RAG pipeline.
 *
 * Looks up passages for the requested destination in the curated knowledge base.
 * Returns a [RagContext] with an empty passage list when the destination is not
 * covered, so callers can carry on and generate exactly as they did before.
 *
 * Never throws: a failure to read the corpus degrades to no retrieval rather than a
 * failed trip.
 */
class RetrieveTravelContextUseCase @Inject constructor(
    private val knowledgeBase: KnowledgeBase
) {

    suspend operator fun invoke(
        destination: String,
        interests: List<String> = emptyList(),
        notes: String = "",
        topK: Int = DEFAULT_TOP_K
    ): RagContext = runCatching {
        val chunks = knowledgeBase.chunks()
        val destinationId = RetrievalEngine.matchDestination(destination)
            ?: RetrievalEngine.matchDestination(notes)
            ?: return@runCatching empty(destination)

        val passages = RetrievalEngine.retrieve(
            chunks = chunks,
            destination = destination,
            interests = interests,
            notes = notes,
            topK = topK
        )
        RagContext(
            destinationId = destinationId,
            requestedDestination = destination,
            passages = passages,
            retrievalMode = RagContext.LEXICAL,
            notice = knowledgeBase.notice()
        )
    }.getOrElse { empty(destination) }

    private fun empty(destination: String) = RagContext(
        destinationId = "",
        requestedDestination = destination,
        passages = emptyList()
    )

    companion object {
        /** Four passages is roughly 400 words — enough to help, small enough not to crowd the prompt. */
        const val DEFAULT_TOP_K = 4
    }
}

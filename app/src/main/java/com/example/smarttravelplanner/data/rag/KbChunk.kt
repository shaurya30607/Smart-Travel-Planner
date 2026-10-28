package com.example.smarttravelplanner.data.rag

import com.google.gson.annotations.SerializedName

/**
 * One retrievable passage of the curated travel knowledge base.
 *
 * The knowledge base ships as an asset (`assets/kb/knowledge_base.json`) and is the
 * corpus for the retrieval step of TripWeaver's RAG pipeline. Every passage carries
 * its own provenance so the app can show the traveller where a fact came from.
 *
 * All monetary figures live only in chunks whose [topic] is `costs`, and those chunks
 * must carry a [costBasis] label. Nothing in this corpus is a live price.
 */
data class KbChunk(
    @SerializedName("id")             val id: String = "",
    @SerializedName("destination")    val destination: String = "",
    @SerializedName("destination_id") val destinationId: String = "",
    @SerializedName("topic")          val topic: String = "",
    @SerializedName("title")          val title: String = "",
    @SerializedName("text")           val text: String = "",
    @SerializedName("source_name")    val sourceName: String = "",
    @SerializedName("source_url")     val sourceUrl: String = "",
    @SerializedName("as_of")          val asOf: String = "",
    /** Present only on cost passages, e.g. "ESTIMATE - per person per day, INR...". */
    @SerializedName("cost_basis")     val costBasis: String? = null
) {
    /** Text used for scoring: the title and topic carry signal as well as the body. */
    val searchableText: String
        get() = "$title ${topic.replace('_', ' ')} $text"

    /** Short provenance line shown under a generated itinerary. */
    fun citation(): String =
        "$destination · ${topicLabel()} — $sourceName ($asOf)"

    fun topicLabel(): String = when (topic) {
        "getting_there"   -> "Getting there"
        "best_time"       -> "Best time to visit"
        "top_sights"      -> "What to see"
        "local_transport" -> "Local transport"
        "food"            -> "Food and dining"
        "costs"           -> "Estimated costs"
        else              -> topic.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}

/** Root of `assets/kb/knowledge_base.json`. */
data class KnowledgeBaseFile(
    @SerializedName("version")   val version: Int = 0,
    @SerializedName("generated") val generated: String = "",
    @SerializedName("notice")    val notice: String = "",
    @SerializedName("chunks")    val chunks: List<KbChunk> = emptyList()
)

/** A passage selected by the retriever, with the score that selected it. */
data class RetrievedChunk(
    val chunk: KbChunk,
    /** BM25 contribution from the traveller's interests and notes. */
    val lexicalScore: Double,
    /** Final score actually used for ranking (lexical + topic prior). */
    val score: Double
)

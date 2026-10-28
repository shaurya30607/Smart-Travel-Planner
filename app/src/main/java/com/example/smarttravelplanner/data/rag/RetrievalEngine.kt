package com.example.smarttravelplanner.data.rag

import kotlin.math.ln

/**
 * The retrieval half of TripWeaver's RAG pipeline.
 *
 * Pure Kotlin: no Android, no network, no storage. Everything here is a function of
 * its arguments, which is why it can be unit tested without an emulator.
 *
 * Retrieval runs in two stages:
 *
 *  1. **Destination gate.** The planner always knows which destination the traveller
 *     asked for, so there is no reason to search the whole corpus. If the destination
 *     is not covered by the knowledge base, retrieval returns nothing — the app must
 *     never inject passages about one place into a plan for another.
 *
 *  2. **Topic ranking.** Within that destination, the six passages are ranked with
 *     BM25 over the traveller's interests and free-text notes, plus a small prior so
 *     that universally relevant topics (cost, timing) are not crowded out when the
 *     traveller gives no hints at all.
 */
object RetrievalEngine {

    /** BM25 term-frequency saturation. */
    private const val K1 = 1.2

    /** BM25 length normalisation. */
    private const val B = 0.75

    /**
     * How much every topic matters to an itinerary regardless of what was asked.
     * Cost and timing shape a plan whatever the traveller's interests are.
     */
    private val TOPIC_PRIOR = mapOf(
        "costs" to 0.9,
        "best_time" to 0.7,
        "top_sights" to 0.5,
        "local_transport" to 0.3,
        "food" to 0.3,
        "getting_there" to 0.2
    )

    /**
     * Destination names and the well-known places inside them. A traveller may type
     * "Alleppey" or "Munnar" when the corpus is filed under Kerala.
     */
    private val ALIASES: Map<String, List<String>> = mapOf(
        "goa" to listOf("goa", "panaji", "panjim", "calangute", "baga", "anjuna",
            "palolem", "vagator", "arambol", "madgaon", "margao"),
        "jaipur" to listOf("jaipur", "pink city", "amber fort", "amer", "rajasthan"),
        "manali" to listOf("manali", "solang", "rohtang", "kullu", "himachal", "lahaul", "spiti"),
        "kerala" to listOf("kerala", "alleppey", "alappuzha", "munnar", "kochi",
            "cochin", "backwater", "fort kochi"),
        "rishikesh" to listOf("rishikesh", "haridwar", "tapovan", "uttarakhand",
            "ganga", "laxman jhula", "lakshman jhula")
    )

    private val STOP_WORDS: Set<String> = (
        "a an the and or but for of to in on at by with from is are was were be been " +
        "i me my we our you your it its this that these those as if then than so very more most " +
        "some any can will just about into over under do does did have has had not no yes"
        ).split(" ").toSet()

    // ─────────────────────────── destination gate ───────────────────────────

    /**
     * Resolves free text to a knowledge-base destination id, or null when the corpus
     * does not cover it. The longest alias wins so "fort kochi" beats "kochi".
     */
    fun matchDestination(text: String?): String? {
        val haystack = text?.lowercase().orEmpty()
        if (haystack.isBlank()) return null
        var best: String? = null
        var bestLength = 0
        ALIASES.forEach { (id, names) ->
            names.forEach { name ->
                if (haystack.contains(name) && name.length > bestLength) {
                    best = id
                    bestLength = name.length
                }
            }
        }
        return best
    }

    // ──────────────────────────────── ranking ────────────────────────────────

    /**
     * Returns the passages to put in front of the model, highest scoring first.
     *
     * @param chunks the whole corpus
     * @param destination what the traveller typed, e.g. "Alleppey"
     * @param interests selected interest tags
     * @param notes free-text special instructions
     * @param topK how many passages to inject
     * @return an empty list when the destination is not covered
     */
    fun retrieve(
        chunks: List<KbChunk>,
        destination: String?,
        interests: List<String> = emptyList(),
        notes: String = "",
        topK: Int = 4
    ): List<RetrievedChunk> {
        if (topK <= 0) return emptyList()

        val destinationId = matchDestination(destination) ?: matchDestination(notes) ?: return emptyList()
        val candidates = chunks.filter { it.destinationId == destinationId }
        if (candidates.isEmpty()) return emptyList()

        val documents = candidates.map { tokenize(it.searchableText) }
        val documentFrequency = HashMap<String, Int>()
        documents.forEach { document ->
            document.toSet().forEach { term ->
                documentFrequency[term] = (documentFrequency[term] ?: 0) + 1
            }
        }
        val averageLength = documents.sumOf { it.size }.toDouble() / documents.size

        val queryTerms = tokenize(interests.joinToString(" ") + " " + notes).toSet()

        return candidates.mapIndexed { index, chunk ->
            val lexical = if (queryTerms.isEmpty()) 0.0 else bm25(
                queryTerms = queryTerms,
                document = documents[index],
                documentFrequency = documentFrequency,
                documentCount = documents.size,
                averageLength = averageLength
            )
            val prior = TOPIC_PRIOR[chunk.topic] ?: 0.0
            RetrievedChunk(chunk = chunk, lexicalScore = lexical, score = lexical + prior)
        }.sortedByDescending { it.score }.take(topK)
    }

    private fun bm25(
        queryTerms: Set<String>,
        document: List<String>,
        documentFrequency: Map<String, Int>,
        documentCount: Int,
        averageLength: Double
    ): Double {
        if (document.isEmpty()) return 0.0
        val termFrequency = HashMap<String, Int>()
        document.forEach { termFrequency[it] = (termFrequency[it] ?: 0) + 1 }

        var score = 0.0
        queryTerms.forEach { term ->
            val frequency = termFrequency[term] ?: 0
            if (frequency > 0) {
                val containing = documentFrequency[term] ?: 0
                val idf = ln(1.0 + (documentCount - containing + 0.5) / (containing + 0.5))
                val numerator = frequency * (K1 + 1)
                val denominator = frequency + K1 * (1 - B + B * document.size / averageLength)
                score += idf * numerator / denominator
            }
        }
        return score
    }

    // ─────────────────────────────── tokenising ───────────────────────────────

    /** Lowercase, strip punctuation, drop stop words and very short tokens, then stem. */
    fun tokenize(text: String?): List<String> =
        text.orEmpty()
            .lowercase()
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .split(' ')
            .filter { it.length > 2 && it !in STOP_WORDS }
            .map { stem(it) }

    /**
     * Deliberately crude suffix stripping — enough that "beach" and "beaches", or
     * "temple" and "temples", collapse to the same token.
     *
     * Order matters. A blanket "es" rule would turn "temples" into "templ" while
     * leaving "temple" untouched, so the two would never match. Only the sibilant
     * endings drop "es"; everything else drops a single trailing "s", and words
     * ending in a double "s" are left alone.
     */
    private fun stem(token: String): String = when {
        token.endsWith("ies") && token.length > 4 -> token.dropLast(3) + "y"
        token.endsWith("ing") && token.length > 5 -> token.dropLast(3)
        token.length > 4 && SIBILANT_PLURALS.any { token.endsWith(it) } -> token.dropLast(2)
        token.endsWith("s") && !token.endsWith("ss") && token.length > 3 -> token.dropLast(1)
        else -> token
    }

    private val SIBILANT_PLURALS = listOf("ches", "shes", "xes", "ses")

    /** Exposed for tests: the stemmer is easy to get subtly wrong. */
    internal fun stemForTest(token: String): String = stem(token)

    /** Destination ids the corpus covers, for diagnostics and settings screens. */
    fun coveredDestinations(): Set<String> = ALIASES.keys
}

package com.example.smarttravelplanner.data.rag

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Retrieval is pure Kotlin, so it is tested here with no emulator and no network.
 *
 * Two groups of tests:
 *  - behaviour of [RetrievalEngine] against a small hand-built corpus, which is
 *    deterministic and independent of the shipped asset
 *  - sanity of the real `assets/kb/knowledge_base.json` that actually ships,
 *    skipped gracefully if the file cannot be located from the test working directory
 */
class RetrievalEngineTest {

    // ───────────────────────────── fixtures ─────────────────────────────

    private fun chunk(dest: String, topic: String, text: String) = KbChunk(
        id = "$dest-$topic",
        destination = dest.replaceFirstChar { it.uppercase() },
        destinationId = dest,
        topic = topic,
        title = "$topic in $dest",
        text = text,
        sourceName = "Test Source",
        sourceUrl = "https://example.test/",
        asOf = "2026-10",
        costBasis = if (topic == "costs") "ESTIMATE - test basis" else null
    )

    private val corpus = listOf(
        chunk("goa", "costs", "Budget travel is cheaper than mid range travel. Rupees per person per day."),
        chunk("goa", "best_time", "Peak season runs November to February with dry pleasant weather."),
        chunk("goa", "top_sights", "Beaches at Baga and Anjuna, churches of Old Goa, Dudhsagar waterfall."),
        chunk("goa", "food", "Fish curry rice, prawn balchao, xacuti and bebinca are Goan specialities."),
        chunk("goa", "local_transport", "Scooter hire is common, taxis are negotiated, buses are slow."),
        chunk("goa", "getting_there", "Dabolim and Mopa airports, Madgaon railway station on the Konkan line."),
        chunk("jaipur", "costs", "Budget guest houses are inexpensive. Monument tickets add to daily spend."),
        chunk("jaipur", "top_sights", "Amber Fort, Hawa Mahal, City Palace and Jantar Mantar observatory."),
        chunk("jaipur", "food", "Dal baati churma, laal maas and pyaaz kachori are local dishes.")
    )

    // ─────────────────────── destination gate ───────────────────────

    @Test
    fun matchesAPlainDestinationName() {
        assertEquals("goa", RetrievalEngine.matchDestination("Goa"))
        assertEquals("jaipur", RetrievalEngine.matchDestination("Jaipur"))
    }

    @Test
    fun matchesASubLocationToItsParentDestination() {
        assertEquals("kerala", RetrievalEngine.matchDestination("Alleppey"))
        assertEquals("kerala", RetrievalEngine.matchDestination("Munnar, Kerala"))
        assertEquals("manali", RetrievalEngine.matchDestination("Solang Valley"))
    }

    @Test
    fun destinationMatchingIgnoresCaseAndPunctuation() {
        assertEquals("jaipur", RetrievalEngine.matchDestination("  JAIPUR, India "))
    }

    @Test
    fun returnsNullForADestinationTheCorpusDoesNotCover() {
        assertNull(RetrievalEngine.matchDestination("Paris"))
        assertNull(RetrievalEngine.matchDestination("Andaman Islands"))
        assertNull(RetrievalEngine.matchDestination(""))
        assertNull(RetrievalEngine.matchDestination(null))
    }

    @Test
    fun retrievesNothingForAnUncoveredDestination() {
        // The whole point: never put Goa passages in front of a Paris itinerary.
        val hits = RetrievalEngine.retrieve(corpus, "Paris", listOf("food"), "")
        assertTrue("Uncovered destinations must inject no context", hits.isEmpty())
    }

    @Test
    fun retrievesNothingWhenTheCorpusIsEmpty() {
        assertTrue(RetrievalEngine.retrieve(emptyList(), "Goa", listOf("food"), "").isEmpty())
    }

    // ───────────────────────────── relevance ─────────────────────────────

    @Test
    fun everyRetrievedPassageBelongsToTheRequestedDestination() {
        val hits = RetrievalEngine.retrieve(corpus, "Goa", listOf("sightseeing"), "", topK = 4)
        assertEquals(4, hits.size)
        assertTrue("Another destination leaked into the results",
            hits.all { it.chunk.destinationId == "goa" })
    }

    @Test
    fun costPassageIsAlwaysRetrievedBecauseBudgetAlwaysMatters() {
        listOf("Goa", "Jaipur").forEach { destination ->
            val hits = RetrievalEngine.retrieve(corpus, destination, listOf("photography"), "", topK = 3)
            assertTrue("$destination should still surface its cost passage",
                hits.any { it.chunk.topic == "costs" })
        }
    }

    @Test
    fun foodInterestPullsUpTheFoodPassage() {
        val hits = RetrievalEngine.retrieve(
            corpus, "Goa", listOf("Local Food"), "we want to try prawn curry and xacuti", topK = 4
        )
        assertTrue(hits.map { it.chunk.id }.contains("goa-food"))
    }

    @Test
    fun transportQuestionPullsUpTheTransportPassage() {
        val hits = RetrievalEngine.retrieve(
            corpus, "Goa", emptyList(), "how do we get around, should we hire a scooter", topK = 3
        )
        assertTrue(hits.map { it.chunk.id }.contains("goa-local_transport"))
    }

    @Test
    fun emptyInterestsStillReturnASensibleDefaultSet() {
        val hits = RetrievalEngine.retrieve(corpus, "Goa", emptyList(), "", topK = 3)
        assertEquals(3, hits.size)
        val ids = hits.map { it.chunk.topic }
        assertTrue("Should fall back to the highest-prior topics", ids.contains("costs"))
        assertTrue(ids.contains("best_time"))
    }

    @Test
    fun topKIsRespectedAndNeverExceedsTheCorpus() {
        assertEquals(2, RetrievalEngine.retrieve(corpus, "Goa", emptyList(), "", topK = 2).size)
        assertEquals(6, RetrievalEngine.retrieve(corpus, "Goa", emptyList(), "", topK = 10).size)
        assertEquals(0, RetrievalEngine.retrieve(corpus, "Goa", emptyList(), "", topK = 0).size)
    }

    // ────────────────────────────── ranking ──────────────────────────────

    @Test
    fun resultsAreSortedByDescendingScoreAndScoresAreFinite() {
        val hits = RetrievalEngine.retrieve(corpus, "Goa", listOf("food", "beaches"), "cheap trip", topK = 6)
        hits.forEach { assertTrue("Score must be finite", it.score.isFinite()) }
        for (i in 1 until hits.size) {
            assertTrue("Results are not sorted", hits[i].score <= hits[i - 1].score)
        }
    }

    @Test
    fun aMatchingQueryProducesANonZeroLexicalScore() {
        val hits = RetrievalEngine.retrieve(corpus, "Goa", listOf("food"), "xacuti bebinca fish curry", topK = 6)
        val food = hits.first { it.chunk.id == "goa-food" }
        assertTrue("A direct term match should score above zero", food.lexicalScore > 0.0)
    }

    @Test
    fun anUnrelatedQueryLeavesLexicalScoreAtZero() {
        val hits = RetrievalEngine.retrieve(corpus, "Goa", emptyList(), "zzzz qqqq", topK = 6)
        assertTrue(hits.all { it.lexicalScore == 0.0 })
    }

    // ───────────────────────────── tokenising ─────────────────────────────

    @Test
    fun tokenizerDropsStopWordsShortTokensAndPunctuation() {
        val tokens = RetrievalEngine.tokenize("The beaches, and the FOOD in Goa!")
        assertFalse(tokens.contains("the"))
        assertFalse(tokens.contains("and"))
        assertFalse(tokens.contains("in"))
        assertTrue(tokens.contains("goa"))
        assertTrue(tokens.any { it.startsWith("beach") })
    }

    @Test
    fun tokenizerStemsPluralsSoSingularAndPluralMatch() {
        // A blanket "es" rule breaks this: "temples" would stem to "templ" while
        // "temple" stayed whole, and the two would never match.
        listOf(
            "beach" to "beaches",
            "temple" to "temples",
            "cost" to "costs",
            "market" to "markets",
            "activity" to "activities",
            "bus" to "buses"
        ).forEach { (singular, plural) ->
            assertEquals(
                "'$singular' and '$plural' must stem to the same token",
                RetrievalEngine.tokenize(singular),
                RetrievalEngine.tokenize(plural)
            )
        }
    }

    @Test
    fun stemmerLeavesDoubleSWordsAlone() {
        assertEquals("business", RetrievalEngine.stemForTest("business"))
        assertEquals("pass", RetrievalEngine.stemForTest("pass"))
    }

    @Test
    fun tokenizerHandlesNullAndBlankInput() {
        assertTrue(RetrievalEngine.tokenize(null).isEmpty())
        assertTrue(RetrievalEngine.tokenize("   ").isEmpty())
    }

    // ──────────────────── the corpus that actually ships ────────────────────

    private fun shippedCorpus(): List<KbChunk>? {
        val candidates = listOf(
            File("src/main/assets/kb/knowledge_base.json"),
            File("app/src/main/assets/kb/knowledge_base.json")
        )
        val file = candidates.firstOrNull { it.exists() } ?: return null
        val parsed = Gson().fromJson(file.readText(), KnowledgeBaseFile::class.java)
        return parsed.chunks
    }

    @Test
    fun shippedKnowledgeBaseIsWellFormed() {
        val chunks = shippedCorpus()
        assumeTrue("Shipped knowledge base not reachable from this working directory", chunks != null)
        requireNotNull(chunks)

        assertTrue("Expected at least 15 passages, found ${chunks.size}", chunks.size >= 15)
        assertEquals("Passage ids must be unique", chunks.size, chunks.map { it.id }.toSet().size)

        chunks.forEach { c ->
            assertTrue("${c.id} has no destination id", c.destinationId.isNotBlank())
            assertTrue("${c.id} has no source", c.sourceName.isNotBlank())
            assertTrue("${c.id} has no as-of date", c.asOf.isNotBlank())
            assertTrue("${c.id} text is too short", c.text.split(" ").size >= 40)
        }
    }

    @Test
    fun everyCostPassageIsLabelledAsAnEstimate() {
        val chunks = shippedCorpus()
        assumeTrue("Shipped knowledge base not reachable", chunks != null)
        requireNotNull(chunks)

        val costChunks = chunks.filter { it.topic == "costs" }
        assertTrue("The corpus should contain cost passages", costChunks.isNotEmpty())
        costChunks.forEach { c ->
            assertNotNull("${c.id} must declare a cost basis", c.costBasis)
            assertTrue("${c.id} cost basis must say ESTIMATE",
                c.costBasis!!.contains("ESTIMATE"))
            assertTrue("${c.id} body must state that figures are estimates",
                c.text.contains("ESTIMATE"))
        }
    }

    @Test
    fun everyCoveredDestinationHasPassagesInTheShippedCorpus() {
        val chunks = shippedCorpus()
        assumeTrue("Shipped knowledge base not reachable", chunks != null)
        requireNotNull(chunks)

        RetrievalEngine.coveredDestinations().forEach { id ->
            assertTrue("No passages shipped for destination '$id'",
                chunks.any { it.destinationId == id })
        }
    }

    @Test
    fun retrievalOverTheShippedCorpusStaysWithinOneDestination() {
        val chunks = shippedCorpus()
        assumeTrue("Shipped knowledge base not reachable", chunks != null)
        requireNotNull(chunks)

        val hits = RetrievalEngine.retrieve(
            chunks, "Alleppey", listOf("relaxation"), "overnight houseboat on the backwaters", topK = 4
        )
        assertEquals(4, hits.size)
        assertTrue(hits.all { it.chunk.destinationId == "kerala" })
    }
}

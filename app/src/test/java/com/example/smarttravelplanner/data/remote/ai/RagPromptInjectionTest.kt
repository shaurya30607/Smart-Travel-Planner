package com.example.smarttravelplanner.data.remote.ai

import com.example.smarttravelplanner.data.rag.KbChunk
import com.example.smarttravelplanner.data.rag.RetrievedChunk
import org.junit.Assert.*
import org.junit.Test

/**
 * The augmentation half of TripWeaver's RAG pipeline.
 *
 * These tests pin down two things that matter more than retrieval quality:
 *  - when nothing is retrieved, the prompt must be byte-for-byte what it was before
 *  - when something is retrieved, the passage text, its source and its cost labelling
 *    must all reach the model
 */
class RagPromptInjectionTest {

    private fun passage(
        id: String = "goa-costs",
        destination: String = "Goa",
        topic: String = "costs",
        text: String = "ESTIMATE, per person per day. Budget travel is 1,500 to 2,500 rupees.",
        costBasis: String? = "ESTIMATE - per person per day, INR"
    ) = RetrievedChunk(
        chunk = KbChunk(
            id = id,
            destination = destination,
            destinationId = destination.lowercase(),
            topic = topic,
            title = "Typical $destination costs",
            text = text,
            sourceName = "Goa Tourism Development Corporation",
            sourceUrl = "https://goatourism.gov.in/",
            asOf = "2026-10",
            costBasis = costBasis
        ),
        lexicalScore = 1.0,
        score = 1.9
    )

    // ───────────────────────── nothing retrieved ─────────────────────────

    @Test
    fun emptyRetrievalProducesAnEmptyBlock() {
        assertEquals("", ItineraryPromptBuilder.buildRetrievedContextBlock(emptyList()))
    }

    @Test
    fun promptIsUnchangedWhenNothingWasRetrieved() {
        // This is the safety property: an uncovered destination must behave exactly
        // as the app did before retrieval existed.
        val before = ItineraryPromptBuilder.buildUserMessage(
            destination = "Paris", days = 3, budget = 60000.0,
            interests = listOf("food"), travelStyle = "balanced",
            startDate = "", notes = "", weatherForecast = null
        )
        val after = ItineraryPromptBuilder.buildUserMessage(
            destination = "Paris", days = 3, budget = 60000.0,
            interests = listOf("food"), travelStyle = "balanced",
            startDate = "", notes = "", weatherForecast = null,
            retrievedContext = ItineraryPromptBuilder.buildRetrievedContextBlock(emptyList())
        )
        assertEquals(before, after)
    }

    // ───────────────────────── something retrieved ─────────────────────────

    @Test
    fun blockCarriesPassageTextSourceAndDate() {
        val block = ItineraryPromptBuilder.buildRetrievedContextBlock(listOf(passage()))
        assertTrue(block.contains("Budget travel is 1,500 to 2,500 rupees"))
        assertTrue(block.contains("Goa Tourism Development Corporation"))
        assertTrue(block.contains("2026-10"))
        assertTrue(block.contains("Goa"))
    }

    @Test
    fun blockNumbersPassagesForCitation() {
        val block = ItineraryPromptBuilder.buildRetrievedContextBlock(
            listOf(passage(id = "goa-costs"), passage(id = "goa-food", topic = "food", costBasis = null))
        )
        assertTrue(block.contains("[1]"))
        assertTrue(block.contains("[2]"))
    }

    @Test
    fun costPassagesCarryTheirEstimateLabelIntoThePrompt() {
        val block = ItineraryPromptBuilder.buildRetrievedContextBlock(listOf(passage()))
        assertTrue("The model must be told these are estimates", block.contains("ESTIMATE"))
        assertTrue(block.contains("not live prices"))
    }

    @Test
    fun nonCostPassagesDoNotGetACostLabel() {
        val block = ItineraryPromptBuilder.buildRetrievedContextBlock(
            listOf(passage(topic = "food", text = "Fish curry rice and bebinca.", costBasis = null))
        )
        assertFalse(block.contains("["+"ESTIMATE - per person"))
    }

    @Test
    fun blockInstructsTheModelNotToContradictOrInvent() {
        val block = ItineraryPromptBuilder.buildRetrievedContextBlock(listOf(passage()))
        assertTrue(block.contains("Do not contradict a passage"))
        assertTrue(block.contains("Do not invent additional sources"))
    }

    @Test
    fun retrievedContextActuallyReachesTheUserMessage() {
        val block = ItineraryPromptBuilder.buildRetrievedContextBlock(listOf(passage()))
        val message = ItineraryPromptBuilder.buildUserMessage(
            destination = "Goa", days = 4, budget = 40000.0,
            interests = listOf("Local Food"), travelStyle = "balanced",
            startDate = "", notes = "", weatherForecast = null,
            retrievedContext = block
        )
        assertTrue("The context block must appear in the prompt sent to the model",
            message.contains("RETRIEVED TRAVEL CONTEXT"))
        assertTrue(message.contains("Goa Tourism Development Corporation"))
        // the original prompt content must survive alongside it
        assertTrue(message.contains("DESTINATION: Goa"))
        assertTrue(message.contains("Output ONLY the raw JSON"))
    }

    @Test
    fun blockDoesNotLeakIntoTheSystemPrompt() {
        // The system prompt defines the output schema and must stay untouched.
        assertFalse(ItineraryPromptBuilder.itinerarySystemPrompt.contains("RETRIEVED TRAVEL CONTEXT"))
    }
}

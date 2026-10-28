package com.example.smarttravelplanner.data.rag

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the curated travel knowledge base from `assets/kb/knowledge_base.json`.
 *
 * The file is small (around 30 passages) so it is parsed once and held in memory.
 * A failure to read or parse it is not fatal: the app falls back to an empty corpus,
 * retrieval returns nothing, and itinerary generation continues exactly as it did
 * before TripWeaver's RAG step existed.
 */
@Singleton
class KnowledgeBase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    @Volatile
    private var cached: List<KbChunk>? = null

    @Volatile
    private var notice: String = ""

    /** Every passage in the corpus, or an empty list if the asset could not be read. */
    suspend fun chunks(): List<KbChunk> {
        cached?.let { return it }
        return withContext(Dispatchers.IO) {
            val loaded = runCatching {
                val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
                val parsed = gson.fromJson(json, KnowledgeBaseFile::class.java)
                notice = parsed?.notice.orEmpty()
                parsed?.chunks.orEmpty().filter { it.id.isNotBlank() && it.text.isNotBlank() }
            }.getOrElse { error ->
                android.util.Log.w(TAG, "Knowledge base unavailable; continuing without retrieval.", error)
                emptyList()
            }
            cached = loaded
            loaded
        }
    }

    /** The corpus-level disclaimer, shown alongside sources in the UI. */
    fun notice(): String = notice

    /** True when the corpus covers this destination, for diagnostics and settings. */
    suspend fun covers(destination: String?): Boolean {
        val id = RetrievalEngine.matchDestination(destination) ?: return false
        return chunks().any { it.destinationId == id }
    }

    companion object {
        private const val TAG = "KnowledgeBase"
        private const val ASSET_PATH = "kb/knowledge_base.json"
    }
}

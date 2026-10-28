package com.example.smarttravelplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.data.rag.RagSourceRegistry

/**
 * Shows the knowledge-base passages that were retrieved and placed in front of the
 * model before this itinerary was generated — the "augmented" half of TripWeaver's
 * retrieval-augmented generation, made visible to the traveller.
 *
 * Renders nothing at all when the trip was generated without retrieval, which is the
 * case for destinations outside the curated corpus and for trips generated before
 * this feature existed.
 */
@Composable
fun RagSourcesCard(
    tripId: Long,
    modifier: Modifier = Modifier
) {
    val contexts by RagSourceRegistry.contexts.collectAsState()
    val context = contexts[tripId] ?: return
    if (context.isEmpty) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            Text(
                text = "Sources used for this plan",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${context.passages.size} passage(s) from the curated travel knowledge base " +
                    "were retrieved for ${context.requestedDestination} and given to the AI before " +
                    "it planned this trip.",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            context.passages.forEachIndexed { index, retrieved ->
                val chunk = retrieved.chunk
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "[${index + 1}]",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${chunk.destination} · ${chunk.topicLabel()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${chunk.sourceName} · as of ${chunk.asOf}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        chunk.costBasis?.let { basis ->
                            Text(
                                text = basis,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = "Retrieval: ${context.retrievalMode.lowercase()} ranking over a curated corpus. " +
                    "Cost figures above are estimates compiled for this project, not live prices.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

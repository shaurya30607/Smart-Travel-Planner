package com.example.smarttravelplanner.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.smarttravelplanner.domain.model.ExploreDestination
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

/**
 * Bottom sheet shown when a destination card is tapped on the Explore screen.
 * Shows the destination's details and a shortcut to plan a trip there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationDetailsSheet(
    destination: ExploreDestination,
    /** Whether the current month is within the destination's best season. */
    isInSeason: Boolean = false,
    /** e.g. "~1,050 km from Mumbai (straight line)"; null hides the row. */
    distanceFromLabel: String? = null,
    onDismiss: () -> Unit,
    onPlanTrip: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // ── Hero image with name overlay ────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                val context = LocalContext.current
                val imageRequest = remember(destination.imageRes) {
                    ImageRequest.Builder(context)
                        .data(destination.imageRes)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = destination.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0x05000000),
                                    0.45f to Color(0x33000000),
                                    1.0f to Color(0xEE0B111B)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = destination.name,
                        fontFamily = PlayfairFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = Color.White
                    )
                    Text(
                        text = destination.stateCountry,
                        fontFamily = DmSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Tag pill ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF3A2810))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = destination.tag,
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Amber
                )
            }

            // ── Distance from the selected "From" city ───────────────────────
            if (distanceFromLabel != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.NearMe,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = distanceFromLabel,
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        color = OnBackgroundGray
                    )
                }
            }

            // ── Description ─────────────────────────────────────────────────
            if (destination.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = destination.description,
                    fontFamily = DmSansFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Quick facts ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FactCard(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "Ideal duration",
                    value = destination.idealDuration.removeSuffix(" ideal"),
                    modifier = Modifier.weight(1f)
                )
                FactCard(
                    icon = Icons.Outlined.WbSunny,
                    label = if (isInSeason) "Best time · in season now" else "Best time",
                    value = destination.bestSeason.removePrefix("Best: "),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Primary action ──────────────────────────────────────────────
            Button(
                onClick = onPlanTrip,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FlightTakeoff,
                    contentDescription = null,
                    tint = Color(0xFF1A1A1A),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Plan a trip to ${destination.name}",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1A1A1A)
                )
            }
        }
    }
}

@Composable
private fun FactCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Amber,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                fontFamily = DmSansFontFamily,
                fontSize = 11.sp,
                color = OnBackgroundGray
            )
            Text(
                text = value,
                fontFamily = DmSansFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

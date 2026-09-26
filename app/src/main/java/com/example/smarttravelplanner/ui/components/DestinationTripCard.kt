package com.example.smarttravelplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.smarttravelplanner.domain.model.Trip
import com.example.smarttravelplanner.ui.theme.*
import com.example.smarttravelplanner.util.DestinationImageHelper
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Modular destination trip card component for displaying domain [Trip] items across the app.
 *
 * Features:
 * - High-resolution destination cover photo with Coil crossfade
 * - Dark multi-stop vertical gradient overlay ensuring high text legibility
 * - "Planning" (or custom) status badge pill at top-left
 * - Optional trashcan delete button at top-right with confirmation dialog (White icon on dark translucent circle)
 * - Elegant Playfair serif destination title and clean DM Sans metadata
 * - Calendar icon with formatted date range and duration nights
 * - Clean layout without distracting price tags
 *
 * @param trip The domain [Trip] to render.
 * @param modifier Custom modifier applied to the root card container.
 * @param cardHeight Height of the card (default 210.dp for ample vertical breathing room).
 * @param statusText Status badge text (default "Planning").
 * @param onDelete Optional delete callback. If provided, renders the trashcan action button.
 * @param onClick Triggered when the card is clicked.
 */
@Composable
fun DestinationTripCard(
    trip: Trip,
    modifier: Modifier = Modifier,
    cardHeight: Dp = 210.dp,
    statusText: String = "Planning",
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val fmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()) }
    val durationDays = remember(trip.startDate, trip.endDate) {
        val days = trip.endDate.toEpochDay() - trip.startDate.toEpochDay()
        if (days <= 0) 1 else days.toInt()
    }
    val dateText = remember(trip.startDate, trip.endDate) {
        "${trip.startDate.format(fmt)} – ${trip.endDate.format(fmt)} · $durationDays nights"
    }
    val imageModel = remember(trip.destination, trip.coverImageUrl) {
        DestinationImageHelper.getDestinationImage(trip.destination, trip.coverImageUrl)
    }

    DestinationTripCardBase(
        title = trip.destination,
        subtitle = trip.title.ifBlank { null },
        dateText = dateText,
        imageModel = imageModel,
        modifier = modifier,
        cardHeight = cardHeight,
        statusText = statusText,
        onDelete = onDelete,
        deleteConfirmTitle = "Delete Trip",
        deleteConfirmMessage = "Are you sure you want to delete this trip to ${trip.destination}?",
        onClick = onClick
    )
}

/**
 * Base visual destination card that can render either a domain [Trip] or static showcase items.
 *
 * Decouples the presentation styling from any specific model type, making it highly reusable
 * and easy to maintain across different screens and future features.
 */
@Composable
fun DestinationTripCardBase(
    title: String,
    subtitle: String?,
    dateText: String,
    imageModel: Any,
    modifier: Modifier = Modifier,
    cardHeight: Dp = 210.dp,
    statusText: String = "Planning",
    onDelete: (() -> Unit)? = null,
    deleteConfirmTitle: String = "Delete Trip",
    deleteConfirmMessage: String = "Are you sure you want to delete this trip?",
    onClick: () -> Unit = {}
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        val context = LocalContext.current
        val imageRequest = remember(imageModel) {
            ImageRequest.Builder(context)
                .data(imageModel)
                .setHeader("User-Agent", "SmartTravelPlanner/1.0 (Android travel planner app)")
                .crossfade(true)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

        // ── 2. Readability Gradient Scrim ────────────────────────────────────
        // In dark mode: deep atmospheric overlay.
        // In light mode: dark veil removed; clear transparent image body with minimal soft bottom vignette.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    if (isDark) {
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x66000000),
                                0.45f to Color(0x77000000),
                                1.0f to Color(0xF00B111B)
                            )
                        )
                    } else {
                        // Light mode: subtle tint across image + deep bottom contrast for effortless text readability
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.25f),
                                0.35f to Color.Black.copy(alpha = 0.10f),
                                0.60f to Color.Black.copy(alpha = 0.38f),
                                1.0f to Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    }
                )
        )

        // ── 3. Foreground Content ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Status badge & optional delete action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status pill
                val isPlanning = statusText.equals("Planning", ignoreCase = true)
                val badgeBg = if (isPlanning) {
                    if (isDark) Amber.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.40f)
                } else {
                    if (isDark) CompletedGreenBg else Color.Black.copy(alpha = 0.40f)
                }
                val badgeBorder = if (isPlanning) {
                    if (isDark) Amber.copy(alpha = 0.45f) else Amber.copy(alpha = 0.60f)
                } else {
                    if (isDark) Color.Transparent else CompletedGreen.copy(alpha = 0.60f)
                }
                val badgeTextColor = if (isPlanning) AmberLight else CompletedGreen

                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(20.dp))
                        .border(0.5.dp, badgeBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusText,
                        fontFamily = DmSansFontFamily,
                        color = badgeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Delete action button (white icon on dark translucent circle)
                if (onDelete != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Trip",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Bottom Trip Details: Destination, Title/Subtitle, and Date Range
            Column {
                Text(
                    text = title,
                    fontFamily = PlayfairFontFamily,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.65f),
                            blurRadius = 8f
                        )
                    )
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        fontFamily = DmSansFontFamily,
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 13.sp,
                        style = LocalTextStyle.current.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.65f),
                                blurRadius = 6f
                            )
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateText,
                        fontFamily = DmSansFontFamily,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        style = LocalTextStyle.current.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.65f),
                                blurRadius = 6f
                            )
                        )
                    )
                }
            }
        }
    }

    // ── 4. Delete Confirmation Dialog ─────────────────────────────────────────
    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = deleteConfirmTitle,
                    fontFamily = PlayfairFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = deleteConfirmMessage,
                    fontFamily = DmSansFontFamily
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

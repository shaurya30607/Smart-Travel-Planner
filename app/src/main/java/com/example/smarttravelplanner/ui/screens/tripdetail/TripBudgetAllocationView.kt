package com.example.smarttravelplanner.ui.screens.tripdetail

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.model.ActivityCategory
import com.example.smarttravelplanner.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  Data model representing an allocation category with a premium vector symbol
// ─────────────────────────────────────────────────────────────────────────────
private data class BudgetCategoryItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val amount: Double,
    val percentage: Int
)

@Composable
fun TripBudgetAllocationView(
    uiState: TripDetailUiState,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val trip = uiState.trip
    val totalBudget = uiState.totalBudget
    val totalEstimated = uiState.totalEstimatedCost

    // Determine the base budget to display (matching the reference mock if empty)
    val displayTotal = when {
        totalEstimated > 0.0 -> totalEstimated
        totalBudget > 0.0    -> totalBudget
        else                 -> 42500.0
    }

    val daysDiff = if (trip != null) (trip.endDate.toEpochDay() - trip.startDate.toEpochDay()).toInt() else 4
    val days = (daysDiff + 1).coerceAtLeast(1)
    val adults = trip?.adults ?: 1
    val kids = trip?.kids ?: 0
    val travellers = (adults + kids).coerceAtLeast(1)
    val perDay = (displayTotal / days).toInt()
    val perPerson = (displayTotal / travellers).toInt()

    // Prepare standard 5 categories with premium Material 3 vector symbols
    val categoryExpenses = uiState.categoryExpenses
    val totalCatSpend = categoryExpenses.values.sum()

    val categories: List<BudgetCategoryItem> = remember(displayTotal, totalCatSpend, categoryExpenses) {
        if (totalCatSpend > 0.0) {
            // Map actual activities if recorded with non-zero expenses
            val accSpend = categoryExpenses[ActivityCategory.ACCOMMODATION] ?: (displayTotal * 0.35)
            val foodSpend = categoryExpenses[ActivityCategory.FOOD] ?: (displayTotal * 0.20)
            val transSpend = categoryExpenses[ActivityCategory.TRANSPORT] ?: (displayTotal * 0.20)
            val actSpend = (categoryExpenses[ActivityCategory.ATTRACTION] ?: 0.0) +
                    (categoryExpenses[ActivityCategory.CULTURE] ?: 0.0) +
                    (categoryExpenses[ActivityCategory.NATURE] ?: 0.0).let { if (it > 0) it else (displayTotal * 0.15) }
            val shopSpend = (categoryExpenses[ActivityCategory.SHOPPING] ?: 0.0) +
                    (categoryExpenses[ActivityCategory.OTHER] ?: 0.0).let { if (it > 0) it else (displayTotal * 0.10) }

            val total = (accSpend + foodSpend + transSpend + actSpend + shopSpend).coerceAtLeast(1.0)
            listOf(
                BudgetCategoryItem("Accommodation", Icons.Outlined.Hotel, Color(0xFFF59E0B), accSpend, ((accSpend / total) * 100).toInt().coerceAtLeast(5)),
                BudgetCategoryItem("Food & Dining", Icons.Outlined.Restaurant, Color(0xFF06B6D4), foodSpend, ((foodSpend / total) * 100).toInt().coerceAtLeast(5)),
                BudgetCategoryItem("Transport", Icons.Outlined.DirectionsCar, Color(0xFF8B5CF6), transSpend, ((transSpend / total) * 100).toInt().coerceAtLeast(5)),
                BudgetCategoryItem("Activities", Icons.Outlined.LocalActivity, Color(0xFF10B981), actSpend, ((actSpend / total) * 100).toInt().coerceAtLeast(5)),
                BudgetCategoryItem("Shopping", Icons.Outlined.ShoppingBag, Color(0xFFF43F5E), shopSpend, ((shopSpend / total) * 100).toInt().coerceAtLeast(5))
            )
        } else {
            // Default elegant allocation matching design spec with premium symbols
            listOf(
                BudgetCategoryItem("Accommodation", Icons.Outlined.Hotel, Color(0xFFF59E0B), displayTotal * 0.35, 35),
                BudgetCategoryItem("Food & Dining", Icons.Outlined.Restaurant, Color(0xFF06B6D4), displayTotal * 0.20, 20),
                BudgetCategoryItem("Transport", Icons.Outlined.DirectionsCar, Color(0xFF8B5CF6), displayTotal * 0.20, 20),
                BudgetCategoryItem("Activities", Icons.Outlined.LocalActivity, Color(0xFF10B981), displayTotal * 0.15, 15),
                BudgetCategoryItem("Shopping", Icons.Outlined.ShoppingBag, Color(0xFFF43F5E), displayTotal * 0.10, 10)
            )
        }
    }

    val inFormatter = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN")) }

    // Colors
    val cardBackground = if (isDark) SurfaceElevated else Color(0xFFFBFBFB)
    val cardBorder = if (isDark) CardBorderDark else CardBorderLight
    val labelColor = if (isDark) Color(0xFF8E95A5) else Color(0xFF6B7280)
    val subtextColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. Total Estimated Budget Card ─────────────────────────────────────
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = cardBackground,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 0.dp else 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Subtle large watermark currency symbol in top-right
                    Text(
                        text = "₹",
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Light,
                        color = if (isDark) Color(0xFF262C3A).copy(alpha = 0.55f) else Color(0xFFE2E8F0),
                        modifier = Modifier.align(Alignment.TopEnd)
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Section Tag
                        Text(
                            text = "TOTAL ESTIMATED BUDGET",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.3.sp,
                            fontFamily = DmSansFontFamily,
                            color = labelColor
                        )

                        Spacer(Modifier.height(6.dp))

                        // Large Display Amount in Playfair font & warm gold
                        Text(
                            text = "₹${inFormatter.format(displayTotal.toInt())}",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlayfairFontFamily,
                            color = Color(0xFFF59E0B)
                        )

                        Spacer(Modifier.height(4.dp))

                        // Subtitle
                        Text(
                            text = "for $days days • Mid-Range tier • $travellers travellers",
                            fontSize = 13.sp,
                            fontFamily = DmSansFontFamily,
                            color = subtextColor
                        )

                        Spacer(Modifier.height(20.dp))

                        // 2-Column Metrics Row (Savings tip completely removed as requested)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: Per day
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Per day",
                                    fontSize = 12.sp,
                                    fontFamily = DmSansFontFamily,
                                    color = labelColor
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "₹${inFormatter.format(perDay)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = DmSansFontFamily,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Clean single vertical divider
                            VerticalDivider(
                                modifier = Modifier
                                    .height(34.dp)
                                    .padding(horizontal = 14.dp),
                                color = cardBorder,
                                thickness = 0.8.dp
                            )

                            // Column 2: Per person
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                val passengerTag = when {
                                    kids > 0 -> " ($adults Ad, $kids Kd)"
                                    travellers > 1 -> " ($travellers pax)"
                                    else -> " (1 pax)"
                                }
                                Text(
                                    text = "Per person$passengerTag",
                                    fontSize = 12.sp,
                                    fontFamily = DmSansFontFamily,
                                    color = labelColor
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "₹${inFormatter.format(perPerson)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = DmSansFontFamily,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 2. Allocation At A Glance Card ────────────────────────────────────
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = cardBackground,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 0.dp else 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "ALLOCATION AT A GLANCE",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp,
                        fontFamily = DmSansFontFamily,
                        color = labelColor
                    )

                    Spacer(Modifier.height(14.dp))

                    // Multi-segment Horizontal Progress Bar (Spaced Rounded Pills with Material 3 Expressive spring physics)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(11.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { item ->
                            val animatedWeight by animateFloatAsState(
                                targetValue = item.percentage.toFloat(),
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "seg_weight_${item.title}"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(animatedWeight.coerceAtLeast(1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(item.color)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Legend with colored dots
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Row 1: Accommodation, Food & Dining, Transport
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            categories.take(3).forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(item.color)
                                    )
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontFamily = DmSansFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        // Row 2: Activities, Shopping
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            categories.drop(3).forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(item.color)
                                    )
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontFamily = DmSansFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 3. Individual Category Breakdown Cards (Using Premium Vector Symbols) ───
        items(categories.size) { idx ->
            val item = categories[idx]
            CategoryProgressCard(
                item = item,
                formattedAmount = inFormatter.format(item.amount.toInt()),
                cardBackground = cardBackground,
                cardBorder = cardBorder,
                isDark = isDark
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Category Progress Card (With Premium Outlined Vector Icon Container)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryProgressCard(
    item: BudgetCategoryItem,
    formattedAmount: String,
    cardBackground: Color,
    cardBorder: Color,
    isDark: Boolean
) {
    val progressFraction = (item.percentage / 100f).coerceIn(0.04f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessLow
        ),
        label = "cat_progress_${item.title}"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBackground,
        border = BorderStroke(0.8.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Header Row: Premium Symbol + Title on left; Amount + % on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Subtle Outline Vector Symbol + Category Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (isDark) item.color.copy(alpha = 0.90f) else item.color.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = item.title,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = DmSansFontFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Right: Amount and Percentage
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "₹$formattedAmount",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.percentage}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = DmSansFontFamily,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Custom Linear Progress Bar with smooth rounded corners
            val trackColor = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.5.dp))
                    .background(trackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(item.color)
                )
            }
        }
    }
}

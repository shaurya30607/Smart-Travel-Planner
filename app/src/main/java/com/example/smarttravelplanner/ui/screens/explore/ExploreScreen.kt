package com.example.smarttravelplanner.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.model.ExploreDestination
import com.example.smarttravelplanner.domain.model.ExploreSortOrder
import com.example.smarttravelplanner.domain.util.GeoDistance
import com.example.smarttravelplanner.ui.theme.*
import androidx.compose.material3.MaterialTheme

private data class CategoryChip(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private val categories = listOf(
    CategoryChip("all", "All", Icons.Filled.Public),
    CategoryChip("religious", "Religious", Icons.Outlined.Church),
    CategoryChip("cultural", "Cultural", Icons.Outlined.AccountBalance),
    CategoryChip("nature", "Nature", Icons.Outlined.Forest),
    CategoryChip("adventure", "Adventure", Icons.Outlined.Terrain)
)


/** Accent for "In season" badges and the in-season filter chip. */
internal val InSeasonGreen = Color(0xFF4ADE80)

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = hiltViewModel(),
    onMenuClick: () -> Unit = {},
    onPlanTrip: (destinationName: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery = uiState.searchQuery
    val selectedCategory = uiState.selectedCategory
    val selectedCity = uiState.selectedCity
    val filteredDestinations = uiState.destinations

    val listState = rememberLazyListState()
    val scrollOffsetProvider: () -> Float = remember {
        {
            if (listState.firstVisibleItemIndex > 0) {
                1000f
            } else {
                listState.firstVisibleItemScrollOffset.toFloat()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(68.dp))

                // Search Bar + From Selector Row
                val cities = uiState.departureCities
                var showCityMenu by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // From Selector Card with Dropdown
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { showCityMenu = true }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "From",
                                    fontSize = 10.sp,
                                    color = OnBackgroundGray,
                                    fontFamily = DmSansFontFamily
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedCity,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = DmSansFontFamily
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select City",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showCityMenu,
                            onDismissRequest = { showCityMenu = false },
                            offset = DpOffset(x = 0.dp, y = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .width(340.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                        ) {
                            cities.forEachIndexed { index, city ->
                                val isSelected = city == selectedCity
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onCitySelected(city)
                                            showCityMenu = false
                                        }
                                        .padding(horizontal = 20.dp, vertical = 15.dp)
                                ) {
                                    Text(
                                        text = city,
                                        fontFamily = DmSansFontFamily,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Amber else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp
                                    )
                                }
                                if (index < cities.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        thickness = 0.8.dp
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Search Destinations Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = if (searchQuery.isNotEmpty()) Amber else OnBackgroundGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = viewModel::onSearchQueryChange,
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(Amber),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search destinations",
                                                color = OnBackgroundGray,
                                                fontSize = 14.sp,
                                                fontFamily = DmSansFontFamily
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onSearchQueryChange("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = OnBackgroundGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Category Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Amber else MaterialTheme.colorScheme.surface)
                                .clickable { viewModel.onCategorySelected(cat.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.label,
                                    tint = if (isSelected) Color(0xFF1A1A1A) else Color(0xFF8E9BAE),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = cat.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF1A1A1A) else Color(0xFF8E9BAE),
                                    fontFamily = DmSansFontFamily
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // In-season toggle + sort toggle, then result count
                if (!uiState.isLoading && uiState.errorMessage == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "In season now" filter
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (uiState.inSeasonOnly) InSeasonGreen
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { viewModel.onInSeasonToggled() }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WbSunny,
                                contentDescription = null,
                                tint = if (uiState.inSeasonOnly) Color(0xFF0B111B) else InSeasonGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "In season",
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = if (uiState.inSeasonOnly) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.inSeasonOnly) Color(0xFF0B111B) else Color(0xFF8E9BAE)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        ExploreSortOrder.entries.forEach { order ->
                            val isSelected = uiState.sortOrder == order
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Amber else MaterialTheme.colorScheme.surface)
                                    .clickable { viewModel.onSortOrderSelected(order) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = order.label,
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF1A1A1A) else Color(0xFF8E9BAE)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (uiState.inSeasonOnly) {
                            "${filteredDestinations.size} destinations in season this month"
                        } else {
                            "${filteredDestinations.size} destinations"
                        },
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        color = OnBackgroundGray,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Amber)
                    }
                }
            } else if (uiState.errorMessage != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            fontFamily = DmSansFontFamily,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = viewModel::loadDestinations,
                            colors = ButtonDefaults.buttonColors(containerColor = Amber)
                        ) {
                            Text("Retry", color = Color(0xFF1A1A1A), fontFamily = DmSansFontFamily)
                        }
                    }
                }
            } else if (filteredDestinations.isEmpty()) {
                // Empty Search Results State
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            tint = OnBackgroundGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No destinations found",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try searching for a different city, state, or category.",
                            fontFamily = DmSansFontFamily,
                            fontSize = 13.sp,
                            color = OnBackgroundGray,
                            textAlign = TextAlign.Center
                        )
                        if (uiState.hasActiveFilters) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = viewModel::clearFilters,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E2633),
                                    contentColor = Amber
                                )
                            ) {
                                Text(
                                    text = "Clear Filters",
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Destination Cards List with stable keys
                items(filteredDestinations, key = { it.name }) { dest ->
                    ExploreDestinationCard(
                        destination = dest,
                        distanceLabel = uiState.distancesKm[dest.name]?.let(GeoDistance::formatKm),
                        isInSeason = dest.name in uiState.inSeasonNames,
                        onClick = { viewModel.onDestinationClick(dest) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // ── Pinned TopBar with Hamburger & Scrolled Right-Aligned "Explore India" Title ──
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(64.dp)
        ) {
            // Cloudy soft gradient backdrop (fades in smoothly as user scrolls)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val offset = scrollOffsetProvider()
                        val topBarAlpha = (offset / 90f).coerceIn(0f, 1f)
                        alpha = topBarAlpha
                    }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.82f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // TopBar Controls
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Hamburger Menu Button (Top-Left, always pinned and clickable)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), CircleShape)
                        .clickable { onMenuClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
                val indiaColor = if (isLightMode) AmberDark else Amber

                // Right-aligned "Explore India" title (always visible from top, styled with premium typography)
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        ) {
                            append("Explore ")
                        }
                        withStyle(
                            SpanStyle(
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = FontStyle.Italic,
                                fontSize = 28.sp,
                                color = indiaColor
                            )
                        ) {
                            append("India")
                        }
                    }
                )
            }
        }
    }

    // ── Destination details sheet ────────────────────────────────────────────
    uiState.selectedDestination?.let { selected ->
        DestinationDetailsSheet(
            destination = selected,
            isInSeason = selected.name in uiState.inSeasonNames,
            distanceFromLabel = uiState.distancesKm[selected.name]?.let { km ->
                "~${GeoDistance.formatKm(km)} from ${uiState.selectedCity} (straight line)"
            },
            onDismiss = viewModel::onDetailsDismissed,
            onPlanTrip = {
                viewModel.onDetailsDismissed()
                onPlanTrip(selected.name)
            }
        )
    }
}

@Composable
private fun ExploreDestinationCard(
    destination: ExploreDestination,
    distanceLabel: String?,
    isInSeason: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF131923))
            .clickable(onClick = onClick)
    ) {
        // Upper Image Section with Scrim and Title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
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

            // Gradient Scrim for readable text over image
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x05000000),
                                0.40f to Color(0x33000000),
                                1.0f to Color(0xEE0B111B)
                            )
                        )
                    )
            )

            // "In season" badge
            if (isInSeason) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xB30B111B))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WbSunny,
                        contentDescription = null,
                        tint = InSeasonGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "In season",
                        fontFamily = DmSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Distance from the selected "From" city
            if (distanceLabel != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xB30B111B))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NearMe,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = distanceLabel,
                        fontFamily = DmSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Destination Name & Location overlaid on image
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = destination.name,
                    fontFamily = PlayfairFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = Color.White  // Always white - overlaid on image
                )
                Spacer(modifier = Modifier.height(2.dp))
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

        // Bottom Info Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131923))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ideal Duration
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🗓 ",
                    fontSize = 12.sp
                )
                Text(
                    text = destination.idealDuration,
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E8F0)
                )
            }

            Text(
                text = "|",
                color = Color(0xFF334155),
                fontSize = 12.sp
            )

            // Best Season
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "☀️ ",
                    fontSize = 12.sp
                )
                Text(
                    text = destination.bestSeason,
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E8F0)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Tag Pill
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
        }
    }
}

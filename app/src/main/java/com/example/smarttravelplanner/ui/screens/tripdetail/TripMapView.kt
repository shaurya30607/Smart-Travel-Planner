package com.example.smarttravelplanner.ui.screens.tripdetail

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.data.remote.api.RouteService
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.ActivityCategory
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.BackgroundDark
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun TripMapView(
    trip: Trip?,
    itinerary: Itinerary?,
    selectedActivityId: Long?,
    hasMapsApiKey: Boolean,
    onActivitySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    onMapLoaded: () -> Unit = {}
) {
    val context = LocalContext.current
    val activities = itinerary?.activities.orEmpty()
    val validPoints = remember(activities) {
        MapUtils.getValidActivityPoints(activities)
    }

    // Real street road routing points (following actual city roads 1 -> 2 -> 3 -> ...)
    var streetRoutePoints by remember(itinerary?.id, validPoints) {
        mutableStateOf<List<LatLng>>(emptyList())
    }

    LaunchedEffect(validPoints) {
        if (validPoints.size > 1) {
            val coords = validPoints.map { it.second }
            val roadPoints = RouteService.getStreetRoute(coords)
            streetRoutePoints = roadPoints
        } else {
            streetRoutePoints = emptyList()
        }
    }

    // Determine initial camera focus
    val initialTarget = remember(validPoints, trip) {
        validPoints.firstOrNull()?.second
            ?: LatLng(28.6139, 77.2090) // New Delhi default fallback
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialTarget, 12f)
    }

    var mapType by remember { mutableStateOf(MapType.NORMAL) }

    // Re-center and fit bounds whenever itinerary or points change
    LaunchedEffect(validPoints) {
        if (validPoints.size == 1) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(validPoints.first().second, 14f),
                durationMs = 800
            )
        } else if (validPoints.size > 1) {
            val bounds = MapUtils.computeBounds(validPoints.map { it.second })
            if (bounds != null) {
                try {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, 160),
                        durationMs = 900
                    )
                } catch (_: Exception) {
                    // Bounds animation may fail if map is not laid out yet
                    cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 160))
                }
            }
        }
    }

    // Animate to selected activity when user selects one
    LaunchedEffect(selectedActivityId) {
        if (selectedActivityId != null) {
            val targetPair = validPoints.firstOrNull { it.first.id == selectedActivityId }
            if (targetPair != null) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(targetPair.second, 15.5f),
                    durationMs = 600
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (validPoints.isEmpty()) {
            LaunchedEffect(Unit) {
                onMapLoaded()
            }
            // Empty state if no coordinates
            EmptyCoordinatesView(destination = trip?.destination ?: "destination")
        } else {
            // Google Map View
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = remember(mapType) {
                    MapProperties(
                        mapType = mapType,
                        isMyLocationEnabled = false
                    )
                },
                uiSettings = remember {
                    MapUiSettings(
                        zoomControlsEnabled = false,
                        compassEnabled = true,
                        myLocationButtonEnabled = false,
                        mapToolbarEnabled = false
                    )
                },
                onMapLoaded = onMapLoaded,
                onMapClick = {
                    onActivitySelected(null)
                }
            ) {
                // Draw real street-level driving route following actual roads between sequential stops
                val routeToDraw = if (streetRoutePoints.isNotEmpty()) streetRoutePoints else validPoints.map { it.second }
                if (routeToDraw.size > 1) {
                    // Underlay outline casing for crisp contrast on both normal and satellite maps
                    Polyline(
                        points = routeToDraw,
                        color = Color(0xFF0F172A).copy(alpha = 0.5f),
                        width = 10f,
                        geodesic = true
                    )
                    // Vibrant Amber main route path (thin, sleek stroke)
                    Polyline(
                        points = routeToDraw,
                        color = Amber,
                        width = 6f,
                        geodesic = true
                    )
                }


                // Draw custom numbered marker pins for each activity
                validPoints.forEachIndexed { index, (act, latLng) ->
                    val isSelected = act.id == selectedActivityId
                    val markerState = rememberUpdatedMarkerState(position = latLng)

                    MarkerComposable(
                        state = markerState,
                        title = act.title,
                        snippet = act.location,
                        zIndex = if (isSelected) 2.0f else 1.0f,
                        onClick = {
                            onActivitySelected(act.id)
                            true
                        }
                    ) {
                        NumberedMapPin(
                            sequenceNumber = index + 1,
                            category = act.category,
                            isSelected = isSelected
                        )
                    }
                }
            }

            // Fallback Banner if Maps API Key is missing in local.properties
            if (!hasMapsApiKey) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.95f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Amber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Maps Preview Mode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Add MAPS_API_KEY in local.properties to enable full Google Maps tile rendering.",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // Top Floating Map Controls (Recenter, Map Type, Launch Full Day Route)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = if (!hasMapsApiKey) 80.dp else 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Map Type toggle
                SmallFloatingActionButton(
                    onClick = {
                        mapType = if (mapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL
                    },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (mapType == MapType.NORMAL) Icons.Outlined.Layers else Icons.Filled.Layers,
                        contentDescription = "Map Type",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Recenter / Fit Day Stops
                SmallFloatingActionButton(
                    onClick = {
                        val bounds = MapUtils.computeBounds(validPoints.map { it.second })
                        if (bounds != null) {
                            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 160))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    contentColor = Amber,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Outlined.CenterFocusStrong,
                        contentDescription = "Fit Day Stops",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Full Day Route in Google Maps App
                SmallFloatingActionButton(
                    onClick = {
                        MapUtils.launchDayRoute(context, activities)
                    },
                    containerColor = Amber,
                    contentColor = BackgroundDark,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Outlined.Directions,
                        contentDescription = "Open Full Day Route",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Bottom Selected Activity Popup Card
            val selectedActivity = activities.firstOrNull { it.id == selectedActivityId }
            AnimatedVisibility(
                visible = selectedActivity != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                if (selectedActivity != null) {
                    val stopIndex = activities.indexOf(selectedActivity) + 1
                    SelectedActivityMapCard(
                        stopNumber = stopIndex,
                        activity = selectedActivity,
                        onNavigate = {
                            if (MapUtils.isValidCoord(selectedActivity.lat, selectedActivity.lng)) {
                                MapUtils.launchGoogleMapsDirections(
                                    context = context,
                                    lat = selectedActivity.lat!!,
                                    lng = selectedActivity.lng!!,
                                    label = selectedActivity.location.ifBlank { selectedActivity.title }
                                )
                            }
                        },
                        onClose = { onActivitySelected(null) }
                    )
                }
            }
        }
    }
}

/**
 * Custom styled marker pin with sequence number and category color badge.
 */
@Composable
private fun NumberedMapPin(
    sequenceNumber: Int,
    category: ActivityCategory,
    isSelected: Boolean
) {
    val categoryColor = when (category) {
        ActivityCategory.ATTRACTION -> Amber
        ActivityCategory.FOOD -> Color(0xFFFF7043)
        ActivityCategory.NATURE -> Color(0xFF66BB6A)
        ActivityCategory.CULTURE -> Color(0xFFAB47BC)
        ActivityCategory.ACCOMMODATION -> Color(0xFF42A5F5)
        ActivityCategory.SHOPPING -> Color(0xFFEC407A)
        ActivityCategory.TRANSPORT -> Color(0xFF26A69A)
        ActivityCategory.OTHER -> Color(0xFF78909C)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 38.dp else 30.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(if (isSelected) Amber else BackgroundDark)
                .border(
                    width = if (isSelected) 2.5.dp else 2.dp,
                    color = if (isSelected) Color.White else categoryColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sequenceNumber.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = if (isSelected) 14.sp else 12.sp,
                color = if (isSelected) BackgroundDark else Color.White
            )
        }

        // Pointer triangle
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(6.dp)
                .background(if (isSelected) Amber else categoryColor)
        )
    }
}

/**
 * Floating summary card when an activity marker is tapped on the map.
 */
@Composable
private fun SelectedActivityMapCard(
    stopNumber: Int,
    activity: Activity,
    onNavigate: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Amber),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stopNumber.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BackgroundDark
                        )
                    }
                    if (activity.startTime.isNotBlank()) {
                        Text(
                            text = "${activity.startTime}${if (activity.endTime.isNotBlank()) " – ${activity.endTime}" else ""}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = activity.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (activity.location.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = activity.location,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (activity.estimatedCost > 0) {
                    Text(
                        text = "₹${activity.estimatedCost.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Amber
                    )
                } else {
                    Text(
                        text = "Free / Included",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onNavigate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Navigate",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Displayed when an itinerary has no coordinates to plot.
 */
@Composable
private fun EmptyCoordinatesView(destination: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.ExploreOff,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "No Coordinates Available",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Specific GPS coordinates for $destination activities were not provided by the itinerary generator.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

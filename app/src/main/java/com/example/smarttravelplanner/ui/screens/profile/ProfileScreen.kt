package com.example.smarttravelplanner.ui.screens.profile

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.smarttravelplanner.domain.model.UserPreferences
import com.example.smarttravelplanner.ui.components.AiSettingsDialog
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily
import androidx.compose.material3.MaterialTheme
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onCloseClick: () -> Unit = {},
    onNavigateToMyProfile: () -> Unit = {},
    onNavigateToManagePayments: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val prefs = when (val state = uiState) {
        is ProfileUiState.Success -> state.preferences
        else -> UserPreferences()
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ── TOP PASSPORT CARD ─────────────────────────────────────────────
            PassportProfileCard(
                prefs = prefs,
                onCloseClick = onCloseClick,
                onEditClick = onNavigateToMyProfile
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── 2x2 QUICK ACTIONS GRID ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Smart Travel\nCredit Card",
                    icon = Icons.Outlined.CreditCard,
                    iconBg = Amber.copy(alpha = 0.15f),
                    iconTint = Amber,
                    tag = "Free",
                    onClick = {
                        Toast.makeText(context, "Smart Travel Card: Zero Forex & Lounge Access", Toast.LENGTH_SHORT).show()
                    }
                )

                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Manage\nPayments",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconBg = Color(0xFF4FA8F6).copy(alpha = 0.15f),
                    iconTint = Color(0xFF4FA8F6),
                    onClick = onNavigateToManagePayments
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Customer\nService",
                    icon = Icons.Outlined.SupportAgent,
                    iconBg = Color(0xFFF472B6).copy(alpha = 0.15f),
                    iconTint = Color(0xFFF472B6),
                    onClick = { showSupportDialog = true }
                )

                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Device\nTheme",
                    icon = Icons.Outlined.Palette,
                    iconBg = Color(0xFF34D399).copy(alpha = 0.15f),
                    iconTint = Color(0xFF34D399),
                    onClick = { showThemeDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ── OTHER OPTIONS CARD ───────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(0.7.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "OTHER OPTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 10.dp)
                    )

                    ProfileOptionItem(
                        icon = Icons.Outlined.StarBorder,
                        title = "Rate Us",
                        onClick = { showRateDialog = true }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    ProfileOptionItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Notification",
                        onClick = { showNotificationsDialog = true }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    ProfileOptionItem(
                        icon = Icons.Outlined.Share,
                        title = "Share App",
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Plan your dream journeys with Smart Travel Planner! ✈️🌍 Discover curated spots, AI itineraries, and more."
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Smart Travel Planner"))
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    ProfileOptionItem(
                        icon = Icons.Outlined.Settings,
                        title = "Settings",
                        onClick = { showSettingsDialog = true }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    ProfileOptionItem(
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        title = "Log Out",
                        onClick = { showLogoutDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── APPLICATION VERSION & PASSPORT STAMPS ────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Application Version 1.0.0",
                    fontSize = 11.sp,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Vintage passport stamp watermark background
                PassportStampsWatermark(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── DIALOGS ──────────────────────────────────────────────────────────
        if (showSupportDialog) {
            CustomerSupportDialog(onDismiss = { showSupportDialog = false })
        }

        if (showThemeDialog) {
            ThemeSelectionBottomSheet(
                currentTheme = prefs.themeMode,
                onDismiss = { showThemeDialog = false },
                onSave = { selectedTheme ->
                    viewModel.updateTheme(selectedTheme)
                    showThemeDialog = false
                    Toast.makeText(context, "Appearance set to $selectedTheme", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showRateDialog) {
            RateAppDialog(onDismiss = { showRateDialog = false })
        }

        if (showSettingsDialog) {
            AiSettingsDialog(
                prefs = prefs,
                onDismiss = { showSettingsDialog = false },
                onSave = { updatedPrefs ->
                    viewModel.updatePreferences(updatedPrefs)
                    Toast.makeText(context, "AI Settings saved", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showNotificationsDialog) {
            NotificationDialog(onDismiss = { showNotificationsDialog = false })
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(
                        text = "Log Out",
                        fontFamily = PlayfairFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to log out of your account?",
                        fontFamily = DmSansFontFamily
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            onLogoutClick()
                        }
                    ) {
                        Text("Log Out", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    }
}

// ── PASSPORT PROFILE CARD ───────────────────────────────────────────────────

@Composable
private fun PassportProfileCard(
    prefs: UserPreferences,
    onCloseClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = Color(0x66000000)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2ECE1)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD2C0))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFAF6EE),
                            Color(0xFFF2ECE1),
                            Color(0xFFE9DEC9)
                        )
                    )
                )
        ) {
            // Subtle watermark map lines inside passport card
            PassportCardWatermark(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.12f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                // Header Row: [Close Button] - [smart travel profile] - [Hologram Flight Badge]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Close 'X' Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x2B000000))
                            .clickable(onClick = onCloseClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF1E2833),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center Monospace Title
                    Text(
                        text = "smart travel profile",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF283647),
                        letterSpacing = 1.6.sp
                    )

                    // Holographic Visa Flight Badge
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFF9AA2),
                                        Color(0xFFE2B0FF),
                                        Color(0xFF93C5FD),
                                        Color(0xFF6EE7B7)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(100f, 100f)
                                )
                            )
                            .border(1.dp, Color(0x99FFFFFF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flight,
                            contentDescription = "Travel Pass",
                            tint = Color.White,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(-30f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Profile Details Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Column: Avatar + Completion Badge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFDED3BF))
                                .border(1.dp, Color(0xFFC7BBA5), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (prefs.profilePictureUrl.isNotBlank()) {
                                AsyncImage(
                                    model = prefs.profilePictureUrl,
                                    contentDescription = "Profile Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile Photo",
                                    tint = Color(0xFFA8997F),
                                    modifier = Modifier
                                        .size(72.dp)
                                        .offset(y = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Right Column: Monospaced Passport Fields
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PassportField(
                            label = "First name",
                            value = prefs.firstName.ifEmpty { "What the F**k? BC ye kyu nhi chal rha" }
                        )

                        PassportField(
                            label = "Last name",
                            value = prefs.lastName.ifEmpty { "—" }
                        )

                        PassportField(
                            label = "Email",
                            value = prefs.email.ifEmpty { "—" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Right "Edit Profile >" Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF222C3B))
                            .clickable(onClick = onEditClick)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Edit Profile",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = DmSansFontFamily,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Edit",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassportField(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF758292),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF182330)
        )
    }
}

// ── QUICK ACTION CARD (2x2) ─────────────────────────────────────────────────

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    tag: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // "Free" Tag Badge if present
            if (tag != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF7C3AED))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontFamily = DmSansFontFamily
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (tag != null) 10.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ── PROFILE OPTION ITEM ──────────────────────────────────────────────────────

@Composable
private fun ProfileOptionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DmSansFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ── PASSPORT STAMPS WATERMARK GRAPHICS ───────────────────────────────────────

@Composable
private fun PassportStampsWatermark(modifier: Modifier = Modifier) {
    val stampColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val brownStampColor = Amber.copy(alpha = 0.35f)

    Canvas(modifier = modifier) {
        // Circular Visa Stamp 1 (Left)
        drawCircle(
            color = stampColor,
            radius = 48.dp.toPx(),
            center = Offset(size.width * 0.18f, size.height * 0.6f),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
        )
        drawCircle(
            color = stampColor,
            radius = 38.dp.toPx(),
            center = Offset(size.width * 0.18f, size.height * 0.6f),
            style = Stroke(width = 1.dp.toPx())
        )

        // Center Flight Stamp
        drawCircle(
            color = brownStampColor,
            radius = 52.dp.toPx(),
            center = Offset(size.width * 0.55f, size.height * 0.75f),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = brownStampColor,
            radius = 44.dp.toPx(),
            center = Offset(size.width * 0.55f, size.height * 0.75f),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )
        )

        // Right Triangular Stamp
        val triCenter = Offset(size.width * 0.88f, size.height * 0.65f)
        val r = 40.dp.toPx()
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(triCenter.x, triCenter.y - r)
            lineTo(triCenter.x + r * 0.9f, triCenter.y + r * 0.7f)
            lineTo(triCenter.x - r * 0.9f, triCenter.y + r * 0.7f)
            close()
        }
        drawPath(
            path = path,
            color = stampColor,
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
            )
        )
    }
}

@Composable
private fun PassportCardWatermark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val lineColor = Color(0xFF6E5F48)
        // Draw subtle contour/meridian waves
        for (i in 1..5) {
            drawLine(
                color = lineColor,
                start = Offset(0f, size.height * (i * 0.18f)),
                end = Offset(size.width, size.height * (i * 0.18f - 0.05f)),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), (i * 5).toFloat())
            )
        }
    }
}

// ── DIALOGS & BOTTOM SHEETS ─────────────────────────────────────────────────

@Composable
private fun CustomerSupportDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SupportAgent,
                    contentDescription = null,
                    tint = Amber
                )
                Text(
                    text = "Customer Service",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "24/7 Smart Travel Assistance is always here for your flights, trips & queries.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Toll Free Helpline", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text("+91 1800 200 4567", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Email Support", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text("support@smarttravelplanner.com", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Amber)
            ) {
                Text("Got it", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ThemeSelectionBottomSheet(
    currentTheme: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var selectedOption by remember { mutableStateOf(currentTheme.ifEmpty { "Device Theme" }) }
    val options = listOf("Light Theme", "Dark Theme", "Device Theme")
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isVisible = true
    }

    fun animateDismiss(action: () -> Unit = onDismiss) {
        if (!isVisible) return
        coroutineScope.launch {
            isVisible = false
            delay(280)
            action()
        }
    }

    BackHandler(enabled = isVisible) {
        animateDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 280))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB8000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { animateDismiss() }
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(durationMillis = 280, easing = FastOutLinearInEasing)
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Floating Close Button above sheet
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .clickable(onClick = { animateDismiss() }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Sheet Container
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp)
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header Title
                        Text(
                            text = "Select Appearance",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlayfairFontFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Options Container Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                options.forEachIndexed { index, option ->
                                    val isSelected = selectedOption == option
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedOption = option }
                                            .padding(horizontal = 18.dp, vertical = 18.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = option,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = DmSansFontFamily,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        // Custom Radio Indicator
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.5.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.onSurface)
                                                )
                                            }
                                        }
                                    }

                                    if (index < options.size - 1) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            thickness = 0.8.dp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // "Save" Button
                        Button(
                            onClick = {
                                animateDismiss {
                                    onSave(selectedOption)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFC85A0D)
                            )
                        ) {
                            Text(
                                text = "Save",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DmSansFontFamily,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RateAppDialog(onDismiss: () -> Unit) {
    var rating by remember { mutableIntStateOf(5) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Enjoying Smart Travel Planner?",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tap stars to rate your trip planning experience:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..5) {
                        IconButton(onClick = { rating = i }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$i star",
                                tint = if (i <= rating) Amber else Color(0xFF334155),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Toast.makeText(context, "Thank you for the $rating-star rating!️", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Amber)
            ) {
                Text("Submit", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
private fun NotificationDialog(onDismiss: () -> Unit) {
    var flightAlerts by remember { mutableStateOf(true) }
    var priceDrops by remember { mutableStateOf(true) }
    var tripReminders by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Notification Preferences",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NotificationToggleRow("Flight & Status Alerts", flightAlerts) { flightAlerts = it }
                NotificationToggleRow("Exclusive Price Drops", priceDrops) { priceDrops = it }
                NotificationToggleRow("Upcoming Trip Reminders", tripReminders) { tripReminders = it }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Amber)
            ) {
                Text("Save", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun NotificationToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Amber,
                uncheckedTrackColor = Color(0xFF334155)
            )
        )
    }
}

package com.example.smarttravelplanner.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.ui.components.BottomNavigationBar
import com.example.smarttravelplanner.ui.screens.auth.AuthViewModel
import com.example.smarttravelplanner.ui.screens.auth.LoginScreen
import com.example.smarttravelplanner.ui.screens.explore.ExploreScreen
import com.example.smarttravelplanner.ui.screens.home.HomeScreen
import com.example.smarttravelplanner.ui.screens.mytrips.MyTripsScreen
import com.example.smarttravelplanner.ui.screens.payment.ManagePaymentsScreen
import com.example.smarttravelplanner.ui.screens.plan.PlanScreen
import com.example.smarttravelplanner.ui.screens.profile.MyProfileScreen
import com.example.smarttravelplanner.ui.screens.profile.ProfileScreen
import com.example.smarttravelplanner.ui.screens.profile.ProfileViewModel
import com.example.smarttravelplanner.ui.screens.tripdetail.TripDetailScreen
import com.example.smarttravelplanner.ui.theme.Amber


/** SavedStateHandle key used to pre-fill the Plan screen's destination from Explore. */
private const val PLAN_PREFILL_DESTINATION_KEY = "plan_prefill_destination"

@Composable
fun AppNavigation(
    profileViewModel: ProfileViewModel,
    authViewModel: AuthViewModel = hiltViewModel()
) {

    val navController = rememberNavController()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle(initialValue = null)

    if (isLoggedIn == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Amber,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
        }
        return
    }

    // Remember the initial startDestination so the NavGraph is not destroyed and recreated
    // when isLoggedIn updates in DataStore upon successful login.
    val startDestination = remember { if (isLoggedIn == true) "home" else "login" }

    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route ?: startDestination

    // Routes where the bottom nav should be hidden
    val hiddenNavRoutes = setOf(
        "login", "profile", "my_profile", "manage_payments", "trip_detail/{tripId}"
    )
    val isImeVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    val isNavRoute = hiddenNavRoutes.none { currentRoute.startsWith(it.substringBefore("{")) }

    Scaffold(
        bottomBar = {
            if (!isImeVisible) {
                AnimatedVisibility(
                    visible = isNavRoute,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(durationMillis = 200))
                ) {
                    BottomNavigationBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding)
        ) {

            // ── Auth ──────────────────────────────────────────────────────────
            composable(
                route = "login",
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        targetOffset = { fullWidth -> (fullWidth * 0.35f).toInt() }
                    ) + scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        targetOffset = { fullWidth -> (fullWidth * 0.35f).toInt() }
                    ) + scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    ) + scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                }
            ) {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            // ── Main tabs ─────────────────────────────────────────────────────
            composable(
                route = "home",
                enterTransition = {
                    if (initialState.destination.route?.startsWith("login") == true) {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        ) + scaleIn(
                            initialScale = 0.94f,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        )
                    } else {
                        fadeIn(animationSpec = tween(durationMillis = 300))
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route?.startsWith("login") == true) {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        ) + scaleIn(
                            initialScale = 0.94f,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        )
                    } else {
                        fadeIn(animationSpec = tween(durationMillis = 300))
                    }
                },
                exitTransition = {
                    if (targetState.destination.route?.startsWith("login") == true) {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                            targetOffset = { fullWidth -> (fullWidth * 0.35f).toInt() }
                        ) + scaleOut(
                            targetScale = 0.92f,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        )
                    } else {
                        fadeOut(animationSpec = tween(durationMillis = 300))
                    }
                },
                popExitTransition = {
                    if (targetState.destination.route?.startsWith("login") == true) {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                            targetOffset = { fullWidth -> (fullWidth * 0.35f).toInt() }
                        ) + scaleOut(
                            targetScale = 0.92f,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        )
                    } else {
                        fadeOut(animationSpec = tween(durationMillis = 300))
                    }
                }
            ) {
                HomeScreen(
                    onPlanWithAiClick = {
                        navController.navigate("plan") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onExploreClick = {
                        navController.navigate("explore") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onMyTripsClick = {
                        navController.navigate("mytrips") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onTripClick = { tripId ->
                        navController.navigate("trip_detail/$tripId")
                    },
                    onMenuClick = { navController.navigate("profile") }
                )
            }

            composable("plan") { planEntry ->
                // Destination handed over from Explore → "Plan a trip here"
                val prefillDestination by planEntry.savedStateHandle
                    .getStateFlow<String?>(PLAN_PREFILL_DESTINATION_KEY, null)
                    .collectAsStateWithLifecycle()

                PlanScreen(
                    prefillDestination = prefillDestination,
                    onPrefillConsumed = {
                        planEntry.savedStateHandle[PLAN_PREFILL_DESTINATION_KEY] = null
                    },
                    onMenuClick = { navController.navigate("profile") },
                    onTripGenerated = { tripId ->
                        navController.navigate("trip_detail/$tripId") {
                            popUpTo("plan") { inclusive = false }
                        }
                    }
                )
            }

            composable("mytrips") {
                MyTripsScreen(
                    onTripClick = { tripId ->
                        navController.navigate("trip_detail/$tripId")
                    },
                    onMenuClick = { navController.navigate("profile") }
                )
            }

            composable("explore") {
                ExploreScreen(
                    onMenuClick = { navController.navigate("profile") },
                    onPlanTrip = { destinationName ->
                        // Same options as the bottom-nav "Plan" tab so the tab stays highlighted
                        navController.navigate("plan") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set(PLAN_PREFILL_DESTINATION_KEY, destinationName)
                    }
                )
            }

            // ── Trip Detail ───────────────────────────────────────────────────
            composable(
                route     = "trip_detail/{tripId}",
                arguments = listOf(navArgument("tripId") { type = NavType.LongType })
            ) {
                TripDetailScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ── Profile stack ─────────────────────────────────────────────────
            composable("profile") {
                ProfileScreen(
                    viewModel              = profileViewModel,
                    onCloseClick           = { navController.popBackStack() },
                    onNavigateToMyProfile  = { navController.navigate("my_profile") },
                    onNavigateToManagePayments = { navController.navigate("manage_payments") },
                    onLogoutClick          = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable("my_profile") {
                MyProfileScreen(
                    viewModel = profileViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable("manage_payments") {
                ManagePaymentsScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }
}

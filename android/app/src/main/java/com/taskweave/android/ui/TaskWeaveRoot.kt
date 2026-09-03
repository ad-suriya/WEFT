package com.taskweave.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.taskweave.android.ExternalIntent
import com.taskweave.android.ui.chat.ChatScreen
import com.taskweave.android.ui.execution.ExecutionScreen
import com.taskweave.android.ui.focuslock.BlocklistScreen
import com.taskweave.android.ui.navigation.Routes
import com.taskweave.android.ui.navigation.TopDestination
import com.taskweave.android.ui.signin.SignInScreen
import com.taskweave.android.ui.tasks.TaskDetailScreen
import com.taskweave.android.ui.tasks.TaskListScreen
import com.taskweave.android.ui.today.TodayScreen
import kotlinx.coroutines.flow.Flow
import android.content.Context

@Composable
fun TaskWeaveRoot(
    state: RootUiState,
    onSignIn: (Context) -> Unit,
    onSignOut: () -> Unit,
    intentEvents: Flow<ExternalIntent>,
) {
    when (state) {
        RootUiState.Loading -> LoadingScreen()
        RootUiState.SignedOut -> SignInScreen(onSignIn = onSignIn)
        is RootUiState.SignedIn -> SignedInScaffold(
            displayName = state.session.displayName ?: state.session.email ?: "",
            onSignOut = onSignOut,
            intentEvents = intentEvents,
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignedInScaffold(
    displayName: String,
    onSignOut: () -> Unit,
    intentEvents: Flow<ExternalIntent>,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val rootVm: RootViewModel = hiltViewModel()

    val error by rootVm.errorMessage.collectAsStateWithLifecycle()
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            rootVm.consumeError()
        }
    }

    // Ask for notification permission once (Android 13+); push + status nudges need it.
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Calendar-scope consent (requested right after first sign-in).
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { rootVm.onCalendarConsentHandled() }
    val calendarConsent by rootVm.calendarConsent.collectAsStateWithLifecycle()
    LaunchedEffect(calendarConsent) {
        calendarConsent?.let { sender ->
            consentLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    // React to share-sheet / notification deep links.
    LaunchedEffect(Unit) {
        intentEvents.collect { event ->
            when (event) {
                is ExternalIntent.OpenTask -> navController.navigate(Routes.task(event.taskId))
                is ExternalIntent.ShareCapture -> {
                    val prefill = android.net.Uri.encode(event.body.take(280))
                    navController.navigate("tasks?prefill=$prefill")
                }
            }
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopDestination.entries.any { it.route == currentRoute } ||
        currentRoute?.startsWith("tasks") == true

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(visible = showBottomBar) {
                NavigationBar {
                    val destinations = backStackEntry?.destination
                    TopDestination.entries.forEach { dest ->
                        val selected = destinations?.hierarchy?.any {
                            it.route == dest.route || it.route?.substringBefore('?') == dest.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(stringResource(dest.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopDestination.START,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopDestination.TODAY.route) {
                TodayScreen(
                    onOpenTask = { navController.navigate(Routes.task(it)) },
                    onOpenChat = { navController.navigate(TopDestination.CHAT.route) },
                )
            }
            composable(
                route = "tasks?prefill={prefill}",
                arguments = listOf(
                    androidx.navigation.navArgument("prefill") {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                TaskListScreen(
                    prefillTitle = entry.arguments?.getString("prefill"),
                    displayName = displayName,
                    onOpenTask = { navController.navigate(Routes.task(it)) },
                    onSignOut = onSignOut,
                )
            }
            composable(TopDestination.FOCUS.route) {
                ExecutionScreen(
                    onOpenTask = { navController.navigate(Routes.task(it)) },
                    onChooseApps = { navController.navigate(Routes.BLOCKLIST) },
                )
            }
            composable(Routes.BLOCKLIST) {
                BlocklistScreen(onBack = { navController.popBackStack() })
            }
            composable(TopDestination.CHAT.route) { ChatScreen() }
            composable(Routes.TASK_PATTERN) { entry ->
                TaskDetailScreen(
                    taskId = entry.arguments?.getString("taskId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

package com.example.futurefocus.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.futurefocus.service.FocusLockService
import com.example.futurefocus.ui.component.BottomNavBar
import com.example.futurefocus.ui.screen.CreateGoalScreen
import com.example.futurefocus.ui.screen.DurationScreen
import com.example.futurefocus.ui.screen.FocusLockScreen
import com.example.futurefocus.ui.screen.GoalDetailScreen
import com.example.futurefocus.ui.screen.GoalsScreen
import com.example.futurefocus.ui.screen.HistoryScreen
import com.example.futurefocus.ui.screen.HomeScreen
import com.example.futurefocus.ui.screen.OnboardingScreen
import com.example.futurefocus.ui.screen.PermissionScreen
import com.example.futurefocus.ui.screen.ProfileScreen
import com.example.futurefocus.ui.screen.SessionCompleteScreen
import com.example.futurefocus.ui.screen.StatisticsScreen
import com.example.futurefocus.utils.PermissionHelper
import com.example.futurefocus.viewmodel.FocusViewModel

private val mainRoutes = setOf(
    Screen.Home.route,
    Screen.Statistics.route,
    Screen.Goals.route,
    Screen.CreateGoal.route,
    Screen.GoalDetail.route,
    Screen.History.route,
    Screen.Duration.route,
    Screen.Profile.route,
)

@Composable
fun FutureFocusApp(
    navController: NavHostController = rememberNavController(),
    focusViewModel: FocusViewModel = viewModel()
) {
    val sessions by focusViewModel.sessions.collectAsState()
    val dailyGoal by focusViewModel.dailyGoal.collectAsState()
    val showSessionFailed by focusViewModel.showSessionFailed.collectAsState()
    val goals by focusViewModel.goals.collectAsState()
    val subtasks by focusViewModel.subtasks.collectAsState()
    val completedGoalId by focusViewModel.completedGoalId.collectAsState()

    val context = LocalContext.current
    val isOnboardingCompleted = remember {
        mutableStateOf(
            context.getSharedPreferences("futurefocus", Context.MODE_PRIVATE)
                .getBoolean("onboarding_completed", false)
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in mainRoutes

    val selectedNavIndex = when (currentRoute) {
        Screen.Home.route -> 0
        Screen.Statistics.route, Screen.History.route -> 1
        Screen.Goals.route, Screen.CreateGoal.route, Screen.GoalDetail.route -> 2
        Screen.Profile.route -> 3
        else -> 0
    }

    LaunchedEffect(Unit) {
        focusViewModel.preloadQuotes()
    }

    val pendingSessionParams = remember {
        mutableStateOf<Triple<Int, String?, String?>?>(null)
    }

    LaunchedEffect(Unit) {
        if (!isOnboardingCompleted.value) {
            navController.navigate(Screen.Onboarding.route)
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    selectedIndex = selectedNavIndex,
                    onFocusClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onStatsClick = {
                        navController.navigate(Screen.Statistics.route) {
                            popUpTo(Screen.Home.route)
                            launchSingleTop = true
                        }
                    },
                    onGoalsClick = {
                        navController.navigate(Screen.Goals.route) {
                            popUpTo(Screen.Home.route)
                            launchSingleTop = true
                        }
                    },
                    onProfileClick = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Home.route)
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onGetStarted = {
                        context.getSharedPreferences("futurefocus", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("onboarding_completed", true)
                            .apply()
                        isOnboardingCompleted.value = true
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onEnableAccessibility = {
                        PermissionHelper.openAccessibilitySettings(context)
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    stats = focusViewModel.stats(),
                    dailyGoal = dailyGoal,
                    goals = goals,
                    showSessionFailed = showSessionFailed,
                    completedGoalId = completedGoalId,
                    onDismissSessionFailed = focusViewModel::dismissSessionFailed,
                    onDismissCompletedGoal = focusViewModel::dismissCompletedGoal,
                    onUpdateDailyGoal = focusViewModel::updateDailyGoal,
                    onStartFocus = { navController.navigate(Screen.Duration.route) },
                    onOpenHistory = { navController.navigate(Screen.History.route) },
                    onOpenStatistics = { navController.navigate(Screen.Statistics.route) },
                    onOpenCreateGoal = { navController.navigate(Screen.CreateGoal.route) },
                    onGoalClick = { goalId ->
                        navController.navigate(Screen.GoalDetail.createRoute(goalId))
                    }
                )
            }
            composable(Screen.Duration.route) {
                DurationScreen(
                    goals = goals,
                    onBack = { navController.popBackStack() },
                    onCreateGoal = {
                        navController.navigate(Screen.CreateGoal.route)
                    },
                    onStartSession = { minutes, goalId, goalTitle ->
                        if (PermissionHelper.isAccessibilityServiceEnabled(context, FocusLockService::class.java)) {
                            val session = focusViewModel.startSession(minutes, goalId, goalTitle)
                            navController.navigate(Screen.FocusLock.createRoute(session.id)) {
                                popUpTo(Screen.Duration.route) { inclusive = true }
                            }
                        } else {
                            pendingSessionParams.value = Triple(minutes, goalId, goalTitle)
                            navController.navigate(Screen.Permission.route)
                        }
                    }
                )
            }
            composable(Screen.Permission.route) {
                PermissionScreen(
                    onProceed = {
                        val params = pendingSessionParams.value
                        if (params != null) {
                            val (minutes, goalId, goalTitle) = params
                            val session = focusViewModel.startSession(minutes, goalId, goalTitle)
                            pendingSessionParams.value = null
                            navController.navigate(Screen.FocusLock.createRoute(session.id)) {
                                popUpTo(Screen.Duration.route) { inclusive = true }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.CreateGoal.route) {
                CreateGoalScreen(
                    onBack = { navController.popBackStack() },
                    onSave = { goal, subtaskList ->
                        focusViewModel.createGoal(goal, subtaskList)
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.GoalDetail.route) { backStackEntry ->
                val goalId = backStackEntry.arguments?.getString("goalId").orEmpty()
                val goal = focusViewModel.getGoal(goalId)
                val goalSubtasks = subtasks[goalId].orEmpty()
                GoalDetailScreen(
                    goal = goal,
                    subtasks = goalSubtasks,
                    onToggleSubtask = { subtaskId, completed ->
                        focusViewModel.toggleSubtask(goalId, subtaskId, completed)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.FocusLock.route) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId").orEmpty()
                LaunchedEffect(sessionId) {
                    focusViewModel.preloadQuotes()
                }

                FocusLockScreen(
                    session = focusViewModel.getSession(sessionId),
                    onExitAttempt = { focusViewModel.registerExitAttempt(sessionId) },
                    onGiveUp = {
                        focusViewModel.failSession(sessionId)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    },
                    onCompleted = {
                        focusViewModel.completeSession(sessionId)
                        navController.navigate(Screen.SessionComplete.createRoute(sessionId)) {
                            popUpTo(Screen.FocusLock.route) { inclusive = true }
                        }
                    },
                    onBackgroundExit = {
                        focusViewModel.failSession(sessionId)
                        focusViewModel.markSessionFailed()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Goals.route) {
                GoalsScreen(
                    goals = goals,
                    onGoalClick = { goalId ->
                        navController.navigate(Screen.GoalDetail.createRoute(goalId))
                    },
                    onCreateGoal = {
                        navController.navigate(Screen.CreateGoal.route)
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    sessions = sessions,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Statistics.route) {
                StatisticsScreen(
                    stats = focusViewModel.stats(),
                    dailyGoal = dailyGoal,
                    focusViewModel = focusViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SessionComplete.route) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId").orEmpty()
                val session = focusViewModel.getSession(sessionId)
                val goal = session?.goalId?.let { focusViewModel.getGoal(it) }
                SessionCompleteScreen(
                    session = session,
                    goal = goal,
                    onHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen()
            }
        }
    }
}

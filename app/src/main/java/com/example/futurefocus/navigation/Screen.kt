package com.example.futurefocus.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Duration : Screen("duration")
    data object FocusLock : Screen("focus/{sessionId}") {
        fun createRoute(sessionId: String) = "focus/$sessionId"
    }
    data object You : Screen("you?tab={tab}") {
        fun createRoute(tab: Int = 0) = "you?tab=$tab"
    }
    data object Goals : Screen("goals")
    data object CreateGoal : Screen("create_goal")
    data object GoalDetail : Screen("goal_detail/{goalId}") {
        fun createRoute(goalId: String) = "goal_detail/$goalId"
    }
    data object EditGoal : Screen("edit_goal/{goalId}") {
        fun createRoute(goalId: String) = "edit_goal/$goalId"
    }
    data object Permission : Screen("permission")
    data object Onboarding : Screen("onboarding")
    data object Profile : Screen("profile")
}

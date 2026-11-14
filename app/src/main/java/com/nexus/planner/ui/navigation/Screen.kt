package com.nexus.planner.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Timeline : Screen("timeline", "Today")
    object Architect : Screen("architect", "Plan")
    object Coach : Screen("coach", "Agent")
    object Stats : Screen("stats", "Stats")
}

package pbs.edu.fotokrzyk.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pbs.edu.fotokrzyk.ui.history.HistoryScreen
import pbs.edu.fotokrzyk.ui.home.HomeScreen

@Composable
fun AppNav() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = Route.Home.path
    ) {
        composable(Route.Home.path) {
            HomeScreen(
                onGoHistory = { nav.navigate(Route.History.path) }
            )
        }
        composable(Route.History.path) {
            HistoryScreen(
                onBack = { nav.popBackStack() }
            )
        }
    }
}

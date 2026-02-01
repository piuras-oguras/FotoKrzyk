package pbs.edu.fotokrzyk.navigation

sealed class Route(val path: String) {
    data object Home : Route("home")
    data object History : Route("history")
}

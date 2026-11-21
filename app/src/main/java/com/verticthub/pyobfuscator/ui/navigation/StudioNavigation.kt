package com.verticthub.pyobfuscator.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.verticthub.pyobfuscator.design.components.NavDestinationItem

enum class StudioRoute(val route: String, val label: String, val icon: ImageVector) {
    WORKSPACE("workspace", "Workspace", Icons.Default.Code),
    OBFUSCATE("obfuscate", "Obfuscate", Icons.Default.Tune),
    BUILD("build", "Build", Icons.Default.Build),
    HISTORY("history", "History", Icons.Default.History),
    PROGRESS("progress", "Progress", Icons.Default.Tune),
    RESULT("result", "Result", Icons.Default.Code),
    SETTINGS("settings", "Settings", Icons.Default.Tune),
}

val PrimaryNavDestinations: List<NavDestinationItem> = listOf(
    NavDestinationItem(StudioRoute.WORKSPACE.route, StudioRoute.WORKSPACE.label, StudioRoute.WORKSPACE.icon),
    NavDestinationItem(StudioRoute.OBFUSCATE.route, StudioRoute.OBFUSCATE.label, StudioRoute.OBFUSCATE.icon),
    NavDestinationItem(StudioRoute.BUILD.route, StudioRoute.BUILD.label, StudioRoute.BUILD.icon),
    NavDestinationItem(StudioRoute.HISTORY.route, StudioRoute.HISTORY.label, StudioRoute.HISTORY.icon),
)

package com.verticthub.pyobfuscator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.verticthub.pyobfuscator.design.components.VHBottomNavigation
import com.verticthub.pyobfuscator.design.components.VHErrorPanel
import com.verticthub.pyobfuscator.design.components.VHTopBar
import com.verticthub.pyobfuscator.design.theme.LocalStudioPalette
import com.verticthub.pyobfuscator.design.theme.VertictStudioTheme
import com.verticthub.pyobfuscator.ui.StudioViewModel
import com.verticthub.pyobfuscator.ui.navigation.PrimaryNavDestinations
import com.verticthub.pyobfuscator.ui.navigation.StudioRoute
import com.verticthub.pyobfuscator.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: StudioViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            VertictStudioTheme(themeMode = state.settings.themeMode) {
                val palette = LocalStudioPalette.current
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(state.bannerMessage) {
                    state.bannerMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearBanners()
                    }
                }

                val isTopLevel = PrimaryNavDestinations.any { it.route == currentRoute }
                val screenTitle = when (currentRoute) {
                    StudioRoute.WORKSPACE.route -> "VertictHub Studio"
                    StudioRoute.OBFUSCATE.route -> "Konfigurasi Proteksi"
                    StudioRoute.BUILD.route -> "Build & Packaging"
                    StudioRoute.HISTORY.route -> "Riwayat Artefak"
                    StudioRoute.PROGRESS.route -> "Proses Eksekusi"
                    StudioRoute.RESULT.route -> "Hasil & Inspeksi"
                    StudioRoute.SETTINGS.route -> "Pengaturan Studio"
                    else -> "VertictHub Studio"
                }

                Scaffold(
                    containerColor = palette.background,
                    topBar = {
                        VHTopBar(
                            title = screenTitle,
                            subtitle = state.workspace?.filename ?: "Python 3.13 ARM64",
                            engineReady = state.engineReady,
                            showBack = !isTopLevel,
                            onBackClick = { navController.popBackStack() },
                            onSettingsClick = if (currentRoute != StudioRoute.SETTINGS.route) {
                                { navController.navigate(StudioRoute.SETTINGS.route) }
                            } else null,
                        )
                    },
                    bottomBar = {
                        if (isTopLevel) {
                            VHBottomNavigation(items = PrimaryNavDestinations, currentRoute = currentRoute, onSelect = { route ->
                                navController.navigate(route) {
                                    popUpTo(StudioRoute.WORKSPACE.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            })
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { innerPadding ->
                    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        state.errorBanner?.let { err ->
                            VHErrorPanel(title = "Pemberitahuan Engine", message = err, onDismiss = { viewModel.clearBanners() })
                        }
                        NavHost(navController = navController, startDestination = StudioRoute.WORKSPACE.route, modifier = Modifier.weight(1f)) {
                            composable(StudioRoute.WORKSPACE.route) {
                                WorkspaceScreen(
                                    state = state, viewModel = viewModel,
                                    onNavigateObfuscate = { navController.navigate(StudioRoute.OBFUSCATE.route) },
                                    onNavigateBuild = { navController.navigate(StudioRoute.BUILD.route) },
                                    onNavigateResult = { navController.navigate(StudioRoute.RESULT.route) },
                                )
                            }
                            composable(StudioRoute.OBFUSCATE.route) {
                                ObfuscateScreen(
                                    state = state, viewModel = viewModel,
                                    onStartProgress = { navController.navigate(StudioRoute.PROGRESS.route) },
                                    onCompletedResult = {
                                        navController.navigate(StudioRoute.RESULT.route) {
                                            popUpTo(StudioRoute.PROGRESS.route) { inclusive = true }
                                        }
                                    },
                                )
                            }
                            composable(StudioRoute.BUILD.route) {
                                BuildScreen(state = state, viewModel = viewModel, onNavigateResult = { navController.navigate(StudioRoute.RESULT.route) })
                            }
                            composable(StudioRoute.PROGRESS.route) {
                                ProgressScreen(state = state, viewModel = viewModel, onBack = { navController.popBackStack() })
                            }
                            composable(StudioRoute.RESULT.route) {
                                ResultScreen(state = state, viewModel = viewModel)
                            }
                            composable(StudioRoute.HISTORY.route) {
                                HistoryScreen(state = state, viewModel = viewModel, onOpenResult = { navController.navigate(StudioRoute.RESULT.route) })
                            }
                            composable(StudioRoute.SETTINGS.route) {
                                SettingsScreen(state = state, viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.resumeapp.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.resumeapp.features.ats.presentation.ATSScreen
import com.resumeapp.features.onboarding.presentation.OnboardingNavHost
import com.resumeapp.features.resume.presentation.ResumeNavHost
import com.resumeapp.features.settings.presentation.SettingsScreen
import com.resumeapp.features.templates.presentation.TemplateGalleryScreen

private data class MainDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val MAIN_DESTINATIONS = listOf(
    MainDestination("app/resume_list", "Currículos", Icons.Default.Description),
    MainDestination("app/templates", "Templates", Icons.Default.Style),
    MainDestination("app/settings", "Configurações", Icons.Default.Settings)
)

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = "onboarding"
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // A bottom bar aparece apenas no app principal (rotas "app/...").
    val showBottomBar = currentRoute?.startsWith("app") == true

    Scaffold(
        // Os insets sao tratados por cada tela; o Scaffold nao deve duplicar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    MAIN_DESTINATIONS.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        popUpTo("app/resume_list") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val contentPadding = if (showBottomBar) {
            PaddingValues(bottom = innerPadding.calculateBottomPadding())
        } else {
            PaddingValues(0.dp)
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            // Onboarding flow
            composable("onboarding") {
                OnboardingNavHost(
                    onFinished = {
                        navController.navigate("app") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            // Main app flow
            navigation(startDestination = "resume_list", route = "app") {
                composable("resume_list") {
                    ResumeNavHost(
                        onOpenAts = { resumeId ->
                            navController.navigate("app/ats/$resumeId")
                        }
                    )
                }
                composable(
                    route = "ats/{resumeId}",
                    arguments = listOf(navArgument("resumeId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val resumeId = backStackEntry.arguments?.getString("resumeId") ?: ""
                    ATSScreen(resumeId = resumeId, onBack = { navController.popBackStack() })
                }
                composable("templates") {
                    TemplateGalleryScreen(onSelect = { template ->
                        navController.navigate("app/resume_list")
                    })
                }
                composable("settings") {
                    SettingsScreen()
                }
            }
        }
    }
}

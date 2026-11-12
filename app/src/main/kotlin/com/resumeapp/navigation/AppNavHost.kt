package com.resumeapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.resumeapp.features.ats.presentation.ATSScreen
import com.resumeapp.features.onboarding.presentation.OnboardingNavHost
import com.resumeapp.features.resume.presentation.ResumeNavHost
import com.resumeapp.features.settings.presentation.SettingsScreen
import com.resumeapp.features.templates.presentation.TemplateGalleryScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = "onboarding"
) {
    NavHost(navController, startDestination) {
        // Onboarding flow
        composable("onboarding") {
            OnboardingNavHost(navController, "welcome")
        }

        // Main app flow
        navigation(startDestination = "resume_list", route = "app") {
            composable("resume_list") {
                ResumeNavHost(navController, "list")
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
                    navController.navigate("app/resume_list/editor/new?template=${template.id}")
                })
            }
            composable("settings") {
                SettingsScreen()
            }
        }
    }
}
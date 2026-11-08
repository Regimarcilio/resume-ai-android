package com.resumeapp.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.resumeapp.features.onboarding.presentation.OnboardingNavHost
import com.resumeapp.features.resume.presentation.ResumeNavHost
import com.resumeapp.features.ats.presentation.ATSScreen
import com.resumeapp.features.templates.presentation.TemplateGalleryScreen
import com.resumeapp.features.settings.presentation.SettingsScreen
import com.resumeapp.features.onboarding.presentation.OnboardingNavHost
import com.resumeapp.features.resume.presentation.ResumeNavHost

@Composable
fun AppNavHost(
    navController: NavController,
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
                val resumeId = backStackEntry.getString()!!
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
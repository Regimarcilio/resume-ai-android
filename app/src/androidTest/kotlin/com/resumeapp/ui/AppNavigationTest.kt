package com.resumeapp.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertExists
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.resumeapp.MainActivity
import com.resumeapp.navigation.AppNavHost
import com.resumeapp.ui.theme.ResumeTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(com.resumeapp.di.AppModule::class, com.resumeapp.di.NetworkModule::class, com.resumeapp.di.DatabaseModule::class, com.resumeapp.di.RepositoryModule::class, com.resumeapp.di.PreferencesModule::class, com.resumeapp.pdf.di.PdfModule::class, com.resumeapp.billing.di.BillingModule::class)
class AppNavigationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun appNavHost_startsAtOnboarding() {
        composeRule.setContent {
            ResumeTheme {
                val navController = rememberNavController()
                AppNavHost(navController, "onboarding")
            }
        }

        composeRule.onNodeWithText("Welcome").assertExists()
    }

    @Test
    fun appNavHost_navigatesToResumeList() {
        composeRule.setContent {
            ResumeTheme {
                val navController = rememberNavController()
                AppNavHost(navController, "onboarding")
            }
        }

        // Simulate onboarding completion
        composeRule.onNodeWithText("Get Started").performClick()
        
        // Should navigate to resume list
        composeRule.onNodeWithText("My Resumes").assertExists()
    }
}
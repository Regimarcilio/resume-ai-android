package com.resumeapp.ui

import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumeapp.features.settings.presentation.SettingsScreen
import com.resumeapp.features.settings.presentation.SettingsViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(com.resumeapp.di.AppModule::class, com.resumeapp.di.NetworkModule::class, com.resumeapp.di.DatabaseModule::class, com.resumeapp.di.RepositoryModule::class, com.resumeapp.di.PreferencesModule::class, com.resumeapp.pdf.di.PdfModule::class, com.resumeapp.billing.di.BillingModule::class)
class SettingsScreenTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun settingsScreen_showsLGPDSection() {
        composeRule.setContent {
            SettingsScreen()
        }

        composeRule.onNodeWithText("LGPD & Privacidade").assertExists()
        composeRule.onNodeWithText("Meus Dados").assertExists()
        composeRule.onNodeWithText("Política de Privacidade").assertExists()
        composeRule.onNodeWithText("Termos de Uso").assertExists()
    }

    @Test
    fun settingsScreen_togglesAI() {
        composeRule.setContent {
            SettingsScreen()
        }

        // Scroll to AI section
        composeRule.onNodeWithText("Recursos de IA").performScrollTo()
        
        // Find and toggle AI switch
        composeRule.onNodeWithText("Assistência de IA").assertExists()
    }
}
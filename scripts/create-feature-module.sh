#!/bin/bash
# =============================================================================
# Create Feature Module Script
# Usage: ./scripts/create-feature-module.sh <feature-name> [description]
# Example: ./scripts/create-feature-module.sh notifications "Push notifications"
# =============================================================================

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# =============================================================================
# Helpers
# =============================================================================
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# =============================================================================
# Validation
# =============================================================================
if [ $# -lt 1 ]; then
    log_error "Usage: $0 <feature-name> [description]"
    log_error "Example: $0 notifications 'Push notifications'"
    exit 1
fi

FEATURE_NAME=$1
FEATURE_DESCRIPTION=${2:-"Feature module for $FEATURE_NAME"}
FEATURE_PACKAGE="com.resumeapp.features.$FEATURE_NAME"

# Validate feature name (lowercase, hyphens allowed)
if [[ ! $FEATURE_NAME =~ ^[a-z][a-z0-9-]*$ ]]; then
    log_error "Feature name must be lowercase, start with letter, only hyphens allowed"
    exit 1
fi

# Check if module already exists
MODULE_DIR="features/$FEATURE_NAME"
if [ -d "$MODULE_DIR" ]; then
    log_error "Module $MODULE_DIR already exists"
    exit 1
fi

# =============================================================================
# Create Module Structure
# =============================================================================
log_info "Creating feature module: $FEATURE_NAME"
log_info "Package: $FEATURE_PACKAGE"
log_info "Description: $FEATURE_DESCRIPTION"

# Create directories
mkdir -p "$MODULE_DIR/src/main/kotlin/${FEATURE_PACKAGE//./\/}/presentation"
mkdir -p "$MODULE_DIR/src/main/kotlin/${FEATURE_PACKAGE//./\/}/domain"
mkdir -p "$MODULE_DIR/src/main/kotlin/${FEATURE_PACKAGE//./\/}/di"
mkdir -p "$MODULE_DIR/src/main/res/values"
mkdir -p "$MODULE_DIR/src/test/kotlin/${FEATURE_PACKAGE//./\/}"
mkdir -p "$MODULE_DIR/src/androidTest/kotlin/${FEATURE_PACKAGE//./\/}"

# =============================================================================
# Generate build.gradle.kts
# =============================================================================
log_info "Generating build.gradle.kts"
cat > "$MODULE_DIR/build.gradle.kts" << EOF
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.parcelize")
    id("org.jetbrains.kotlin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.resumeapp.convention.android-library")
    id("com.resumeapp.convention.compose")
    id("com.resumeapp.convention.ktlint")
    id("com.resumeapp.convention.detekt")
    id("com.resumeapp.convention.testing")
}

android {
    namespace = "$FEATURE_PACKAGE"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/*"
        }
    }
}

dependencies {
    // Core modules
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ai"))
    implementation(project(":core:pdf"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.intents)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
EOF

# =============================================================================
# Generate Domain Module (UseCases + Repository Interfaces)
# =============================================================================
log_info "Generating Domain layer"

# Convert feature name to PascalCase for class names
FEATURE_CLASS=$(echo "$FEATURE_NAME" | sed 's/-\([a-z]\)/\U\1/g' | sed 's/^./\U&/')

# Domain - Repository Interface
cat > "$MODULE_DIR/src/main/kotlin/${FEATURE_PACKAGE//./\/}/domain/${FEATURE_CLASS}Repository.kt" << EOF
package ${FEATURE_PACKAGE}.domain

import com.resumeapp.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for $FEATURE_DESCRIPTION
 * Implementations live in core:data or feature-specific data modules
 */
interface ${FEATURE_CLASS}Repository {
    // TODO: Define repository methods
    // suspend fun getData(): Result<Data>
    // fun observeData(): Flow<Data>
}
EOF

# Domain - UseCase Interface
cat > "$MODULE_DIR/src/main/kotlin/${FEATURE_PACKAGE//./\/}/domain/${FEATURE_CLASS}UseCase.kt" << EOF
package ${FEATURE_PACKAGE}.domain

import com.resumeapp.domain.model.Result

/**
 * UseCase interface for $FEATURE_DESCRIPTION
 * Business logic orchestration
 */
interface ${FEATURE_CLASS}UseCase {
    // TODO: Define use case methods
    // suspend fun execute(params: Params): Result<Outcome>
}
EOF

# Domain - Params/Outcome data classes
cat > "$MODULE_DIR/src/main/kotlin/${FEATURE_PACKAGE//./\/}/domain/${FEATURE_CLASS}Models.kt" << EOF
package ${FEATURE_PACKAGE}.domain

import kotlinx.serialization.Serializable

@Serializable
data class ${FEATURE_CLASS}Params(
    // TODO: Define input parameters
)

@Serializable
data class ${FEATURE_CLASS}Outcome(
    // TODO: Define output
)
EOF

# =============================================================================
# Generate Presentation Layer (ViewModel + Screen)
# =============================================================================
log_info "Generating Presentation layer"

# ViewModel
cat > "$MODULE_DIR/src/main/kotlin/${FEATURE_PACKAGE//./\/}/presentation/${FEATURE_CLASS}ViewModel.kt" << EOF
package ${FEATURE_PACKAGE}.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumeapp.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ${FEATURE_CLASS}ViewModel @Inject constructor(
    // private val useCase: ${FEATURE_CLASS}UseCase
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(${FEATURE_CLASS}UiState.Idle)
    val uiState = _uiState.asStateFlow()

    // Actions
    fun onAction() {
        // TODO: Implement action
        // viewModelScope.launch {
        //     val result = useCase.execute(${FEATURE_CLASS}Params())
        //     result.onSuccess { _uiState.value = ${FEATURE_CLASS}UiState.Success(it) }
        //         .onFailure { _uiState.value = ${FEATURE_CLASS}UiState.Error(it.message) }
        // }
    }
}

// UI States
sealed interface ${FEATURE_CLASS}UiState {
    data class Loading(val message: String = "Carregando...") : ${FEATURE_CLASS}UiState
    data class Success(val data: Any) : ${FEATURE_CLASS}UiState
    data class Error(val message: String) : ${FEATURE_CLASS}UiState
    object Idle : ${FEATURE_CLASS}UiState
}
EOF

# Composable Screen
cat > "$MODULE_DIR/src/main/kotlin/${FEATURE_PACKAGE//./\/}/presentation/${FEATURE_CLASS}Screen.kt" << EOF
package ${FEATURE_PACKAGE}.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ${FEATURE_CLASS}Screen(
    // onNavigate: (Route) -> Unit = {}
) {
    val viewModel = hiltViewModel<${FEATURE_CLASS}ViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is ${FEATURE_CLASS}UiState.Loading -> {
                LoadingScreen(message = it.message)
            }
            is ${FEATURE_CLASS}UiState.Success -> {
                ContentScreen(data = it.data)
            }
            is ${FEATURE_CLASS}UiState.Error -> {
                ErrorScreen(message = it.message, onRetry = { /* viewModel.onRetry() */ })
            }
            ${FEATURE_CLASS}UiState.Idle -> {
                IdleScreen()
            }
        }
    }
}

@Composable
fun LoadingScreen(message: String) {
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.padding
    import androidx.compose.material3.CircularProgressIndicator
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        Text(text = message, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun ContentScreen(data: Any) {
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.padding
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Success!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Text(text = "Data: \$data", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.padding
    import androidx.compose.material3.Button
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.compose.material3.ButtonDefaults

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Error", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Text(text = message, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 3)
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Tentar Novamente", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun IdleScreen() {
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.padding
    import androidx.compose.material3.Button
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.compose.material3.ButtonDefaults

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Ready", fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(text = "Tap to start", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(
                onClick = { /* viewModel.onAction() */ },
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            ) {
                Text("Start")
            }
        }
    }
}
EOF

# =============================================================================
# Generate DI Module
# =============================================================================
log_info "Generating DI Module"

cat > "$MODULE_DIR/src/main/kotlin/${FEATURE_PACKAGE//./\/}/di/${FEATURE_CLASS}Module.kt" << EOF
package ${FEATURE_PACKAGE}.di

import com.resumeapp.domain.model.Result
import dagger.Module
import dagger.Provides
import dagger.hilt.android.InstallIn
import dagger.hilt.android.components.SingletonComponent
import dagger.hilt.android.scopes.Singleton
import ${FEATURE_PACKAGE}.domain.${FEATURE_CLASS}Repository
import ${FEATURE_PACKAGE}.domain.${FEATURE_CLASS}UseCase

@Module
@InstallIn(SingletonComponent::class)
object ${FEATURE_CLASS}Module {

    @Provides
    @Singleton
    fun provide${FEATURE_CLASS}Repository(): ${FEATURE_CLASS}Repository {
        // TODO: Return actual implementation
        // return ${FEATURE_CLASS}RepositoryImpl()
        TODO("Implement ${FEATURE_CLASS}Repository")
    }

    @Provides
    @Singleton
    fun provide${FEATURE_CLASS}UseCase(repository: ${FEATURE_CLASS}Repository): ${FEATURE_CLASS}UseCase {
        // TODO: Return actual implementation
        // return ${FEATURE_CLASS}UseCaseImpl(repository)
        TODO("Implement ${FEATURE_CLASS}UseCase")
    }
}
EOF

# =============================================================================
# Generate Navigation Graph
# =============================================================================
log_info "Generating Navigation Graph"

cat > "$MODULE_DIR/src/main/kotlin/${FEATURE_PACKAGE//./\/}/navigation/${FEATURE_CLASS}NavGraph.kt" << EOF
package ${FEATURE_PACKAGE}.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ${FEATURE_PACKAGE}.presentation.${FEATURE_CLASS}Screen
import ${FEATURE_PACKAGE}.presentation.${FEATURE_CLASS}ViewModel

@Composable
fun ${FEATURE_CLASS}NavHost(
    navController: NavController,
    startDestination: String = "main"
) {
    NavHost(navController, startDestination) {
        composable("main") {
            val viewModel = hiltViewModel<${FEATURE_CLASS}ViewModel>()
            ${FEATURE_CLASS}Screen()
        }
        // Add more destinations as needed
    }
}
EOF

# =============================================================================
# Generate Unit Test Template
# =============================================================================
log_info "Generating Unit Test template"

cat > "$MODULE_DIR/src/test/kotlin/${FEATURE_PACKAGE//./\/}/presentation/${FEATURE_CLASS}ViewModelTest.kt" << EOF
package ${FEATURE_PACKAGE}.presentation

import com.resumeapp.domain.model.Result
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class ${FEATURE_CLASS}ViewModelTest {

    @Mock
    lateinit var mockUseCase: ${FEATURE_CLASS}UseCase

    private lateinit var viewModel: ${FEATURE_CLASS}ViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = ${FEATURE_CLASS}ViewModel(mockUseCase)
    }

    @Test
    fun `initial state is Idle`() = runBlockingTest {
        assertTrue(viewModel.uiState.value is ${FEATURE_CLASS}UiState.Idle)
    }

    @Test
    fun `onAction loads data successfully`() = runBlockingTest {
        // Given
        val expectedData = "test data"
        `when`(mockUseCase.execute(${FEATURE_CLASS}Params())).thenReturn(Result.Success(expectedData))

        // When
        viewModel.onAction()

        // Then
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is ${FEATURE_CLASS}UiState.Success)
        assertEquals(expectedData, (state as ${FEATURE_CLASS}UiState.Success).data)
    }

    @Test
    fun `onAction handles error`() = runBlockingTest {
        // Given
        val errorMessage = "Network error"
        `when`(mockUseCase.execute(${FEATURE_CLASS}Params())).thenReturn(Result.Failure(Exception(errorMessage)))

        // When
        viewModel.onAction()

        // Then
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is ${FEATURE_CLASS}UiState.Error)
        assertEquals(errorMessage, (state as ${FEATURE_CLASS}UiState.Error).message)
    }
}
EOF

# =============================================================================
# Generate Android Test Template
# =============================================================================
cat > "$MODULE_DIR/src/androidTest/kotlin/${FEATURE_PACKAGE//./\/}/presentation/${FEATURE_CLASS}ScreenTest.kt" << EOF
package ${FEATURE_PACKAGE}.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertExists
import org.junit.Rule
import org.junit.Test

class ${FEATURE_CLASS}ScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun idleScreen_displaysReadyState() {
        composeRule.setContent {
            ${FEATURE_CLASS}Screen()
        }

        composeRule.onNodeWithText("Ready").assertExists()
    }
}
EOF

# =============================================================================
# Generate strings.xml
# =============================================================================
cat > "$MODULE_DIR/src/main/res/values/strings.xml" << EOF
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="feature_name">$FEATURE_NAME</string>
    <string name="feature_description">$FEATURE_DESCRIPTION</string>
    <string name="loading">Carregando...</string>
    <string name="error_generic">Ocorreu um erro</string>
    <string name="retry">Tentar novamente</string>
    <string name="success">Sucesso</string>
    <string name="ready">Pronto</string>
</resources>
EOF

# =============================================================================
# Generate consumer-rules.pro
# =============================================================================
cat > "$MODULE_DIR/consumer-rules.pro" << EOF
# ProGuard rules for ${FEATURE_NAME} feature module
# Add any specific rules needed for this module

# Keep Hilt generated classes
-keep class ${FEATURE_PACKAGE}.** { *; }

# Keep serialization classes
-keep class kotlinx.serialization.** { *; }

# Keep Room entities if any
-keep class ${FEATURE_PACKAGE}.domain.model.** { *; }
EOF

# =============================================================================
# Generate proguard-rules.pro
# =============================================================================
cat > "$MODULE_DIR/proguard-rules.pro" << EOF
# ProGuard rules for ${FEATURE_NAME} feature module

# Keep Hilt generated classes
-keep class ${FEATURE_PACKAGE}.** { *; }

# Keep Compose related
-keep class androidx.compose.** { *; }

# Keep serialization
-keep class kotlinx.serialization.** { *; }
EOF

# =============================================================================
# Update settings.gradle.kts reminder
# =============================================================================
log_warn "Don't forget to add the module to settings.gradle.kts:"
log_warn "  include(\"features:$FEATURE_NAME\")"

# =============================================================================
# Summary
# =============================================================================
log_success "Feature module '$FEATURE_NAME' created successfully!"
log_info "Module location: $MODULE_DIR"
log_info ""
log_info "Generated structure:"
log_info "  $MODULE_DIR/"
log_info "  ├── build.gradle.kts"
log_info "  ├── proguard-rules.pro"
log_info "  ├── consumer-rules.pro"
log_info "  ├── src/"
log_info "  │   ├── main/"
log_info "  │   │   ├── kotlin/${FEATURE_PACKAGE//./\/}/"
log_info "  │   │   │   ├── domain/"
log_info "  │   │   │   │   ├── ${FEATURE_CLASS}Repository.kt"
log_info "  │   │   │   │   ├── ${FEATURE_CLASS}UseCase.kt"
log_info "  │   │   │   │   └── ${FEATURE_CLASS}Models.kt"
log_info "  │   │   │   ├── presentation/"
log_info "  │   │   │   │   ├── ${FEATURE_CLASS}ViewModel.kt"
log_info "  │   │   │   │   ├── ${FEATURE_CLASS}Screen.kt"
log_info "  │   │   │   │   └── navigation/"
log_info "  │   │   │   │       └── ${FEATURE_CLASS}NavGraph.kt"
log_info "  │   │   │   └── di/"
log_info "  │   │   │       └── ${FEATURE_CLASS}Module.kt"
log_info "  │   │   ├── res/values/strings.xml"
log_info "  │   ├── test/"
log_info "  │   │   └── kotlin/${FEATURE_PACKAGE//./\/}/presentation/${FEATURE_CLASS}ViewModelTest.kt"
log_info "  │   └── androidTest/"
log_info "  │       └── kotlin/${FEATURE_PACKAGE//./\/}/presentation/${FEATURE_CLASS}ScreenTest.kt"
log_info "  ├── proguard-rules.pro"
log_info "  └── consumer-rules.pro"
log_info ""
log_warn "Next steps:"
log_warn "  1. Add 'include(\"features:$FEATURE_NAME\")' to settings.gradle.kts"
log_warn "  2. Implement ${FEATURE_CLASS}Repository and ${FEATURE_CLASS}UseCase"
log_warn "  3. Add navigation destination in app's NavHost"
log_warn "  4. Run ./gradlew :features:$FEATURE_NAME:build to verify"
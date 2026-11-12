@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resumeapp.features.onboarding.presentation

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.resumeapp.features.onboarding.presentation.OnboardingUiState.Consent
import com.resumeapp.features.onboarding.presentation.OnboardingUiState.TemplatePicker
import com.resumeapp.features.onboarding.presentation.OnboardingUiState.Complete
import com.resumeapp.features.onboarding.presentation.OnboardingUiState.Error
import com.resumeapp.features.onboarding.presentation.OnboardingUiState.Policy
import com.resumeapp.features.onboarding.presentation.OnboardingUiState.Welcome
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Score
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import com.resumeapp.domain.model.ConsentPurpose
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Onboarding flow.
 *
 * The flow is driven by [OnboardingViewModel]'s state machine rather than by a
 * nested NavHost: a NavHost inside another NavHost's destination cannot reuse
 * the parent controller (the ViewModelStore is already set), which crashed
 * with "ViewModelStore should be set before setGraph call".
 */
@Composable
fun OnboardingNavHost(
    onFinished: () -> Unit
) {
    val viewModel = hiltViewModel<OnboardingViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is OnboardingUiState.Welcome -> WelcomeScreen(onNext = { viewModel.onWelcomeNext() })
        is OnboardingUiState.Policy -> PolicyScreen(onAccept = { viewModel.onPolicyAccepted() })
        is OnboardingUiState.Consent -> ConsentScreen(
            onPurposeToggled = { purpose, checked -> viewModel.onConsentPurposeToggled(purpose, checked) },
            onAccept = { viewModel.onConsentAccepted() }
        )
        is OnboardingUiState.TemplatePicker -> TemplatePickerScreen(
            onTemplateSelected = { id -> viewModel.onTemplateSelected(id) },
            onComplete = { viewModel.onCompleteOnboarding() },
            onSkip = { viewModel.onSkipTemplatePicker() }
        )
        is OnboardingUiState.Complete -> {
            CompletionScreen()
            LaunchedEffect(Unit) { onFinished() }
        }
        is OnboardingUiState.Error -> ErrorScreen(
            message = state.message,
            onRetry = { viewModel.onWelcomeNext() }
        )
    }
}

@Composable
fun WelcomeScreen(onNext: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Logo/Icon
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "App Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )

            // Title
            Text(
                text = "Bem-vindo ao ResumeAI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Subtitle
            Text(
                text = "Crie currículos profissionais com IA,\nscore ATS e templates elegantes.",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Features preview
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FeatureRow(Icons.Default.SmartToy, "IA Assist", "Melhore bullets e keywords")
                FeatureRow(Icons.Default.Score, "ATS Score", "Score 0-100 + sugestões")
                FeatureRow(Icons.Default.PictureAsPdf, "PDF Perfeito", "Export pixel-perfect")
                FeatureRow(Icons.Default.Security, "LGPD Compliant", "Seus dados no seu dispositivo")
            }

            // CTA Button
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Começar", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun FeatureRow(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PolicyScreen(onAccept: () -> Unit) {
    var scrollPosition by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Política de Privacidade",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(24.dp)
        )

        // Scrollable policy text
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .weight(1f)
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                // Policy content - in real app, load from assets/privacy-policy.html
                PolicySection("1. Quem Somos", "Este aplicativo é desenvolvido por [Empresa]. Somos o Controlador dos seus dados (LGPD Art. 5º).")
                PolicySection("2. Dados Coletados", "Processamos apenas o que você insere: nome, email, telefone, experiências, educação, habilidades. CPF é opcional.")
                PolicySection("3. Finalidades", "Criar/editar currículo, IA Assist, ATS Scoring, Export PDF, Compartilhamento via Intent.")
                PolicySection("4. Armazenamento", "100% no seu dispositivo. Room + SQLCipher (AES-256). Backup excluído.")
                PolicySection("5. IA Gateway", "Processamento via Gateway próprio (EngAI). Cache semântico, Guardrail, Trace sanitizado.")
                PolicySection("6. Seus Direitos (Art. 18)", "Acesso, Correção, Anonimização, Portabilidade, Eliminação, Revogação. Tela 'Meus Dados' no app.")
                PolicySection("7. Retenção", "Currículos: enquanto não excluir. Cache IA: 30 dias. Traces: 90 dias. Consentimento: 5 anos pós-exclusão.")
                PolicySection("8. Segurança", "SQLCipher AES-256, Encrypted DataStore, Guardrail IA, Sanitização traces.")
                PolicySection("9. Contato DPO", "dpo@empresa.com | Canal no app: Configurações → Privacidade → Falar com DPO")
            }
        }

        // Accept button - only enabled after scroll
        Button(
            onClick = onAccept,
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            enabled = scrollPosition > 0.8f, // Enable after 80% scroll
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = if (scrollPosition > 0.8f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Li e aceito a Política de Privacidade", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PolicySection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()) {
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.padding(top = 4.dp))
        Text(text = content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ConsentScreen(
    onPurposeToggled: (ConsentPurpose, Boolean) -> Unit,
    onAccept: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Seu Consentimento",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(24.dp)
        )

        Text(
            text = "Escolha para quais finalidades autoriza o processamento dos seus dados:",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )

        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ConsentRow(
                purpose = ConsentPurpose.GENERATE_RESUME,
                title = "Criar e editar currículo",
                description = "Essencial para o funcionamento do app",
                checked = true,
                enabled = false,
                onToggle = { }
            )

            ConsentRow(
                purpose = ConsentPurpose.AI_ASSIST,
                title = "IA Assist (melhorar bullets, keywords)",
                description = "Opcional - processa texto do currículo via IA Gateway",
                checked = true,
                enabled = true,
                onToggle = { onPurposeToggled(ConsentPurpose.AI_ASSIST, it) }
            )

            ConsentRow(
                purpose = ConsentPurpose.ATS_SCORING,
                title = "ATS Scoring (score 0-100 + sugestões)",
                description = "Opcional - analisa currículo contra vaga",
                checked = true,
                enabled = true,
                onToggle = { onPurposeToggled(ConsentPurpose.ATS_SCORING, it) }
            )

            ConsentRow(
                purpose = ConsentPurpose.ANALYTICS,
                title = "Analytics anônimos (melhoria do app)",
                description = "Opcional - eventos sem identificação pessoal (PostHog self-hosted)",
                checked = false,
                enabled = true,
                onToggle = { onPurposeToggled(ConsentPurpose.ANALYTICS, it) }
            )
        }

        Button(
            onClick = onAccept,
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Continuar", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ConsentRow(
    purpose: ConsentPurpose,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = if (enabled) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.padding(top = 2.dp))
            Text(text = description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Checkbox(
            checked = checked,
            onCheckedChange = if (enabled) onToggle else null,
            enabled = enabled,
            colors = androidx.compose.material3.CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun TemplatePickerScreen(
    onTemplateSelected: (String) -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Escolha seu Template",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(24.dp)
        )

        Text(
            text = "Selecione o estilo visual do seu currículo. Pode mudar depois.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )

        // Template grid
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.padding(24.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(TemplateOption.defaultTemplates) { template ->
                TemplateCard(
                    template = template,
                    onClick = { onTemplateSelected(template.id) }
                )
            }
        }

        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSkip,
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Pular")
            }
            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Confirmar")
            }
        }
    }
}

@Composable
fun TemplateCard(
    template: TemplateOption,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview placeholder
            Box(
                modifier = Modifier
                    .size(80.dp, 100.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = template.name.substring(0, 1),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(text = template.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (template.isPremium) {
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text(
                            text = "PRO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                        )
                    }
                }
                Text(text = template.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun CompletionScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )

            Text(
                text = "Tudo Pronto! 🎉",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Seu currículo está configurado.\nComece a editar agora mesmo!",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = { /* Navigate to Home/Editor */ },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Ir para o Editor", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = "Erro",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}

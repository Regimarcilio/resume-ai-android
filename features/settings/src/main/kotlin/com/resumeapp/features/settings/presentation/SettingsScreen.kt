package com.resumeapp.features.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun SettingsScreen() {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            androidx.compose.material3.TopAppBar(
                title = { Text("Configurações", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Account Section
                SettingsSection(title = "Conta", icon = Icons.Default.Person) {
                    SettingRow(
                        title = "Perfil",
                        subtitle = "Gerencie suas informações pessoais",
                        icon = Icons.Default.Person,
                        onClick = { /* Navigate to profile */ }
                    )
                    SettingRow(
                        title = "Assinatura",
                        subtitle = "${uiState.subscriptionTier.displayName} - ${uiState.subscriptionStatus}",
                        icon = Icons.Default.Subscriptions,
                        onClick = { /* Navigate to subscription */ }
                    )
                }

                // Privacy & LGPD Section
                SettingsSection(title = "Privacidade e LGPD", icon = Icons.Default.Lock) {
                    SettingRow(
                        title = "Meus Dados (Art. 18 LGPD)",
                        subtitle = "Visualizar, exportar, anonimizar ou excluir seus dados",
                        icon = Icons.Default.Info,
                        onClick = { /* Navigate to My Data screen */ }
                    )
                    SettingRow(
                        title = "Política de Privacidade",
                        subtitle = "Leia nossa política completa",
                        icon = Icons.Default.Policy,
                        onClick = { /* Show privacy policy */ }
                    )
                    SettingRow(
                        title = "Termos de Uso",
                        subtitle = "Leia nossos termos de uso",
                        icon = Icons.Default.Description,
                        onClick = { /* Show terms */ }
                    )
                    SettingRow(
                        title = "Revogar Consentimento",
                        subtitle = "Retire seu consentimento e exclua todos os dados",
                        icon = Icons.Default.Delete,
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = { /* Show revoke confirmation */ }
                    )
                }

                // App Preferences Section
                SettingsSection(title = "Preferências do App", icon = Icons.Default.Settings) {
                    SettingRow(
                        title = "Template Padrão",
                        subtitle = "Escolha o template padrão para novos currículos",
                        icon = Icons.Default.Palette,
                        onClick = { /* Navigate to template picker */ }
                    )
                    SettingRow(
                        title = "Exportação Automática",
                        subtitle = "Salvar backup automático ao exportar",
                        icon = Icons.Default.Download,
                        trailing = {
                            androidx.compose.material3.Switch(
                                checked = false, // uiState.autoExport
                                onCheckedChange = { /* viewModel.setAutoExport(it) */ }
                            )
                        }
                    )
                }

                // About Section
                SettingsSection(title = "Sobre", icon = Icons.Default.Info) {
                    SettingRow(
                        title = "Versão do App",
                        subtitle = "1.0.0 (build 1)",
                        icon = Icons.Default.Info,
                        onClick = { /* Show version info */ }
                    )
                    SettingRow(
                        title = "Licenças de Código Aberto",
                        subtitle = "Ver licenças das bibliotecas usadas",
                        icon = Icons.Default.LibraryBooks,
                        onClick = { /* Show licenses */ }
                    )
                    SettingRow(
                        title = "Enviar Feedback",
                        subtitle = "Ajude-nos a melhorar o app",
                        icon = Icons.Default.Feedback,
                        onClick = { /* Send feedback */ }
                    )
                }

                // Danger Zone
                SettingsSection(title = "Zona de Perigo", icon = Icons.Default.Warning) {
                    SettingRow(
                        title = "Excluir Conta e Todos os Dados",
                        subtitle = "Ação irreversível - remove tudo permanentemente",
                        icon = Icons.Default.DeleteForever,
                        textColor = MaterialTheme.colorScheme.error,
                        iconColor = MaterialTheme.colorScheme.error,
                        onClick = { /* Show delete confirmation */ }
                    )
                }
            }
        }
    }
}

// Reusable Settings Section
@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                content()
            }
        }
    }
}

// Reusable Setting Row
@Composable
fun SettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                androidx.compose.foundation.shape.RectangleShape
            )
            .padding(16.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material.ripple(rememberRipple()),
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor)
                    Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            trailing?.invoke()
        }
    }
}

// My Data Screen (LGPD Art. 18)
@Composable
fun MyDataScreen() {
    val viewModel = hiltViewModel<MyDataViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            androidx.compose.material3.TopAppBar(
                title = { Text("Meus Dados (Art. 18 LGPD)", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { /* navController.popBackStack() */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Data overview card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
                            Spacer(modifier = androidx.compose.foundation.layout.padding(start = 12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Seus Direitos (Art. 18 LGPD)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Você tem controle total sobre seus dados pessoais", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf(
                            RightAction("Confirmar Processamento", "Ver para quais finalidades seus dados são processados", Icons.Default.CheckCircle, { /* navigate */ }),
                            RightAction("Acessar Dados", "Visualizar todos os seus dados armazenados", Icons.Default.Visibility, { /* navigate */ }),
                            RightAction("Corrigir Dados", "Atualize ou corrija informações incorretas", Icons.Default.Edit, { /* navigate */ }),
                            RightAction("Anonimizar", "Substitua dados pessoais por *** mantendo estrutura", Icons.Default.ContentCopy, { /* navigate */ }),
                            RightAction("Eliminar Desnecessários", "Remova cache, logs e dados temporários", Icons.Default.CleaningServices, { /* navigate */ }),
                            RightAction("Portabilidade", "Exporte todos os seus dados em JSON", Icons.Default.Download, { /* navigate */ }),
                            RightAction("Excluir Tudo", "Apague TODOS os dados irreversivelmente", Icons.Default.DeleteForever, { /* navigate */ }, MaterialTheme.colorScheme.error)
                        ) { action ->
                            ActionCard(
                                title = action.title,
                                description = action.description,
                                icon = action.icon,
                                onClick = action.onClick,
                                textColor = action.textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

data class RightAction(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val textColor: Color = MaterialTheme.colorScheme.onSurface
)

@Composable
fun ActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = androidx.compose.foundation.layout.padding(start = 16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// Delete All Confirmation Dialog
@Composable
fun DeleteAllConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Would be used with Dialog composable
}
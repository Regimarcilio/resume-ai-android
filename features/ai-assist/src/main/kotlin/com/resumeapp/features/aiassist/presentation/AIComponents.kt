package com.resumeapp.features.aiassist.presentation

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SmartToy
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

@Composable
fun InlineAIButton(
    sectionType: SectionType,
    currentItems: List<SectionItem>,
    context: ResumeContext,
    onBulletsImproved: (List<String>) -> Unit,
    isLoading: Boolean = false
) {
    var showSuggestions by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // This would be connected to ViewModel in real implementation
    // For now, it's a standalone component

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // AI Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    showSuggestions = true
                    // Trigger AI improvement - would call ViewModel
                },
                enabled = !isLoading,
                modifier = Modifier.padding(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoading) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                if (isLoading) {
                    androidx.compose.material3.ProgressIndicator(
                        modifier = Modifier.size(16.dp).padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(Icons.Default.SmartToy, contentDescription = "IA", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text("Melhorar com IA", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Suggestions Panel
        if (showSuggestions) {
            AnimatedVisibility(visible = showSuggestions) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Sugestões da IA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { /* dismiss */ }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }

                        Text("Clique em 'Gerar sugestões' para ver melhorias da IA para esta seção.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    // Trigger AI - would call ViewModel
                                },
                                enabled = false, // Would be enabled when not loading
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (false) { // isLoading
                                    androidx.compose.material3.ProgressIndicator(modifier = Modifier.size(16.dp).padding(end = 8.dp))
                                } else {
                                    Icon(Icons.Default.SmartToy, contentDescription = "IA", tint = MaterialTheme.colorScheme.onPrimary)
                                }
                                Text("Gerar sugestões")
                            }
                        )
                    }
                }
            }
        }
    }
}
}

// Reusable AI Suggestion Card
@Composable
fun AISuggestionCard(
    suggestion: String,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(suggestion, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Aplicar")
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Dispensar")
                }
            }
        }
    }
}

// AI Floating Action Button for global AI access
@Composable
fun GlobalAIFloatingActionButton(
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    androidx.compose.material3.FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        if (isLoading) {
            androidx.compose.material3.ProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        } else {
            Icon(Icons.Default.SmartToy, contentDescription = "IA Assist", tint = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

// Inline AI Suggestion Banner for sections
@Composable
fun SectionAISuggestionBanner(
    suggestions: List<String>,
    onAccept: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    if (suggestions.isNotEmpty() || isLoading) {
        AnimatedVisibility(visible = suggestions.isNotEmpty() || isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Sugestões da IA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        if (!isLoading) {
                            IconButton(onClick = { /* dismiss all */ }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar sugestões", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }

                    if (isLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.material3.ProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Gerando sugestões...", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(suggestions) { suggestion ->
                                AISuggestionCard(
                                    suggestion = suggestion,
                                    onAccept = { /* accept this suggestion */ },
                                    onDismiss = { /* dismiss this suggestion */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// AI-powered text field with inline suggestions
@Composable
fun AIEnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    sectionType: SectionType,
    context: ResumeContext,
    onAIImprove: () -> Unit,
    isAILoading: Boolean = false,
    aiSuggestions: List<String> = emptyList()
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label and AI button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { /* trigger AI improve */ },
                enabled = !isAILoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAILoading) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                if (isAILoading) {
                    androidx.compose.material3.ProgressIndicator(modifier = Modifier.size(16.dp).padding(end = 8.dp))
                } else {
                    Icon(Icons.Default.SmartToy, contentDescription = "IA", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text("IA", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Text field
        androidx.compose.material3.TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(placeholder) },
            singleLine = false,
            maxLines = 5,
            minLines = 3,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                textColor = MaterialTheme.colorScheme.onSurface,
                placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        )

        // AI Suggestions below text field
        if (suggestions.isNotEmpty()) {
            AnimatedVisibility(visible = true) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Sugestões da IA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(suggestions) { suggestion ->
                                AISuggestionCard(
                                    suggestion = suggestion,
                                    onAccept = { /* apply */ },
                                    onDismiss = { /* dismiss */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
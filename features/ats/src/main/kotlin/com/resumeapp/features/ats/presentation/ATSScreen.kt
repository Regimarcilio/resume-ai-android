package com.resumeapp.features.ats.presentation

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgeDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Score
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Apps
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import com.resumeapp.domain.model.ATSScore
import com.resumeapp.domain.model.ATSSuggestion
import com.resumeapp.domain.model.SuggestionType

@Composable
fun ATSScreen(
    resumeId: String,
    onBack: () -> Unit
) {
    val viewModel = hiltViewModel<ATSViewModel>(resumeId = resumeId)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            androidx.compose.material3.TopAppBar(
                title = { Text("ATS Score", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.refreshATS() },
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        if (uiState.isLoading) {
                            ProgressIndicator(modifier = Modifier.size(16.dp).padding(end = 8.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text("Atualizar", fontSize = 13.sp)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            when (uiState.state) {
                is ATSUiState.Loading -> LoadingScreen(message = "Analisando currículo...")
                is ATSUiState.Error -> ErrorScreen(message = it.message, onRetry = { /* retry */ })
                is ATSUiState.Content -> {
                    ATSContent(
                        score = it.score,
                        suggestions = it.suggestions,
                        onApplySuggestion = { viewModel.applySuggestion(it) },
                        onAddKeyword = { keyword -> /* add keyword */ }
                    )
                }
            }
        }
    }
}

@Composable
fun ATSContent(
    score: ATSScore,
    suggestions: List<ATSSuggestion>,
    onApplySuggestion: (ATSSuggestion) -> Unit,
    onAddKeyword: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Score Gauge
        ScoreGauge(score = score.overallScore)

        // Subscores
        SubscoreRow(
            formatting = score.formattingScore,
            keywordMatch = score.keywordMatch,
            completeness = score.sectionCompleteness
        )

        // Missing Keywords
        if (score.missingKeywords.isNotEmpty()) {
            MissingKeywordsCard(
                keywords = score.missingKeywords,
                onAddKeyword = onAddKeyword
            )
        }

        // Suggestions
        if (suggestions.isNotEmpty()) {
            SuggestionsList(
                suggestions = suggestions,
                onApply = onApplySuggestion
            )
        }
    }
}

@Composable
fun ScoreGauge(score: Int) {
    val color = when {
        score >= 80 -> MaterialTheme.colorScheme.primary
        score >= 60 -> MaterialTheme.colorScheme.tertiary
        score >= 40 -> MaterialTheme.colorScheme.warning
        else -> MaterialTheme.colorScheme.error
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Circular progress indicator
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.size(120.dp),
                    strokeWidth = 12.dp,
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    trackStrokeWidth = 12.dp
                )
                Text(
                    text = "$score",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Text(
                text = "ATS Score",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = when {
                    score >= 80 -> "Excelente! Seu currículo passa na maioria dos ATS"
                    score >= 60 -> "Bom. Pequenas melhorias podem aumentar suas chances"
                    score >= 40 -> "Atenção. Várias melhorias necessárias"
                    else -> "Crítico. Muitas otimizações necessárias"
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun SubscoreRow(
    formatting: Int,
    keywordMatch: Int,
    completeness: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SubscoreItem("Formatação", formatting, Icons.Default.FormatAlignLeft)
        SubscoreItem("Keywords", keywordMatch, Icons.Default.Key)
        SubscoreItem("Completude", completeness, Icons.Default.Checklist)
    }
}

@Composable
fun SubscoreItem(title: String, score: Int, icon: ImageVector) {
    val color = when {
        score >= 80 -> MaterialTheme.colorScheme.primary
        score >= 60 -> MaterialTheme.colorScheme.tertiary
        score >= 40 -> MaterialTheme.colorScheme.warning
        else -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Text(text = "$score", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.material3.LinearProgressIndicator(
            progress = score / 100f,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}

@Composable
fun MissingKeywordsCard(
    keywords: List<String>,
    onAddKeyword: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.warningContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onWarningContainer)
                Text("Keywords Faltando", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onWarningContainer)
            }
            Text("Estas palavras-chave aparecem na vaga mas não no seu currículo:", color = MaterialTheme.colorScheme.onWarningContainer, fontSize = 13.sp)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(keywords.take(10)) { keyword ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(keyword, color = MaterialTheme.colorScheme.onSurface)
                        Button(onClick = { /* add keyword */ }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Text("Adicionar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionsList(
    suggestions: List<ATSSuggestion>,
    onApply: (ATSSuggestion) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sugestões de Melhoria", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { suggestion ->
                SuggestionCard(
                    suggestion = suggestion,
                    onApply = { onApply(suggestion) }
                )
            }
        }
    }
}

@Composable
fun SuggestionCard(
    suggestion: ATSSuggestion,
    onApply: () -> Unit
) {
    val priorityColor = when (suggestion.priority) {
        1 -> MaterialTheme.colorScheme.error
        2 -> MaterialTheme.colorScheme.warning
        else -> MaterialTheme.colorScheme.primary
    }

    val typeIcon = when (suggestion.type) {
        SuggestionType.ADD_KEYWORD -> Icons.Default.Key
        SuggestionType.REPHRASE -> Icons.Default.Edit
        SuggestionType.ADD_METRIC -> Icons.Default.TrendingUp
        SuggestionType.FORMAT_FIX -> Icons.Default.FormatAlignLeft
        SuggestionType.REORDER -> Icons.Default.SwapVert
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = priorityColor, modifier = Modifier.size(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(suggestion.message, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        if (suggestion.example != null) {
                            Text("Exemplo: ${suggestion.example}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                androidx.compose.material3.Badge(
                    badgeContent = { Text("${suggestion.priority}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError) },
                    backgroundColor = priorityColor,
                    badgeColors = androidx.compose.material3.BadgeDefaults.badgeColors(backgroundColor = priorityColor)
                ) {
                    Icon(typeIcon, contentDescription = null, tint = priorityColor)
                }
            }
            suggestion.example?.let { example ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Exemplo: $example", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Button(onClick = { /* apply */ }, colors = ButtonDefaults.buttonColors(containerColor = priorityColor)) {
                        Text("Aplicar")
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            Text(text = message, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Error, contentDescription = "Erro", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
            Text(text = "Erro", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Text(text = message, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                Text("Tentar Novamente")
            }
        }
    }
}
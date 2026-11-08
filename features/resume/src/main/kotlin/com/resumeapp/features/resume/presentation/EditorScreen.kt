package com.resumeapp.features.resume.presentation

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
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
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
fun ResumeNavHost(
    navController: NavController,
    startDestination: String = "list"
) {
    val viewModel = hiltViewModel<ResumeListViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NavHost(navController, startDestination) {
        composable("list") {
            ResumeListScreen(
                resumes = uiState.resumes,
                onNewResume = { viewModel.onNewResume() },
                onEditResume = { id -> navController.navigate("editor/$it") },
                onDeleteResume = { viewModel.onDeleteResume(it) }
            )
        }
        composable(
            route = "editor/{resumeId}",
            arguments = listOf(navArgument("resumeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val resumeId = backStackEntry.getString()!!
            EditorScreen(resumeId = resumeId, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun ResumeListScreen(
    resumes: List<ResumeListItem>,
    onNewResume: () -> Unit,
    onEditResume: (String) -> Unit,
    onDeleteResume: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            androidx.compose.material3.TopAppBar(
                title = { Text("Meus Currículos", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            // List
            if (resumes.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Description,
                    title = "Nenhum currículo ainda",
                    message = "Crie seu primeiro currículo profissional",
                    actionText = "Criar Currículo",
                    onAction = onNewResume
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    items(resumes) { resume ->
                        ResumeCard(
                            resume = resume,
                            onEdit = { onEditResume(resume.id) },
                            onDelete = { onDeleteResume(resume.id) }
                        )
                    }
                }
            }

            // FAB
            androidx.compose.material3.FloatingActionButton(
                onClick = onNewResume,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo currículo")
            }
        }
    }
}

@Composable
fun ResumeCard(
    resume: ResumeListItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            // Template preview thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp, 80.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = resume.templateName.substring(0, 1).uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = resume.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Atualizado: ${resume.updatedAt}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "${resume.sectionsCount} seções", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { /* TODO: Duplicate */ }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicar")
                }
                IconButton(onClick = { /* TODO: Edit */ }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { /* TODO: Delete */ }) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir")
                }
            }
        }
    }
}

@Composable
fun EditorScreen(
    resumeId: String,
    onBack: () -> Unit
) {
    val viewModel = hiltViewModel<EditorViewModel>(resumeId = resumeId)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            androidx.compose.material3.TopAppBar(
                title = { Text(uiState.resume?.personalInfo?.fullName?.ifEmpty { "Editor" } ?: "Editor", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Save indicator
                    if (uiState.isSaving) {
                        androidx.compose.material3.ProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Save button
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = !uiState.isSaving
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Salvar")
                    }
                    // ATS Button
                    IconButton(onClick = { /* TODO: Navigate to ATS */ }) {
                        Icon(Icons.Default.Score, contentDescription = "ATS Score")
                    }
                    // AI Assist Button
                    IconButton(onClick = { /* TODO: Show AI Assist */ }) {
                        Icon(Icons.Default.SmartToy, contentDescription = "IA Assist")
                    }
                    // Export Button
                    IconButton(onClick = { /* TODO: Export */ }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            // Section tabs
            SectionTabs(
                sections = uiState.sections,
                selectedSection = uiState.selectedSectionId,
                onTabClick = { viewModel.selectSection(it) },
                onAddSection = { viewModel.addSection(it) }
            )

            // Section Editor Content
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                when (uiState.editorState) {
                    is EditorUiState.EditorState.Loading -> {
                        LoadingScreen(message = "Carregando currículo...")
                    }
                    is EditorUiState.EditorState.Error -> {
                        ErrorScreen(message = it.message, onRetry = { viewModel.loadResume() })
                    }
                    is EditorUiState.EditorState.Content -> {
                        SectionEditor(
                            section = it.currentSection,
                            onContentChange = { viewModel.updateSectionContent(it) },
                            onAddItem = { viewModel.addSectionItem(it) },
                            onDeleteItem = { viewModel.deleteSectionItem(it) },
                            onReorderItems = { viewModel.reorderSectionItems(it) },
                            onAIImprove = { viewModel.requestAIImprovement(it) },
                            aiSuggestions = it.aiSuggestions,
                            isAILoading = it.isAILoading
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTabs(
    sections: List<SectionTab>,
    selectedSection: String?,
    onTabClick: (String) -> Unit,
    onAddSection: (SectionType) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(androidx.compose.foundation.gestures.rememberScrollState())
    ) {
        sections.forEach { section ->
            androidx.compose.material3.Tab(
                selected = section.id == selectedSection,
                onClick = { onTabClick(section.id) },
                text = { Text(section.title) },
                icon = { Icon(section.icon, contentDescription = null) }
            )
        }

        // Add section button
        androidx.compose.material3.IconButton(
            onClick = { /* Show bottom sheet with section types */ },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar seção")
        }
    }
}

@Composable
fun SectionEditor(
    section: SectionEditorState,
    onContentChange: (String) -> Unit,
    onAddItem: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onReorderItems: (List<String>) -> Unit,
    onAIImprove: () -> Unit,
    aiSuggestions: List<String>,
    isAILoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header with actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = section.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isAILoading) {
                    IconButton(onClick = onAIImprove) {
                        Icon(Icons.Default.SmartToy, contentDescription = "Melhorar com IA", tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    androidx.compose.material3.ProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onAddItem) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar item")
                }
            }
        }

        // AI Suggestions banner
        if (aiSuggestions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Sugestões da IA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(aiSuggestions) { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(suggestion, color = MaterialTheme.colorScheme.onSurface)
                                Button(onClick = { /* Apply suggestion */ }) {
                                    Text("Aplicar")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Items list with drag-reorder
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = true
        ) {
            items(section.items, key = { it.id }) { item ->
                SectionItemCard(
                    item = item,
                    onEdit = { /* Edit item */ },
                    onDelete = { onDeleteItem(item.id) },
                    onDrag = { /* Handle drag */ }
                )
            }
        }

        // Empty state
        if (section.items.isEmpty()) {
            EmptyState(
                icon = Icons.Default.AddBox,
                title = "Seção vazia",
                message = "Adicione seu primeiro item",
                actionText = "Adicionar item",
                onAction = onAddItem
            )
        }
    }
}

@Composable
fun SectionItemCard(
    item: SectionItemState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDrag: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drag handle
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Arrastar para reordenar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                if (item.subtitle.isNotBlank()) {
                    Text(text = item.subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.dateRange.isNotBlank()) {
                    Text(text = item.dateRange, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.highlights.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        item.highlights.forEach { highlight ->
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("\u2022", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(highlight, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { /* Edit */ }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { /* Delete */ }) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Text(text = message, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun LoadingScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.padding(16.dp))
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

// Data classes for UI State
data class ResumeListItem(
    val id: String,
    val name: String,
    val templateName: String,
    val updatedAt: String,
    val sectionsCount: Int
)

data class SectionTab(
    val id: String,
    val title: String,
    val icon: ImageVector
)

data class SectionTabItem(
    val id: String,
    val title: String,
    val icon: ImageVector
)

sealed interface SectionType {
    data class EXPERIENCE : SectionType
    data class EDUCATION : SectionType
    data class SKILLS : SectionType
    data class PROJECTS : SectionType
    data class CERTIFICATIONS : SectionType
    data class LANGUAGES : SectionType
    data class CUSTOM(val name: String) : SectionType
}

data class SectionItemState(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateRange: String,
    val highlights: List<String>
)

sealed interface EditorUiState {
    data class Loading : EditorUiState
    data class Error(val message: String) : EditorUiState
    data class Content(
        val resume: Resume?,
        val sections: List<SectionTab>,
        val selectedSectionId: String?,
        val currentSection: SectionEditorState,
        val selectedSectionId: String?,
        val isSaving: Boolean,
        val editorState: EditorState
    ) : EditorUiState

    sealed interface EditorState {
        data class Loading : EditorState
        data class Error(val message: String) : EditorState
        data class Content(
            val currentSection: SectionEditorState,
            val selectedSectionId: String?,
            val isSaving: Boolean,
            val aiSuggestions: List<String>,
            val isAILoading: Boolean
        ) : EditorState
    }
}

data class SectionEditorState(
    val id: String,
    val title: String,
    val type: SectionType,
    val items: List<SectionItemState>,
    val aiSuggestions: List<String> = emptyList(),
    val isAILoading: Boolean = false
)

data class SectionItemState(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateRange: String,
    val highlights: List<String>
)

data class ResumeListItem(
    val id: String,
    val name: String,
    val templateName: String,
    val updatedAt: String,
    val sectionsCount: Int
)
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resumeapp.features.resume.presentation

import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.ContentCopy
import com.resumeapp.domain.model.SectionType
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.IconButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Score
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Resume flow (list -> editor).
 *
 * Uses its own NavController: a nested NavHost must not reuse the parent
 * controller, otherwise the ViewModelStore is set twice and navigation throws
 * "ViewModelStore should be set before setGraph call".
 */
@Composable
fun ResumeNavHost(
    onOpenAts: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    val viewModel = hiltViewModel<ResumeListViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = uiState as? ResumeListUiState.Content

    NavHost(navController, "list") {
        composable("list") {
            ResumeListScreen(
                resumes = listState?.resumes.orEmpty(),
                onNewResume = { viewModel.onNewResume { id -> navController.navigate("editor/$id") } },
                onEditResume = { id -> navController.navigate("editor/$id") },
                onDeleteResume = { viewModel.onDeleteResume(it) }
            )
        }
        composable(
            route = "editor/{resumeId}",
            arguments = listOf(navArgument("resumeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val resumeId = backStackEntry.arguments?.getString("resumeId") ?: ""
            EditorScreen(
                resumeId = resumeId,
                onBack = { navController.popBackStack() },
                onOpenAts = onOpenAts
            )
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
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
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            // Template preview thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp, 64.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = resume.templateName.substring(0, 1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // weight(1f) com espaco minimo: sem isso os botoes de acao
            // espremem a coluna e o titulo quebra uma letra por linha.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 96.dp)
            ) {
                Text(
                    text = resume.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "Atualizado: ${resume.updatedAt}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${resume.sectionsCount} seções",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Acoes empilhadas verticalmente (48dp de largura em vez de 144dp).
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir")
                }
            }
        }
    }
}

@Composable
fun EditorScreen(
    resumeId: String,
    onBack: () -> Unit,
    onOpenAts: (String) -> Unit = {}
) {
    val viewModel = hiltViewModel<EditorViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val content = uiState as? EditorUiState.Content

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            androidx.compose.material3.TopAppBar(
                title = { Text(content?.resume?.personalInfo?.fullName?.ifEmpty { "Editor" } ?: "Editor", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Save indicator
                    if ((content?.isSaving == true)) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Save button
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = !(content?.isSaving == true)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Salvar")
                    }
                    // ATS Button
                    IconButton(onClick = { onOpenAts(content?.resume?.id.orEmpty()) }) {
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

            // Aba de dados pessoais + abas de secoes
            SectionTabs(
                sections = content?.sections.orEmpty(),
                selectedSection = content?.selectedSectionId,
                onTabClick = { viewModel.selectSection(it) },
                onAddSection = { viewModel.addSection(it) }
            )

            // Section Editor Content
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                // if/else (nunca `return` dentro de um lambda @Composable):
                // um return antecipado desalinha os grupos do Compose e causa
                // IndexOutOfBoundsException em ComposerImpl.exitGroup.
                if (content?.selectedSectionId == PERSONAL_INFO_SECTION_ID) {
                    PersonalInfoEditor(
                        personalInfo = content.resume.personalInfo,
                        onSave = { fullName, email, phone, location, linkedin, summary ->
                            viewModel.updatePersonalInfo(fullName, email, phone, location, linkedin, summary)
                        }
                    )
                } else {
                    when (val editorState = content?.editorState ?: EditorUiState.EditorState.Loading()) {
                        is EditorUiState.EditorState.Loading -> {
                            LoadingScreen(message = "Carregando currículo...")
                        }
                        is EditorUiState.EditorState.Error -> {
                            ErrorScreen(message = editorState.message, onRetry = { viewModel.loadResume() })
                        }
                        is EditorUiState.EditorState.Content -> {
                            SectionEditor(
                                section = editorState.currentSection,
                                onContentChange = { itemsJson -> viewModel.updateSectionContent(itemsJson) },
                                onAddItem = { viewModel.addSectionItem() },
                                onDeleteItem = { itemId -> viewModel.deleteSectionItem(itemId) },
                                onReorderItems = { itemIds -> viewModel.reorderSectionItems(itemIds) },
                                onAIImprove = { viewModel.requestAIImprovement() },
                                aiSuggestions = editorState.aiSuggestions,
                                isAILoading = editorState.isAILoading
                            )
                        }
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
            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        // Aba fixa de dados pessoais (primeira, sempre presente).
        androidx.compose.material3.Tab(
            selected = selectedSection == PERSONAL_INFO_SECTION_ID,
            onClick = { onTabClick(PERSONAL_INFO_SECTION_ID) },
            text = { Text("Dados Pessoais") },
            icon = { Icon(Icons.Default.Person, contentDescription = null) }
        )

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
                    androidx.compose.material3.CircularProgressIndicator(
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
                    // Column (nao LazyColumn): evitar container lazy dentro de
                    // outro container lazy (altura infinita -> crash).
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        aiSuggestions.forEach { suggestion ->
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



/** Id da aba virtual de dados pessoais (nao e uma secao persistida). */
const val PERSONAL_INFO_SECTION_ID = "__personal_info__"

/**
 * Formulario dos dados de contato do curriculo.
 * Sem esta tela o usuario criava um curriculo sem consegue preencher
 * nome, email ou telefone.
 */
@Composable
fun PersonalInfoEditor(
    personalInfo: com.resumeapp.domain.model.PersonalInfo,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var fullName by remember { mutableStateOf(personalInfo.fullName) }
    var email by remember { mutableStateOf(personalInfo.email) }
    var phone by remember { mutableStateOf(personalInfo.phone) }
    var location by remember { mutableStateOf(personalInfo.location) }
    var linkedin by remember { mutableStateOf(personalInfo.linkedin) }
    var summary by remember { mutableStateOf(personalInfo.summary) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        androidx.compose.material3.Text(
            text = "Seus dados",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        androidx.compose.material3.Text(
            text = "Estas informações aparecem no topo do currículo exportado.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        androidx.compose.material3.OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Nome completo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        androidx.compose.material3.OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        androidx.compose.material3.OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Telefone") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        androidx.compose.material3.OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Localização") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        androidx.compose.material3.OutlinedTextField(
            value = linkedin,
            onValueChange = { linkedin = it },
            label = { Text("LinkedIn") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        androidx.compose.material3.OutlinedTextField(
            value = summary,
            onValueChange = { summary = it },
            label = { Text("Resumo profissional") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        androidx.compose.material3.Button(
            onClick = { onSave(fullName, email, phone, location, linkedin, summary) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salvar dados")
        }
    }
}

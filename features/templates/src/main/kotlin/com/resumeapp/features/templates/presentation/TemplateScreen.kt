package com.resumeapp.features.templates.presentation

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
import androidx.compose.foundation.lazy.LazyHorizontalGrid
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.gridItems
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Spacer

@Composable
fun TemplateGalleryScreen(
    onSelect: (Template) -> Unit
) {
    val viewModel = viewModel<TemplateGalleryViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            androidx.compose.material3.TopAppBar(
                title = { Text("Escolha seu Template", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            // Filter tabs
            TemplateFilterTabs(
                selectedFilter = uiState.filter,
                onFilterChange = { viewModel.setFilter(it) }
            )

            // Template Grid
            if (uiState.isLoading) {
                LoadingGrid()
            } else if (uiState.templates.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Palette,
                    title = "Nenhum template encontrado",
                    message = "Tente alterar o filtro",
                    actionText = "Limpar filtros",
                    onAction = { viewModel.setFilter(TemplateFilter.ALL) }
                )
            } else {
                TemplateGrid(
                    templates = uiState.templates,
                    onSelect = onSelect
                )
            }
        }
    }
}

@Composable
fun TemplateFilterTabs(
    selectedFilter: TemplateFilter,
    onFilterChange: (TemplateFilter) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(androidx.compose.foundation.gestures.rememberScrollState())
    ) {
        TemplateFilter.values().forEach { filter ->
            androidx.compose.material3.FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.displayName) },
                leadingIcon = { if (filter.icon != null) Icon(filter.icon!!, contentDescription = null) },
                selectedIcon = { if (filter.icon != null) Icon(filter.icon!!, contentDescription = null) },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(end = 8.dp))
        }
    }
}

@Composable
fun TemplateGrid(
    templates: List<Template>,
    onSelect: (Template) -> Unit
) {
    LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        items(templates) { template ->
            TemplateCard(
                template = template,
                onClick = { onSelect(template) }
            )
        }
    }
}

@Composable
fun TemplateCard(
    template: Template,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Preview placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = template.name.substring(0, 1).uppercase(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column {
                Row {
                    Text(text = template.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (template.isPremium) {
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(start = 8.dp))
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
                Text(text = template.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = androidx.compose.ui.text.overflow.Ellipsis)
            }
        }
    }
}

@Composable
fun TemplatePreviewScreen(
    template: Template,
    onSelect: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            androidx.compose.material3.TopAppBar(
                title = { Text("Preview: ${template.name}", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    Button(
                        onClick = onSelect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Usar este Template")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            // Full preview
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Full page preview
                Box(
                    modifier = Modifier
                        .width(600.dp)
                        .aspectRatio(1.414f) // A4 ratio
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                        .shadow(elevation = 8.dp)
                ) {
                    // Would render actual template preview here
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Preview do Template: ${template.name}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Visualização em tamanho real do template",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingGrid() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            Text("Carregando templates...", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

enum class TemplateFilter(
    val displayName: String,
    val icon: ImageVector?
) {
    ALL("Todos", Icons.Default.Apps),
    MODERN("Modernos", Icons.Default.Dashboard),
    CLASSIC("Clássicos", Icons.Default.MenuBook),
    CREATIVE("Criativos", Icons.Default.Palette),
    ATS("Otimizados ATS", Icons.Default.CheckCircle),
    PREMIUM("Premium", Icons.Default.Star),
    FREE("Gratuitos", Icons.Default.FreeBreakfast);

    override fun toString(): String = displayName
}

data class Template(
    val id: String,
    val name: String,
    val description: String,
    val category: TemplateCategory,
    val previewImageUrl: String,
    val isPremium: Boolean,
    val isActive: Boolean
)

enum class TemplateCategory {
    MODERN, CLASSIC, CREATIVE, ATS_OPTIMIZED
}
package com.resumeapp.features.templates.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumeapp.domain.model.Template
import com.resumeapp.domain.model.TemplateCategory
import com.resumeapp.domain.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Template Gallery ViewModel - lists the active templates and filters by category.
 */
@HiltViewModel
class TemplateGalleryViewModel @Inject constructor(
    private val templateRepository: TemplateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateGalleryUiState())
    val uiState: StateFlow<TemplateGalleryUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
    }

    fun loadTemplates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val templates = templateRepository.getAllActive()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                templates = templates,
                visibleTemplates = filterTemplates(templates, _uiState.value.filter)
            )
        }
    }

    fun setFilter(filter: TemplateFilter) {
        val state = _uiState.value
        _uiState.value = state.copy(
            filter = filter,
            visibleTemplates = filterTemplates(state.templates, filter)
        )
    }

    private fun filterTemplates(templates: List<Template>, filter: TemplateFilter): List<Template> =
        when (filter) {
            TemplateFilter.ALL -> templates
            TemplateFilter.MODERN -> templates.filter { it.category == TemplateCategory.MODERN }
            TemplateFilter.CLASSIC -> templates.filter { it.category == TemplateCategory.CLASSIC }
            TemplateFilter.CREATIVE -> templates.filter { it.category == TemplateCategory.CREATIVE }
            TemplateFilter.ATS -> templates.filter { it.category == TemplateCategory.ATS_OPTIMIZED }
            TemplateFilter.PREMIUM -> templates.filter { it.isPremium }
            TemplateFilter.FREE -> templates.filterNot { it.isPremium }
        }
}

data class TemplateGalleryUiState(
    val isLoading: Boolean = false,
    val filter: TemplateFilter = TemplateFilter.ALL,
    val templates: List<Template> = emptyList(),
    val visibleTemplates: List<Template> = emptyList()
)

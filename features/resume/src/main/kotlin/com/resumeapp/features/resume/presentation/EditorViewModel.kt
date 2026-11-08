package com.resumeapp.features.resume.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.SectionType
import com.resumeapp.domain.usecase.resume.*
import com.resumeapp.domain.usecase.aiassist.ImproveBulletsUseCase
import com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase
import com.resumeapp.domain.model.SectionItem
import com.resumeapp.domain.usecase.Result.Success
import com.resumeapp.domain.usecase.Result.Failure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val resumeId: String,
    private val getResumeUseCase: GetResumeUseCase,
    private val saveResumeUseCase: SaveResumeUseCase,
    private val updateSectionUseCase: UpdateSectionUseCase,
    private val addSectionUseCase: AddSectionUseCase,
    private val deleteSectionUseCase: DeleteSectionUseCase,
    private val reorderSectionsUseCase: ReorderSectionsUseCase,
    private val improveBulletsUseCase: ImproveBulletsUseCase,
    private val analyzeATSUseCase: AnalyzeATSUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadResume()
    }

    private fun loadResume() {
        viewModelScope.launch {
            _uiState.value = EditorUiState.Loading
            val result = getResumeUseCase(com.resumeapp.domain.usecase.IdParams(resumeId))
            _uiState.value = when (result) {
                is Result.Success -> EditorUiState.Content(
                    resume = result.value,
                    sections = buildSectionTabs(result.value),
                    selectedSectionId = result.value.sections.firstOrNull()?.id,
                    currentSection = buildSectionEditorState(result.value, result.value.sections.firstOrNull()?.id),
                    isSaving = false,
                    editorState = EditorUiState.EditorState.Content(
                        currentSection = buildSectionEditorState(result.value, result.value.sections.firstOrNull()?.id),
                        selectedSectionId = result.value.sections.firstOrNull()?.id,
                        isSaving = false,
                        aiSuggestions = emptyList(),
                        isAILoading = false
                    )
                )
                is Result.Failure -> EditorUiState.Error(result.error.message ?: "Erro ao carregar currículo")
            }
        }
    }

    fun selectSection(sectionId: String) {
        _uiState.update { state ->
            if (state is EditorUiState.Content) {
                val section = state.resume.sections.find { it.id == it }
                state.copy(
                    selectedSectionId = section?.id,
                    currentSection = section?.let { buildSectionEditorState(state.resume, it.id) } ?: state.currentSection,
                    editorState = state.editorState.copy(
                        currentSection = state.currentSection.copy(
                            id = section?.id ?: "",
                            title = section?.title ?: "",
                            type = section?.type ?: SectionType.CUSTOM
                        )
                    )
                )
            } else this
        }
    }

    fun addSection(sectionType: SectionType) {
        _uiState.update { state ->
            if (state is EditorUiState.Content) {
                val newSection = com.resumeapp.domain.model.ResumeSection(
                    type = sectionType,
                    title = sectionType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    itemsJson = "[]",
                    order = state.resume.sections.size
                )
                val updatedResume = state.resume.copy(
                    sections = state.resume.sections + newSection,
                    version = state.resume.version + 1
                )
                state.copy(
                    resume = updatedResume,
                    sections = buildSectionTabs(updatedResume),
                    selectedSectionId = newSection.id,
                    currentSection = buildSectionEditorState(updatedResume, newSection.id),
                    editorState = state.editorState.copy(
                        currentSection = buildSectionEditorState(state.resume, newSection.id)
                    )
                )
            } else this
        }
    }

    fun save() {
        _uiState.update { state ->
            if (state is EditorUiState.Content) {
                state.copy(isSaving = true, editorState = state.editorState.copy(isSaving = true))
            } else this
        }

        viewModelScope.launch {
            val resume = (_uiState.value as? EditorUiState.Content)?.resume ?: return@launch
            val result = saveResumeUseCase(SaveResumeUseCase.Params(resume = _uiState.value as? EditorUiState.Content?.resume ?: return@launch))
            _uiState.update { state ->
                if (state is EditorUiState.Content) {
                    state.copy(
                        isSaving = false,
                        editorState = state.editorState.copy(isSaving = false)
                    )
                } else this
            }
        }
    }

    fun updateSectionContent(sectionId: String, itemsJson: String) {
        _uiState.update { state ->
            if (state is EditorUiState.Content) {
                val updatedSections = state.resume.sections.map { section ->
                    if (section.id == sectionId) {
                        section.copy(itemsJson = itemsJson)
                    } else section
                }
                val updatedResume = state.resume.copy(
                    sections = updatedSections,
                    updatedAt = System.currentTimeMillis(),
                    version = state.resume.version + 1
                )
                val updatedSection = updatedResume.sections.find { it.id == sectionId }
                state.copy(
                    resume = updatedResume,
                    currentSection = updatedSection?.let { buildSectionEditorState(updatedResume, it.id) } ?: state.currentSection,
                    editorState = state.editorState.copy(
                        currentSection = state.currentSection.copy(
                            items = buildSectionItems(updatedSection)
                        )
                    )
                )
            } else this
        }
    }

    fun addSectionItem(sectionId: String) {
        _uiState.update { state ->
            if (state is EditorUiState.Content) {
                val updatedSections = state.resume.sections.map { section ->
                    if (section.id == sectionId) {
                        val newItem = com.resumeapp.domain.model.SectionItem(
                            title = "Novo item",
                            subtitle = "",
                            startDate = "",
                            endDate = "",
                            highlights = emptyList()
                        )
                        val items = com.google.gson.Gson().fromJson(
                            section.itemsJson,
                            com.google.gson.reflect.TypeToken.getParameterized(List::class.java, com.resumeapp.domain.model.SectionItem::class.java).type
                        ) ?: emptyList()
                        section.copy(itemsJson = com.google.gson.Gson().toJson(items + com.resumeapp.domain.model.SectionItem(
                            title = "Novo item",
                            subtitle = "",
                            startDate = "",
                            endDate = "",
                            highlights = emptyList()
                        )))
                    } else section
                }
                val updatedResume = state.resume.copy(
                    sections = updatedSections,
                    version = state.resume.version + 1
                )
                state.copy(
                    resume = updatedResume,
                    currentSection = state.currentSection.copy(
                        items = buildSectionItems(updatedResume.sections.find { it.id == sectionId })
                    ),
                    editorState = state.editorState.copy(
                        currentSection = state.currentSection.copy(
                            items = buildSectionItems(updatedResume.sections.find { it.id == sectionId })
                        )
                    )
                )
            } else this
        }
    }

    fun deleteSectionItem(sectionId: String, itemId: String) {
        // Similar to addSectionItem but removing item
    }

    fun reorderSectionItems(sectionId: String, itemIds: List<String>) {
        // Reorder items in section
    }

    fun requestAIImprovement(sectionId: String) {
        _uiState.update { state ->
            if (state is EditorUiState.Content) {
                state.copy(
                    editorState = state.editorState.copy(isAILoading = true)
                )
            } else this
        }

        viewModelScope.launch {
            val section = (_uiState.value as? EditorUiState.Content)?.resume?.sections
                .find { it.id == sectionId } ?: return@launch

            val items = com.google.gson.Gson().fromJson(
                section.itemsJson,
                com.google.gson.reflect.TypeToken.getParameterized(List::class.java, com.resumeapp.domain.model.SectionItem::class.java).type
            ) ?: emptyList()

            val result = improveBulletsUseCase(ImproveBulletsUseCase.Params(
                sectionType = section.type,
                currentItems = items,
                context = ImproveBulletsUseCase.ResumeContext(),
                maxBullets = 5
            ))

            _uiState.update { state ->
                if (state is EditorUiState.Content) {
                    state.copy(
                        editorState = state.editorState.copy(
                            isAILoading = false,
                            aiSuggestions = when (result) {
                                is Result.Success -> result.value
                                is Result.Failure -> emptyList()
                            }
                        )
                    )
                } else this
            }
        }
    }

    private fun buildSectionTabs(resume: Resume): List<SectionTab> {
        return resume.sections.map { section ->
            SectionTab(
                id = section.id,
                title = section.title,
                icon = sectionTypeToIcon(section.type)
            )
        }
    }

    private fun buildSectionEditorState(resume: Resume, sectionId: String?): SectionEditorState {
        val section = sectionId?.let { id -> resume.sections.find { it.id == id } } ?: resume.sections.firstOrNull()
        val items = section?.let { section ->
            com.google.gson.Gson().fromJson(
                section.itemsJson,
                com.google.gson.reflect.TypeToken.getParameterized(List::class.java, com.resumeapp.domain.model.SectionItem::class.java).type
            ) ?: emptyList()
        } ?: emptyList()

        return SectionEditorState(
            id = section?.id ?: "",
            title = section?.title ?: "",
            type = section?.type ?: SectionType.CUSTOM,
            items = items.map { item ->
                SectionItemState(
                    id = item.id,
                    title = item.title,
                    subtitle = item.subtitle,
                    dateRange = "${item.startDate} - ${item.endDate}",
                    highlights = item.highlights
                )
            }
        )
    }

    private fun buildSectionItems(section: com.resumeapp.domain.model.ResumeSection?): List<SectionItemState> {
        return section?.let { section ->
            val items = com.google.gson.Gson().fromJson(
                section.itemsJson,
                com.google.gson.reflect.TypeToken.getParameterized(List::class.java, com.resumeapp.domain.model.SectionItem::class.java).type
            ) ?: emptyList()
            items.map { item ->
                SectionItemState(
                    id = item.id,
                    title = item.title,
                    subtitle = item.subtitle,
                    dateRange = "${item.startDate} - ${item.endDate}",
                    highlights = item.highlights
                )
            }
        } ?: emptyList()
    }

    private fun sectionTypeToIcon(type: SectionType): ImageVector {
        return when (type) {
            SectionType.EXPERIENCE -> Icons.Default.Work
            SectionType.EDUCATION -> Icons.Default.School
            SectionType.SKILLS -> Icons.Default.Build
            SectionType.PROJECTS -> Icons.Default.Folder
            SectionType.CERTIFICATIONS -> Icons.Default.Verified
            SectionType.LANGUAGES -> Icons.Default.Language
            else -> Icons.Default.Description
        }
    }
}

// UI State Classes
sealed interface EditorUiState {
    data class Loading : EditorUiState
    data class Error(val message: String) : EditorUiState
    data class Content(
        val resume: Resume,
        val sections: List<SectionTab>,
        val selectedSectionId: String?,
        val currentSection: SectionEditorState,
        val isSaving: Boolean,
        val editorState: EditorState
    ) : EditorUiState
    data class Error(val message: String) : EditorUiState

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

data class SectionTab(
    val id: String,
    val title: String,
    val icon: ImageVector
)

data class SectionEditorState(
    val id: String,
    val title: String,
    val type: SectionType,
    val items: List<SectionItemState>,
    val aiSuggestions: List<String> = emptyList(),
    val isAILoading: Boolean = false
) {
    copy(
        id = id,
        title = title,
        type = type,
        items = items,
        aiSuggestions = aiSuggestions,
        isAILoading = isAILoading
    )
}

data class SectionItemState(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateRange: String,
    val highlights: List<String>
)
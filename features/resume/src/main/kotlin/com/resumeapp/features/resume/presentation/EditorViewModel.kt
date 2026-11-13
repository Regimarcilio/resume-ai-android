package com.resumeapp.features.resume.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.ResumeSection
import com.resumeapp.domain.model.SectionItem
import com.resumeapp.domain.model.SectionType
import com.resumeapp.domain.model.displayTitle
import com.resumeapp.domain.usecase.aiassist.ImproveBulletsUseCase
import com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase
import com.resumeapp.domain.usecase.resume.AddSectionUseCase
import com.resumeapp.domain.usecase.resume.DeleteSectionUseCase
import com.resumeapp.domain.usecase.resume.GetResumeUseCase
import com.resumeapp.domain.usecase.resume.ReorderSectionsUseCase
import com.resumeapp.domain.usecase.resume.SaveResumeUseCase
import com.resumeapp.domain.usecase.resume.ExportPdfUseCase
import com.resumeapp.domain.usecase.resume.UpdateResumeUseCase
import com.resumeapp.domain.usecase.resume.UpdateSectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getResumeUseCase: GetResumeUseCase,
    private val saveResumeUseCase: SaveResumeUseCase,
    private val updateSectionUseCase: UpdateSectionUseCase,
    private val addSectionUseCase: AddSectionUseCase,
    private val deleteSectionUseCase: DeleteSectionUseCase,
    private val reorderSectionsUseCase: ReorderSectionsUseCase,
    private val improveBulletsUseCase: ImproveBulletsUseCase,
    private val analyzeATSUseCase: AnalyzeATSUseCase,
    private val updateResumeUseCase: UpdateResumeUseCase,
    private val exportPdfUseCase: ExportPdfUseCase
) : ViewModel() {

    private val gson = Gson()
    private val resumeId: String = savedStateHandle.get<String>("resumeId") ?: ""

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Loading())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        loadResume()
    }

    // ==================== Loading ====================

    fun loadResume() {
        viewModelScope.launch {
            _uiState.value = EditorUiState.Loading()
            when (val result = getResumeUseCase(com.resumeapp.domain.usecase.IdParams(resumeId))) {
                is Result.Success -> _uiState.value = EditorUiState.Content(
                    resume = result.value,
                    sections = buildSectionTabs(result.value),
                    // Abre nos dados pessoais: e o primeiro passo obrigatorio.
                    selectedSectionId = PERSONAL_INFO_SECTION_ID,
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
                is Result.Failure -> _uiState.value = EditorUiState.Error(result.errorMessage)
            }
        }
    }

    // ==================== Section selection ====================

    fun selectSection(sectionId: String) {
        val current = _uiState.value as? EditorUiState.Content ?: return

        // Aba virtual de dados pessoais: nao ha secao correspondente.
        if (sectionId == PERSONAL_INFO_SECTION_ID) {
            _uiState.value = current.copy(selectedSectionId = sectionId)
            return
        }

        val section = current.resume.sections.find { it.id == sectionId } ?: return
        val editor = buildSectionEditorState(current.resume, sectionId)
        _uiState.value = current.copy(
            selectedSectionId = sectionId,
            currentSection = editor,
            editorState = current.editorState.let {
                if (it is EditorUiState.EditorState.Content) {
                    it.copy(currentSection = editor, selectedSectionId = sectionId)
                } else {
                    it
                }
            }
        )
    }

    // ==================== Section mutation ====================

    fun addSection(sectionType: SectionType) {
        val current = _uiState.value as? EditorUiState.Content ?: return
        viewModelScope.launch {
            val newSection = ResumeSection(
                resumeId = resumeId,
                type = sectionType,
                title = sectionType.displayTitle,
                itemsJson = "[]",
                order = current.resume.sections.size
            )
            when (val result = addSectionUseCase(AddSectionUseCase.Params(resumeId, newSection))) {
                is Result.Success -> applyResume(result.value)
                is Result.Failure -> setEditorError(result.errorMessage)
            }
        }
    }

    fun deleteSection(sectionId: String) {
        viewModelScope.launch {
            when (val result = deleteSectionUseCase(DeleteSectionUseCase.Params(resumeId, sectionId))) {
                is Result.Success -> applyResume(result.value)
                is Result.Failure -> setEditorError(result.errorMessage)
            }
        }
    }

    fun reorderSections(sectionIds: List<String>) {
        viewModelScope.launch {
            val orders = sectionIds.withIndex().associate { (index, id) -> id to index }
            when (val result = reorderSectionsUseCase(ReorderSectionsUseCase.Params(resumeId, orders))) {
                is Result.Success -> applyResume(result.value)
                is Result.Failure -> setEditorError(result.errorMessage)
            }
        }
    }

    /** Atualiza o conteudo JSON da secao atualmente selecionada. */
    fun updateSectionContent(itemsJson: String) {
        val sectionId = (_uiState.value as? EditorUiState.Content)?.selectedSectionId ?: return
        updateSectionContent(sectionId, itemsJson)
    }

    private fun updateSectionContent(sectionId: String, itemsJson: String) {
        viewModelScope.launch {
            when (
                val result = updateSectionUseCase(
                    UpdateSectionUseCase.Params(resumeId = resumeId, sectionId = sectionId, itemsJson = itemsJson)
                )
            ) {
                is Result.Success -> applyResume(result.value)
                is Result.Failure -> setEditorError(result.errorMessage)
            }
        }
    }

    fun save() {
        val current = _uiState.value as? EditorUiState.Content ?: return
        _uiState.value = current.copy(isSaving = true)
        viewModelScope.launch {
            when (val result = saveResumeUseCase(SaveResumeUseCase.Params(resume = current.resume))) {
                is Result.Success -> {
                    val updated = _uiState.value as? EditorUiState.Content ?: return@launch
                    _uiState.value = updated.copy(isSaving = false)
                }
                is Result.Failure -> setEditorError(result.errorMessage)
            }
        }
    }

    // ==================== Section items ====================

    fun addSectionItem() {
        val current = _uiState.value as? EditorUiState.Content ?: return
        val sectionId = current.selectedSectionId ?: return
        val section = current.resume.sections.find { it.id == sectionId } ?: return
        val items = parseItems(section.itemsJson).toMutableList()
        items.add(
            SectionItem(
                title = "Novo item",
                subtitle = "",
                startDate = "",
                endDate = "",
                highlights = emptyList()
            )
        )
        updateSectionContent(sectionId, gson.toJson(items))
    }

    fun deleteSectionItem(itemId: String) {
        val current = _uiState.value as? EditorUiState.Content ?: return
        val sectionId = current.selectedSectionId ?: return
        val section = current.resume.sections.find { it.id == sectionId } ?: return
        val items = parseItems(section.itemsJson).filterNot { it.id == itemId }
        updateSectionContent(sectionId, gson.toJson(items))
    }

    fun reorderSectionItems(itemIds: List<String>) {
        val current = _uiState.value as? EditorUiState.Content ?: return
        val sectionId = current.selectedSectionId ?: return
        val section = current.resume.sections.find { it.id == sectionId } ?: return
        val items = parseItems(section.itemsJson)
        val reordered = itemIds.mapNotNull { id -> items.find { it.id == id } } +
            items.filter { it.id !in itemIds }
        updateSectionContent(sectionId, gson.toJson(reordered))
    }

    // ==================== Dados pessoais ====================

    /**
     * Atualiza os dados de contato do curriculo.
     * Sem isso o usuario nao conseguia preencher nome/email/telefone.
     */
    fun updatePersonalInfo(
        fullName: String,
        email: String,
        phone: String,
        location: String,
        linkedin: String,
        summary: String
    ) {
        val current = _uiState.value as? EditorUiState.Content ?: return
        viewModelScope.launch {
            val updatedInfo = current.resume.personalInfo.copy(
                fullName = fullName,
                email = email,
                phone = phone,
                location = location,
                linkedin = linkedin,
                summary = summary
            )
            when (
                val result = updateResumeUseCase(
                    UpdateResumeUseCase.Params(
                        resumeId = resumeId,
                        personalInfo = updatedInfo
                    )
                )
            ) {
                is Result.Success -> applyResume(result.value)
                is Result.Failure -> setEditorError(result.errorMessage)
            }
        }
    }

    // ==================== Exportacao PDF ====================

    /**
     * Gera o PDF do curriculo atual.
     * O callback recebe os bytes para a UI gravar e compartilhar.
     */
    fun exportPdf(onPdf: (ByteArray) -> Unit, onError: (String) -> Unit = {}) {
        val current = _uiState.value as? EditorUiState.Content ?: return
        viewModelScope.launch {
            when (val result = exportPdfUseCase(ExportPdfUseCase.Params(current.resume))) {
                is Result.Success -> onPdf(result.value)
                is Result.Failure -> onError(result.errorMessage)
            }
        }
    }

    // ==================== AI assist ====================

    fun requestAIImprovement() {
        val current = _uiState.value as? EditorUiState.Content ?: return
        val sectionId = current.selectedSectionId ?: return
        val section = current.resume.sections.find { it.id == sectionId } ?: return

        setEditorLoading(true)
        viewModelScope.launch {
            val result = improveBulletsUseCase(
                ImproveBulletsUseCase.Params(
                    sectionType = section.type,
                    currentItems = parseItems(section.itemsJson),
                    context = ImproveBulletsUseCase.ResumeContext(),
                    maxBullets = 5
                )
            )
            val state = _uiState.value as? EditorUiState.Content ?: return@launch
            val editor = state.editorState
            _uiState.value = state.copy(
                editorState = if (editor is EditorUiState.EditorState.Content) {
                    editor.copy(
                        isAILoading = false,
                        aiSuggestions = when (result) {
                            is Result.Success -> result.value
                            is Result.Failure -> emptyList()
                        }
                    )
                } else {
                    editor
                }
            )
        }
    }

    fun runATSAnalysis(jobDescription: String? = null) {
        viewModelScope.launch {
            when (val result = analyzeATSUseCase(AnalyzeATSUseCase.Params(resumeId, jobDescription))) {
                is Result.Success -> Unit
                is Result.Failure -> setEditorError(result.errorMessage)
            }
        }
    }

    // ==================== Helpers ====================

    private fun applyResume(resume: Resume) {
        val current = _uiState.value as? EditorUiState.Content
        val selectedId = current?.selectedSectionId
            ?.takeIf { it == PERSONAL_INFO_SECTION_ID || resume.sections.any { s -> s.id == it } }
            ?: resume.sections.firstOrNull()?.id
        val editor = buildSectionEditorState(resume, selectedId)
        _uiState.value = EditorUiState.Content(
            resume = resume,
            sections = buildSectionTabs(resume),
            selectedSectionId = selectedId,
            currentSection = editor,
            isSaving = false,
            editorState = EditorUiState.EditorState.Content(
                currentSection = editor,
                selectedSectionId = selectedId,
                isSaving = false,
                aiSuggestions = emptyList(),
                isAILoading = false
            )
        )
    }

    private fun setEditorLoading(loading: Boolean) {
        val state = _uiState.value as? EditorUiState.Content ?: return
        val editor = state.editorState
        _uiState.value = state.copy(
            editorState = if (editor is EditorUiState.EditorState.Content) {
                editor.copy(isAILoading = loading)
            } else {
                editor
            }
        )
    }

    private fun setEditorError(message: String) {
        _uiState.value = EditorUiState.Error(message)
    }

    private fun parseItems(json: String): List<SectionItem> {
        val type = object : TypeToken<List<SectionItem>>() {}.type
        return runCatching { gson.fromJson<List<SectionItem>>(json, type) }.getOrNull().orEmpty()
    }

    private fun buildSectionTabs(resume: Resume): List<SectionTab> =
        resume.sections.map { section ->
            SectionTab(id = section.id, title = section.title, icon = sectionTypeToIcon(section.type))
        }

    private fun buildSectionEditorState(resume: Resume, sectionId: String?): SectionEditorState {
        val section = sectionId?.let { id -> resume.sections.find { it.id == id } }
            ?: resume.sections.firstOrNull()
        return SectionEditorState(
            id = section?.id ?: "",
            title = section?.title ?: "",
            type = section?.type ?: SectionType.CUSTOM,
            items = section?.let { parseItems(it.itemsJson) }?.map { item ->
                SectionItemState(
                    id = item.id,
                    title = item.title,
                    subtitle = item.subtitle,
                    dateRange = "${item.startDate} - ${item.endDate}",
                    highlights = item.highlights
                )
            }.orEmpty()
        )
    }

    private fun sectionTypeToIcon(type: SectionType): ImageVector = when (type) {
        SectionType.EXPERIENCE -> Icons.Default.Work
        SectionType.EDUCATION -> Icons.Default.School
        SectionType.SKILLS -> Icons.Default.Build
        SectionType.PROJECTS -> Icons.Default.Folder
        SectionType.CERTIFICATIONS -> Icons.Default.Verified
        SectionType.LANGUAGES -> Icons.Default.Language
        else -> Icons.Default.Description
    }
}

// ==================== UI State ====================

sealed interface EditorUiState {
    data class Loading(val placeholder: Unit = Unit) : EditorUiState
    data class Error(val message: String) : EditorUiState
    data class Content(
        val resume: Resume,
        val sections: List<SectionTab>,
        val selectedSectionId: String?,
        val currentSection: SectionEditorState,
        val isSaving: Boolean,
        val editorState: EditorState
    ) : EditorUiState

    sealed interface EditorState {
        data class Loading(val placeholder: Unit = Unit) : EditorState
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
)

data class SectionItemState(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateRange: String,
    val highlights: List<String>
)

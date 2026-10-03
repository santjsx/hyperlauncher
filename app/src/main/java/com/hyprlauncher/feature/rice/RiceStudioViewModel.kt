package com.hyprlauncher.feature.rice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyprlauncher.data.repository.CustomizationRepository
import com.hyprlauncher.data.repository.RiceRepository
import com.hyprlauncher.data.repository.ThemeRepository
import com.hyprlauncher.domain.model.AppGridConfig
import com.hyprlauncher.domain.model.DockConfig
import com.hyprlauncher.domain.model.IconConfig
import com.hyprlauncher.domain.model.LayoutConfig
import com.hyprlauncher.domain.model.RiceDraft
import com.hyprlauncher.domain.model.RiceProfile
import com.hyprlauncher.domain.model.ThemeConfig
import com.hyprlauncher.domain.model.TypographyConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RiceStudioSection(val title: String) {
    COLORS("COLORS"),
    LAYOUT("LAYOUT"),
    GRID_DOCK("GRID & DOCK"),
    TYPOGRAPHY("TYPOGRAPHY"),
    ICONS("ICONS")
}

data class RiceStudioUiState(
    val profiles: List<RiceProfile> = emptyList(),
    val activeProfile: RiceProfile? = null,
    val draft: RiceDraft = RiceDraft(),
    val currentSection: RiceStudioSection = RiceStudioSection.COLORS,
    val exportedJson: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val isExportModalVisible: Boolean = false,
    val isImportModalVisible: Boolean = false
)

@HiltViewModel
class RiceStudioViewModel @Inject constructor(
    private val riceRepository: RiceRepository,
    private val themeRepository: ThemeRepository,
    private val customizationRepository: CustomizationRepository
) : ViewModel() {

    private val draftFlow = MutableStateFlow(RiceDraft())
    private val sectionFlow = MutableStateFlow(RiceStudioSection.COLORS)
    private val exportedJsonFlow = MutableStateFlow<String?>(null)
    private val statusMessageFlow = MutableStateFlow<String?>(null)
    private val errorMessageFlow = MutableStateFlow<String?>(null)
    private val isExportModalVisibleFlow = MutableStateFlow(false)
    private val isImportModalVisibleFlow = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            riceRepository.ensureDefaultRices()
            riceRepository.activeRice.collect { active ->
                if (active != null && draftFlow.value.profileId == null) {
                    draftFlow.value = RiceDraft.fromProfile(active)
                }
            }
        }
    }

    val uiState: StateFlow<RiceStudioUiState> = combine(
        riceRepository.allRices,
        riceRepository.activeRice,
        draftFlow,
        sectionFlow,
        combine(
            exportedJsonFlow,
            statusMessageFlow,
            errorMessageFlow,
            isExportModalVisibleFlow,
            isImportModalVisibleFlow
        ) { exported, status, error, exportVisible, importVisible ->
            Quintet(exported, status, error, exportVisible, importVisible)
        }
    ) { profiles, active, draft, section, quintet ->
        // If draft is completely blank/new and we have an active profile, initialize it
        val effectiveDraft = if (draft.profileId == null && active != null) {
            RiceDraft.fromProfile(active)
        } else {
            draft
        }

        RiceStudioUiState(
            profiles = profiles,
            activeProfile = active,
            draft = effectiveDraft,
            currentSection = section,
            exportedJson = quintet.first,
            statusMessage = quintet.second,
            errorMessage = quintet.third,
            isExportModalVisible = quintet.fourth,
            isImportModalVisible = quintet.fifth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RiceStudioUiState()
    )

    fun selectSection(section: RiceStudioSection) {
        sectionFlow.value = section
    }

    fun selectProfile(profile: RiceProfile) {
        draftFlow.value = RiceDraft.fromProfile(profile)
        statusMessageFlow.value = "Loaded profile '${profile.name}' into Studio"
        errorMessageFlow.value = null
    }

    fun updateDraftTheme(config: ThemeConfig) {
        draftFlow.value = draftFlow.value.copy(themeConfig = config, isDirty = true)
    }

    fun updateDraftLayout(config: LayoutConfig) {
        val currentCustom = draftFlow.value.customizationConfig
        draftFlow.value = draftFlow.value.copy(
            customizationConfig = currentCustom.copy(layout = config),
            isDirty = true
        )
    }

    fun updateDraftDock(config: DockConfig) {
        val currentCustom = draftFlow.value.customizationConfig
        draftFlow.value = draftFlow.value.copy(
            customizationConfig = currentCustom.copy(dock = config),
            isDirty = true
        )
    }

    fun updateDraftGrid(config: AppGridConfig) {
        val currentCustom = draftFlow.value.customizationConfig
        draftFlow.value = draftFlow.value.copy(
            customizationConfig = currentCustom.copy(grid = config),
            isDirty = true
        )
    }

    fun updateDraftTypography(config: TypographyConfig) {
        val currentCustom = draftFlow.value.customizationConfig
        draftFlow.value = draftFlow.value.copy(
            customizationConfig = currentCustom.copy(typography = config),
            isDirty = true
        )
    }

    fun updateDraftIcons(config: IconConfig) {
        val currentCustom = draftFlow.value.customizationConfig
        draftFlow.value = draftFlow.value.copy(
            customizationConfig = currentCustom.copy(icons = config),
            isDirty = true
        )
    }

    fun updateDraftName(name: String) {
        draftFlow.value = draftFlow.value.copy(name = name, isDirty = true)
    }

    fun discardDraft() {
        val active = uiState.value.activeProfile
        if (active != null) {
            draftFlow.value = RiceDraft.fromProfile(active)
            statusMessageFlow.value = "Discarded draft changes"
            errorMessageFlow.value = null
        }
    }

    fun saveDraft() {
        viewModelScope.launch {
            val currentDraft = draftFlow.value
            val profileToSave = currentDraft.toProfile()
            val result = riceRepository.saveRice(profileToSave)
            result.onSuccess {
                draftFlow.value = currentDraft.copy(isDirty = false)
                statusMessageFlow.value = "Profile '${profileToSave.name}' saved"
                errorMessageFlow.value = null
            }.onFailure { ex ->
                errorMessageFlow.value = "Failed to save: ${ex.message}"
            }
        }
    }

    fun saveDraftAsNew(name: String) {
        viewModelScope.launch {
            val newProfile = draftFlow.value.toProfile().copy(
                id = java.util.UUID.randomUUID().toString(),
                name = name.ifBlank { "Custom Rice" },
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val result = riceRepository.saveRice(newProfile)
            result.onSuccess {
                draftFlow.value = RiceDraft.fromProfile(newProfile)
                statusMessageFlow.value = "Created profile '${newProfile.name}'"
                errorMessageFlow.value = null
            }.onFailure { ex ->
                errorMessageFlow.value = "Failed to create profile: ${ex.message}"
            }
        }
    }

    fun applyDraftToLauncher() {
        viewModelScope.launch {
            val currentDraft = draftFlow.value
            val result = riceRepository.applyDraft(currentDraft)
            result.onSuccess {
                statusMessageFlow.value = "Applied draft ricing to launcher"
                errorMessageFlow.value = null
            }.onFailure { ex ->
                errorMessageFlow.value = "Failed to apply draft: ${ex.message}"
            }
        }
    }

    fun applyProfile(profileId: String) {
        viewModelScope.launch {
            val result = riceRepository.applyRice(profileId)
            result.onSuccess {
                val profile = riceRepository.getRiceById(profileId)
                if (profile != null) {
                    draftFlow.value = RiceDraft.fromProfile(profile)
                }
                statusMessageFlow.value = "Active rice changed to '${profile?.name ?: profileId}'"
                errorMessageFlow.value = null
            }.onFailure { ex ->
                errorMessageFlow.value = "Failed to activate: ${ex.message}"
            }
        }
    }

    fun duplicateProfile(profileId: String, newName: String) {
        viewModelScope.launch {
            val result = riceRepository.duplicateRice(profileId, newName)
            result.onSuccess { duplicated ->
                draftFlow.value = RiceDraft.fromProfile(duplicated)
                statusMessageFlow.value = "Duplicated as '${duplicated.name}'"
                errorMessageFlow.value = null
            }.onFailure { ex ->
                errorMessageFlow.value = "Failed to duplicate: ${ex.message}"
            }
        }
    }

    fun renameProfile(profileId: String, newName: String) {
        viewModelScope.launch {
            val result = riceRepository.renameRice(profileId, newName)
            result.onSuccess {
                statusMessageFlow.value = "Renamed profile to '$newName'"
                errorMessageFlow.value = null
            }.onFailure { ex ->
                errorMessageFlow.value = "Failed to rename: ${ex.message}"
            }
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            val result = riceRepository.deleteRice(profileId)
            result.onSuccess {
                statusMessageFlow.value = "Deleted profile"
                errorMessageFlow.value = null
            }.onFailure { ex ->
                errorMessageFlow.value = "Cannot delete: ${ex.message}"
            }
        }
    }

    fun openExportModal(profile: RiceProfile) {
        exportedJsonFlow.value = riceRepository.exportRice(profile)
        isExportModalVisibleFlow.value = true
    }

    fun closeExportModal() {
        isExportModalVisibleFlow.value = false
        exportedJsonFlow.value = null
    }

    fun openImportModal() {
        isImportModalVisibleFlow.value = true
    }

    fun closeImportModal() {
        isImportModalVisibleFlow.value = false
    }

    fun importRiceProfile(jsonString: String) {
        viewModelScope.launch {
            val result = riceRepository.importRice(jsonString)
            result.onSuccess { imported ->
                isImportModalVisibleFlow.value = false
                draftFlow.value = RiceDraft.fromProfile(imported)
                statusMessageFlow.value = "Imported '${imported.name}' successfully"
                errorMessageFlow.value = null
            }.onFailure { ex ->
                errorMessageFlow.value = "Import failed: ${ex.message}"
            }
        }
    }

    fun clearMessages() {
        statusMessageFlow.value = null
        errorMessageFlow.value = null
    }

    private data class Quintet<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )
}

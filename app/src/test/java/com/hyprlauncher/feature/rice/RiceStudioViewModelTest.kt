package com.hyprlauncher.feature.rice

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.core.designsystem.theme.ThemePresetId
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.database.dao.RiceDao
import com.hyprlauncher.data.repository.CustomizationRepository
import com.hyprlauncher.data.repository.DefaultCustomizationRepository
import com.hyprlauncher.data.repository.DefaultRiceRepository
import com.hyprlauncher.data.repository.DefaultThemeRepository
import com.hyprlauncher.data.repository.RiceRepository
import com.hyprlauncher.data.repository.ThemeRepository
import com.hyprlauncher.domain.model.AppGridConfig
import com.hyprlauncher.domain.model.DockConfig
import com.hyprlauncher.domain.model.IconConfig
import com.hyprlauncher.domain.model.LayoutConfig
import com.hyprlauncher.domain.model.RiceProfile
import com.hyprlauncher.domain.model.ThemeConfig
import com.hyprlauncher.domain.model.TypographyConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RiceStudioViewModelTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var context: Context
    private lateinit var database: HyprDatabase
    private lateinit var riceDao: RiceDao
    private lateinit var themeRepository: ThemeRepository
    private lateinit var customizationRepository: CustomizationRepository
    private lateinit var riceRepository: RiceRepository
    private lateinit var viewModel: RiceStudioViewModel

    @Before
    fun setup() = runBlocking(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        riceDao = database.riceDao()

        val themeDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("studio_test_theme.preferences_pb") }
        )
        themeRepository = DefaultThemeRepository(themeDataStore)

        val customizationDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("studio_test_customization.preferences_pb") }
        )
        customizationRepository = DefaultCustomizationRepository(customizationDataStore)

        riceRepository = DefaultRiceRepository(
            riceDao = riceDao,
            themeRepository = themeRepository,
            customizationRepository = customizationRepository
        )
        riceRepository.ensureDefaultRices()

        viewModel = RiceStudioViewModel(
            riceRepository = riceRepository,
            themeRepository = themeRepository,
            customizationRepository = customizationRepository
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun initialStateLoadsProfilesAndInitializesDraft() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.first { it.profiles.isNotEmpty() }
        assertEquals(5, state.profiles.size)
        assertNotNull(state.activeProfile)
        assertEquals("Arch Dark", state.activeProfile?.name)
        assertEquals("Arch Dark", state.draft.name)
        assertFalse(state.draft.isDirty)
        assertEquals(RiceStudioSection.COLORS, state.currentSection)
    }

    @Test
    fun selectSectionUpdatesCurrentSection() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.profiles.isNotEmpty() }

        viewModel.selectSection(RiceStudioSection.GRID_DOCK)
        val s1 = viewModel.uiState.first { it.currentSection == RiceStudioSection.GRID_DOCK }
        assertEquals(RiceStudioSection.GRID_DOCK, s1.currentSection)

        viewModel.selectSection(RiceStudioSection.TYPOGRAPHY)
        val s2 = viewModel.uiState.first { it.currentSection == RiceStudioSection.TYPOGRAPHY }
        assertEquals(RiceStudioSection.TYPOGRAPHY, s2.currentSection)
    }

    @Test
    fun modifyingDraftStateMarksDirtyWithoutWritingToDatabase() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val initial = viewModel.uiState.first { it.profiles.isNotEmpty() && it.activeProfile != null }
        val initialActiveId = initial.activeProfile?.id
        assertNotNull(initialActiveId)

        viewModel.updateDraftTheme(ThemeConfig(presetId = ThemePresetId.NORD, cornerRadiusDp = 16))
        viewModel.updateDraftLayout(LayoutConfig(showClock = false, wallpaperDim = 0.5f))
        viewModel.updateDraftGrid(AppGridConfig(columns = 5, rows = 7))
        viewModel.updateDraftDock(DockConfig(showLabels = false, iconSizeDp = 40))
        viewModel.updateDraftTypography(TypographyConfig(fontScale = 1.3f))
        viewModel.updateDraftIcons(IconConfig(showAppLabels = false))
        viewModel.updateDraftName("Nord Draft")

        val state = viewModel.uiState.first { it.draft.isDirty }
        assertTrue(state.draft.isDirty)
        assertEquals("Nord Draft", state.draft.name)
        assertEquals(ThemePresetId.NORD, state.draft.themeConfig.presetId)
        assertEquals(16, state.draft.themeConfig.cornerRadiusDp)
        assertEquals(false, state.draft.customizationConfig.layout.showClock)
        assertEquals(5, state.draft.customizationConfig.grid.columns)
        assertEquals(40, state.draft.customizationConfig.dock.iconSizeDp)
        assertEquals(1.3f, state.draft.customizationConfig.typography.fontScale)
        assertEquals(false, state.draft.customizationConfig.icons.showAppLabels)

        // Database remains unchanged (PRD Section 23: changes do not persist until saved)
        val persisted = riceRepository.getRiceById(initialActiveId!!)
        assertNotNull(persisted)
        assertEquals("Arch Dark", persisted?.name)
        assertNotEquals(ThemePresetId.NORD, persisted?.themeConfig?.presetId)
    }

    @Test
    fun discardDraftResetsToActiveProfile() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.profiles.isNotEmpty() }

        viewModel.updateDraftName("Dirty Name")
        viewModel.uiState.first { it.draft.isDirty }

        viewModel.discardDraft()

        val state = viewModel.uiState.first { !it.draft.isDirty && it.statusMessage == "Discarded draft changes" }
        assertFalse(state.draft.isDirty)
        assertEquals("Arch Dark", state.draft.name)
        assertEquals("Discarded draft changes", state.statusMessage)
    }

    @Test
    fun saveDraftPersistsChangesAndClearsDirtyFlag() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val initial = viewModel.uiState.first { it.profiles.isNotEmpty() && it.activeProfile != null && it.draft.profileId != null }
        val activeId = initial.activeProfile!!.id

        viewModel.updateDraftName("Arch Upgraded")
        viewModel.updateDraftTheme(ThemeConfig(presetId = ThemePresetId.ARCH_DARK, cornerRadiusDp = 22))

        viewModel.saveDraft()

        val state = viewModel.uiState.first { it.statusMessage?.contains("saved") == true }
        assertFalse(state.draft.isDirty)
        assertEquals("Profile 'Arch Upgraded' saved", state.statusMessage)

        val retrieved = riceRepository.getRiceById(activeId)
        assertNotNull(retrieved)
        assertEquals("Arch Upgraded", retrieved?.name)
        assertEquals(22, retrieved?.themeConfig?.cornerRadiusDp)
    }

    @Test
    fun saveDraftAsNewCreatesNewProfile() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.profiles.isNotEmpty() }

        viewModel.updateDraftName("Arch Variant")
        viewModel.updateDraftTheme(ThemeConfig(presetId = ThemePresetId.NORD))

        viewModel.saveDraftAsNew("Arch Variant")

        val state = viewModel.uiState.first { it.profiles.size == 6 }
        assertEquals(6, state.profiles.size)
        assertEquals("Arch Variant", state.draft.name)
        assertFalse(state.draft.isDirty)
    }

    @Test
    fun applyDraftToLauncherUpdatesLauncherThemeAndCustomization() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.profiles.isNotEmpty() }

        viewModel.updateDraftTheme(ThemeConfig(presetId = ThemePresetId.DRACULA, cornerRadiusDp = 14))
        viewModel.applyDraftToLauncher()

        val state = viewModel.uiState.first { it.statusMessage == "Applied draft ricing to launcher" }
        assertEquals("Applied draft ricing to launcher", state.statusMessage)
        val activeTheme = themeRepository.themeConfig.first { it.presetId == ThemePresetId.DRACULA }
        assertEquals(ThemePresetId.DRACULA, activeTheme.presetId)
        assertEquals(14, activeTheme.cornerRadiusDp)
    }

    @Test
    fun applyProfileSwitchesActiveProfileAndLoadsDraft() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val initial = viewModel.uiState.first { it.profiles.isNotEmpty() }

        val tokyo = initial.profiles.first { it.name == "Tokyo Night" }
        viewModel.applyProfile(tokyo.id)

        val state = viewModel.uiState.first { it.activeProfile?.id == tokyo.id && it.draft.name == "Tokyo Night" }
        assertEquals(tokyo.id, state.activeProfile?.id)
        assertEquals("Tokyo Night", state.draft.name)
        assertFalse(state.draft.isDirty)
        assertEquals(ThemePresetId.TOKYO_NIGHT, themeRepository.themeConfig.first().presetId)
    }

    @Test
    fun duplicateProfileClonesAndSetsDraft() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val initial = viewModel.uiState.first { it.profiles.isNotEmpty() }

        val original = initial.profiles.first()
        viewModel.duplicateProfile(original.id, "Original Clone")

        val state = viewModel.uiState.first { it.profiles.size == 6 }
        assertEquals(6, state.profiles.size)
        assertEquals("Original Clone", state.draft.name)
        assertFalse(state.draft.isDirty)
    }

    @Test
    fun renameProfileUpdatesName() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val initial = viewModel.uiState.first { it.profiles.isNotEmpty() }

        val target = initial.profiles.last()
        viewModel.renameProfile(target.id, "Custom Renamed Name")

        val state = viewModel.uiState.first { it.profiles.any { p -> p.name == "Custom Renamed Name" } }
        val updated = riceRepository.getRiceById(target.id)
        assertEquals("Custom Renamed Name", updated?.name)
    }

    @Test
    fun deleteProfileRemovesProfile() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val initial = viewModel.uiState.first { it.profiles.isNotEmpty() }

        val toDelete = initial.profiles.last()
        viewModel.deleteProfile(toDelete.id)

        val state = viewModel.uiState.first { it.profiles.size == 4 }
        assertEquals(4, state.profiles.size)
        assertNull(riceRepository.getRiceById(toDelete.id))
    }

    @Test
    fun exportProfileGeneratesJsonAndOpensModal() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val initial = viewModel.uiState.first { it.profiles.isNotEmpty() }

        val profile = initial.profiles.first()
        viewModel.openExportModal(profile)

        val state = viewModel.uiState.first { it.isExportModalVisible }
        assertTrue(state.isExportModalVisible)
        assertNotNull(state.exportedJson)
        assertTrue(state.exportedJson!!.contains(profile.name))

        viewModel.closeExportModal()
        val closedState = viewModel.uiState.first { !it.isExportModalVisible }
        assertFalse(closedState.isExportModalVisible)
    }

    @Test
    fun importProfileJsonImportsProfileAndLoadsDraft() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val initial = viewModel.uiState.first { it.profiles.isNotEmpty() }

        val exportedJson = riceRepository.exportRice(initial.profiles.first())
        viewModel.openImportModal()
        val openState = viewModel.uiState.first { it.isImportModalVisible }
        assertTrue(openState.isImportModalVisible)

        viewModel.importRiceProfile(exportedJson)

        val state = viewModel.uiState.first { it.profiles.size == 6 && !it.isImportModalVisible }
        assertFalse(state.isImportModalVisible)
        assertEquals(6, state.profiles.size)
        assertNull(state.errorMessage)
    }

    @Test
    fun importProfileJsonWithInvalidJsonSetsErrorMessage() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.profiles.isNotEmpty() }

        viewModel.openImportModal()
        viewModel.importRiceProfile("not-a-valid-json")

        val state = viewModel.uiState.first { it.errorMessage != null }
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Import failed"))
    }
}

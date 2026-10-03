package com.hyprlauncher.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.core.designsystem.theme.ThemePresetId
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.database.dao.RiceDao
import com.hyprlauncher.domain.model.AppGridConfig
import com.hyprlauncher.domain.model.CustomizationConfig
import com.hyprlauncher.domain.model.DockConfig
import com.hyprlauncher.domain.model.RiceDraft
import com.hyprlauncher.domain.model.RiceProfile
import com.hyprlauncher.domain.model.ThemeConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
class RiceRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var context: Context
    private lateinit var database: HyprDatabase
    private lateinit var riceDao: RiceDao
    private lateinit var themeRepository: ThemeRepository
    private lateinit var customizationRepository: CustomizationRepository
    private lateinit var repository: RiceRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        riceDao = database.riceDao()

        val themeDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("rice_test_theme.preferences_pb") }
        )
        themeRepository = DefaultThemeRepository(themeDataStore)

        val customizationDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("rice_test_customization.preferences_pb") }
        )
        customizationRepository = DefaultCustomizationRepository(customizationDataStore)

        repository = DefaultRiceRepository(
            riceDao = riceDao,
            themeRepository = themeRepository,
            customizationRepository = customizationRepository
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun ensureDefaultRicesSeedsDefaultProfilesAndSetsArchActive() = runTest(testDispatcher) {
        assertEquals(0, riceDao.getRiceCount())

        repository.ensureDefaultRices()

        val rices = repository.allRices.first()
        assertEquals(5, rices.size)

        val names = rices.map { it.name }
        assertTrue(names.contains("Arch Dark"))
        assertTrue(names.contains("Hyprland Neon"))
        assertTrue(names.contains("Tokyo Night"))
        assertTrue(names.contains("Catppuccin Macchiato"))
        assertTrue(names.contains("Nord Minimal"))

        val active = repository.activeRice.first()
        assertNotNull(active)
        assertEquals("Arch Dark", active?.name)

        // Idempotent seeding
        repository.ensureDefaultRices()
        assertEquals(5, repository.allRices.first().size)
    }

    @Test
    fun getRiceByIdReturnsProfileWhenExists() = runTest(testDispatcher) {
        repository.ensureDefaultRices()
        val all = repository.allRices.first()
        val target = all.first()

        val retrieved = repository.getRiceById(target.id)
        assertNotNull(retrieved)
        assertEquals(target.id, retrieved?.id)
        assertEquals(target.name, retrieved?.name)
    }

    @Test
    fun getRiceByIdReturnsNullWhenNotFound() = runTest(testDispatcher) {
        val retrieved = repository.getRiceById("non-existent-id")
        assertNull(retrieved)
    }

    @Test
    fun saveRiceUpdatesExistingProfile() = runTest(testDispatcher) {
        repository.ensureDefaultRices()
        val target = repository.allRices.first().first()

        val modified = target.copy(
            name = "Arch Linux Rice Custom",
            themeConfig = target.themeConfig.copy(cornerRadiusDp = 20)
        )

        val saveResult = repository.saveRice(modified)
        assertTrue(saveResult.isSuccess)

        val retrieved = repository.getRiceById(target.id)
        assertNotNull(retrieved)
        assertEquals("Arch Linux Rice Custom", retrieved?.name)
        assertEquals(20, retrieved?.themeConfig?.cornerRadiusDp)
    }

    @Test
    fun saveRiceRejectsInvalidProfile() = runTest(testDispatcher) {
        repository.ensureDefaultRices()
        val target = repository.allRices.first().first()

        val invalid = target.copy(
            customizationConfig = CustomizationConfig(
                grid = AppGridConfig(columns = 1) // Column < 2 is invalid
            )
        )

        val result = repository.saveRice(invalid)
        assertTrue(result.isFailure)
    }

    @Test
    fun duplicateRiceCreatesNewProfileWithUniqueId() = runTest(testDispatcher) {
        repository.ensureDefaultRices()
        val target = repository.allRices.first().first()

        val dupResult = repository.duplicateRice(target.id, "Arch Clone")
        assertTrue(dupResult.isSuccess)

        val duplicated = dupResult.getOrThrow()
        assertNotEquals(target.id, duplicated.id)
        assertEquals("Arch Clone", duplicated.name)
        assertEquals(target.themeConfig.presetId, duplicated.themeConfig.presetId)

        val all = repository.allRices.first()
        assertEquals(6, all.size)
    }

    @Test
    fun renameRiceUpdatesNameAndPersists() = runTest(testDispatcher) {
        repository.ensureDefaultRices()
        val target = repository.allRices.first().first()

        val renameResult = repository.renameRice(target.id, "Renamed Arch")
        assertTrue(renameResult.isSuccess)

        val retrieved = repository.getRiceById(target.id)
        assertEquals("Renamed Arch", retrieved?.name)
    }

    @Test
    fun deleteRiceRemovesProfileWhenMultipleExist() = runTest(testDispatcher) {
        repository.ensureDefaultRices()
        val all = repository.allRices.first()
        val toDelete = all.last()

        val deleteResult = repository.deleteRice(toDelete.id)
        assertTrue(deleteResult.isSuccess)

        val remaining = repository.allRices.first()
        assertEquals(4, remaining.size)
        assertNull(repository.getRiceById(toDelete.id))
    }

    @Test
    fun deleteRicePreventsDeletingLastRemainingProfile() = runTest(testDispatcher) {
        repository.ensureDefaultRices()
        val all = repository.allRices.first()

        // Delete 4 profiles
        repository.deleteRice(all[0].id)
        repository.deleteRice(all[1].id)
        repository.deleteRice(all[2].id)
        repository.deleteRice(all[3].id)

        assertEquals(1, repository.allRices.first().size)

        // Attempt to delete the 5th (last remaining)
        val deleteLast = repository.deleteRice(all[4].id)
        assertTrue(deleteLast.isFailure)
        assertEquals(1, repository.allRices.first().size)
    }

    @Test
    fun applyRiceUpdatesActiveProfileAndSynchronizesRepositories() = runTest(testDispatcher) {
        repository.ensureDefaultRices()
        val rices = repository.allRices.first()
        val tokyo = rices.first { it.name == "Tokyo Night" }

        val applyResult = repository.applyRice(tokyo.id)
        assertTrue(applyResult.isSuccess)

        val active = repository.activeRice.first()
        assertEquals(tokyo.id, active?.id)
        assertEquals("Tokyo Night", active?.name)

        // Verify ThemeRepository synced
        val activeTheme = themeRepository.themeConfig.first()
        assertEquals(ThemePresetId.TOKYO_NIGHT, activeTheme.presetId)

        // Verify CustomizationRepository synced
        val activeCustomization = customizationRepository.customizationConfig.first()
        assertEquals(tokyo.customizationConfig.grid.columns, activeCustomization.grid.columns)
        assertEquals(tokyo.customizationConfig.dock.iconSizeDp, activeCustomization.dock.iconSizeDp)
    }

    @Test
    fun applyDraftUpdatesThemeAndCustomizationRepositoriesWithoutPersistingToDao() = runTest(testDispatcher) {
        repository.ensureDefaultRices()
        val initialDaoCount = riceDao.getRiceCount()

        val draft = RiceDraft(
            name = "Live Preview Draft",
            themeConfig = ThemeConfig(presetId = ThemePresetId.CATPPUCCIN, cornerRadiusDp = 18),
            customizationConfig = CustomizationConfig(
                dock = DockConfig(showLabels = false, iconSizeDp = 56)
            ),
            isDirty = true
        )

        val applyResult = repository.applyDraft(draft)
        assertTrue(applyResult.isSuccess)

        // Synchronized to repositories for immediate live launcher effect
        assertEquals(ThemePresetId.CATPPUCCIN, themeRepository.themeConfig.first().presetId)
        assertEquals(18, themeRepository.themeConfig.first().cornerRadiusDp)
        assertEquals(56, customizationRepository.customizationConfig.first().dock.iconSizeDp)

        // Did not write extra records to DAO
        assertEquals(initialDaoCount, riceDao.getRiceCount())
    }

    @Test
    fun exportRiceReturnsFormattedJson() = runTest(testDispatcher) {
        repository.ensureDefaultRices()
        val profile = repository.allRices.first().first()

        val json = repository.exportRice(profile)
        assertTrue(json.contains(profile.id))
        assertTrue(json.contains(profile.name))
        assertTrue(json.contains("themeConfig"))
        assertTrue(json.contains("customizationConfig"))
    }

    @Test
    fun importRiceValidatesAndSavesAsNewProfile() = runTest(testDispatcher) {
        repository.ensureDefaultRices()
        val original = repository.allRices.first().first()
        val exported = repository.exportRice(original)

        val importResult = repository.importRice(exported)
        assertTrue(importResult.isSuccess)

        val imported = importResult.getOrThrow()
        assertNotEquals(original.id, imported.id) // Assigned unique UUID
        assertEquals(6, repository.allRices.first().size)
    }

    @Test
    fun importRiceRejectsMalformedJson() = runTest(testDispatcher) {
        val result = repository.importRice("not-json-content")
        assertTrue(result.isFailure)
    }
}

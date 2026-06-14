package dev.pukan.metroprague.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.pukan.metroprague.domain.model.ThemePreference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreUserPreferencesRepositoryTest {

    @get:Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreUserPreferencesRepository

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test.preferences_pb") },
        )
        repository = DataStoreUserPreferencesRepository(dataStore)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `missing key emits System`() = testScope.runTest {
        val result = repository.themePreference.first()
        assertEquals(ThemePreference.System, result)
    }

    @Test
    fun `stored system value maps to System`() = testScope.runTest {
        dataStore.edit { it[stringPreferencesKey("theme_preference")] = "system" }
        assertEquals(ThemePreference.System, repository.themePreference.first())
    }

    @Test
    fun `stored light value maps to Light`() = testScope.runTest {
        dataStore.edit { it[stringPreferencesKey("theme_preference")] = "light" }
        assertEquals(ThemePreference.Light, repository.themePreference.first())
    }

    @Test
    fun `stored dark value maps to Dark`() = testScope.runTest {
        dataStore.edit { it[stringPreferencesKey("theme_preference")] = "dark" }
        assertEquals(ThemePreference.Dark, repository.themePreference.first())
    }

    @Test
    fun `setThemePreference Dark persists dark`() = testScope.runTest {
        repository.setThemePreference(ThemePreference.Dark)
        assertEquals(ThemePreference.Dark, repository.themePreference.first())
    }

    @Test
    fun `invalid stored value emits System`() = testScope.runTest {
        dataStore.edit { it[stringPreferencesKey("theme_preference")] = "invalid_value" }
        assertEquals(ThemePreference.System, repository.themePreference.first())
    }

    @Test
    fun `new repository instance reading same store restores saved value`() = testScope.runTest {
        repository.setThemePreference(ThemePreference.Dark)

        val repo2 = DataStoreUserPreferencesRepository(dataStore)
        assertEquals(ThemePreference.Dark, repo2.themePreference.first())
    }
}

private fun TestScope.cancel() {
    coroutineContext[kotlinx.coroutines.Job]?.cancel()
}

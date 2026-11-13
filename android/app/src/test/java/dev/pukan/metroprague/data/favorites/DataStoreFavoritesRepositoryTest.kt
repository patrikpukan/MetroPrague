package dev.pukan.metroprague.data.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.pukan.metroprague.domain.model.FavoriteKey
import dev.pukan.metroprague.domain.model.Line
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreFavoritesRepositoryTest {

    @get:Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreFavoritesRepository

    private val muzeumToMotol = FavoriteKey("muzeum", Line.A, "nemocnice-motol")
    private val muzeumToDepo = FavoriteKey("muzeum", Line.A, "depo-hostivar")
    private val florencToLetnany = FavoriteKey("florenc", Line.C, "letnany")

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("favorites.preferences_pb") },
        )
        repository = DataStoreFavoritesRepository(dataStore)
    }

    @After
    fun tearDown() {
        testScope.coroutineContext[Job]?.cancel()
    }

    @Test
    fun `empty store emits empty list`() = testScope.runTest {
        assertEquals(emptyList<FavoriteKey>(), repository.favorites.first())
    }

    @Test
    fun `add then read round trips`() = testScope.runTest {
        repository.add(muzeumToMotol)

        assertEquals(listOf(muzeumToMotol), repository.favorites.first())
    }

    @Test
    fun `adding duplicate keeps one entry in original position`() = testScope.runTest {
        repository.add(muzeumToMotol)
        repository.add(muzeumToDepo)
        repository.add(muzeumToMotol)

        assertEquals(listOf(muzeumToMotol, muzeumToDepo), repository.favorites.first())
    }

    @Test
    fun `remove deletes only matching favorite`() = testScope.runTest {
        repository.add(muzeumToMotol)
        repository.add(muzeumToDepo)
        repository.remove(muzeumToMotol)

        assertEquals(listOf(muzeumToDepo), repository.favorites.first())
    }

    @Test
    fun `insertion order is preserved across three adds`() = testScope.runTest {
        repository.add(muzeumToDepo)
        repository.add(florencToLetnany)
        repository.add(muzeumToMotol)

        assertEquals(
            listOf(muzeumToDepo, florencToLetnany, muzeumToMotol),
            repository.favorites.first(),
        )
    }

    @Test
    fun `new repository over same store reads saved favorites`() = testScope.runTest {
        repository.add(muzeumToMotol)

        val secondRepository = DataStoreFavoritesRepository(dataStore)

        assertEquals(listOf(muzeumToMotol), secondRepository.favorites.first())
    }

    @Test
    fun `concurrent toggles preserve both tap events`() = testScope.runTest {
        coroutineScope {
            repeat(2) {
                launch { repository.toggle(muzeumToMotol) }
            }
        }

        assertEquals(emptyList<FavoriteKey>(), repository.favorites.first())
    }

    @Test
    fun `stored json includes schema version`() = testScope.runTest {
        repository.add(muzeumToMotol)

        val encoded = dataStore.data.first()[stringPreferencesKey("favorites_v1")]
        assertTrue(encoded?.contains("\"version\":1") == true)
    }

    @Test
    fun `malformed json emits empty list`() = testScope.runTest {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("favorites_v1")] = "not json"
        }

        assertEquals(emptyList<FavoriteKey>(), repository.favorites.first())
    }

    @Test
    fun `unknown line is dropped while valid sibling survives`() = testScope.runTest {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("favorites_v1")] =
                """{"version":1,"favorites":[{"stationId":"muzeum","line":"Z","terminusStationId":"unknown"},{"stationId":"muzeum","line":"A","terminusStationId":"depo-hostivar"}]}"""
        }

        assertEquals(listOf(muzeumToDepo), repository.favorites.first())
    }
}

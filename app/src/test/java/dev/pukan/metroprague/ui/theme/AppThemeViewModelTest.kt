package dev.pukan.metroprague.ui.theme

import dev.pukan.metroprague.data.preferences.UserPreferencesRepository
import dev.pukan.metroprague.domain.model.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppThemeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeThemeFlow = MutableStateFlow(ThemePreference.System)

    private val repository = object : UserPreferencesRepository {
        override val themePreference = fakeThemeFlow
        override suspend fun setThemePreference(preference: ThemePreference) {
            fakeThemeFlow.value = preference
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AppThemeViewModel(repository)

    @Test
    fun `initial state is System`() = runTest {
        assertEquals(ThemePreference.System, viewModel().themePreference.value)
    }

    @Test
    fun `repository emission of Dark is reflected in state`() = runTest {
        val vm = viewModel()
        // Subscribe to activate WhileSubscribed sharing before emitting.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.themePreference.collect {}
        }
        fakeThemeFlow.value = ThemePreference.Dark
        assertEquals(ThemePreference.Dark, vm.themePreference.value)
    }

    @Test
    fun `repository emission of Light is reflected in state`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.themePreference.collect {}
        }
        fakeThemeFlow.value = ThemePreference.Light
        assertEquals(ThemePreference.Light, vm.themePreference.value)
    }

    @Test
    fun `repository emission of System after Dark is reflected in state`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.themePreference.collect {}
        }
        fakeThemeFlow.value = ThemePreference.Dark
        fakeThemeFlow.value = ThemePreference.System
        assertEquals(ThemePreference.System, vm.themePreference.value)
    }

    @Test
    fun `Dark remains cached after all subscribers stop beyond timeout`() = runTest {
        val vm = viewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.themePreference.collect {}
        }

        fakeThemeFlow.value = ThemePreference.Dark
        assertEquals(ThemePreference.Dark, vm.themePreference.value)

        collector.cancel()
        runCurrent()
        advanceTimeBy(5_001)
        runCurrent()

        assertEquals(ThemePreference.Dark, vm.themePreference.value)
        assertEquals(ThemePreference.Dark, vm.themePreference.first())
    }
}

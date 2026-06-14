package dev.pukan.metroprague.ui.screens.settings

import dev.pukan.metroprague.data.preferences.UserPreferencesRepository
import dev.pukan.metroprague.domain.model.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeThemeFlow: MutableStateFlow<ThemePreference>
    private var lastSetPreference: ThemePreference? = null
    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeThemeFlow = MutableStateFlow(ThemePreference.System)
        lastSetPreference = null
        repository = object : UserPreferencesRepository {
            override val themePreference = fakeThemeFlow
            override suspend fun setThemePreference(preference: ThemePreference) {
                lastSetPreference = preference
                fakeThemeFlow.value = preference
            }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(repository)

    private fun subscribeAndCollect(vm: SettingsViewModel, scope: kotlinx.coroutines.CoroutineScope) {
        scope.launch(UnconfinedTestDispatcher(testDispatcher.scheduler)) {
            vm.uiState.collect {}
        }
    }

    @Test
    fun `initial state reflects repository System preference`() = runTest {
        val vm = viewModel()
        subscribeAndCollect(vm, backgroundScope)

        val state = vm.uiState.value
        assertEquals(ThemePreference.System, state.theme)
        assertEquals(AppLanguage.System, state.language)
        assertEquals(SettingsDialog.None, state.activeDialog)
    }

    @Test
    fun `initial state reflects repository Dark preference`() = runTest {
        fakeThemeFlow.value = ThemePreference.Dark
        val vm = viewModel()
        subscribeAndCollect(vm, backgroundScope)

        assertEquals(ThemePreference.Dark, vm.uiState.value.theme)
    }

    @Test
    fun `selectTheme Dark writes to repository and closes dialog`() = runTest {
        val vm = viewModel()
        subscribeAndCollect(vm, backgroundScope)
        vm.openThemeSelection()

        vm.selectTheme(ThemePreference.Dark)

        assertEquals(ThemePreference.Dark, vm.uiState.value.theme)
        assertEquals(SettingsDialog.None, vm.uiState.value.activeDialog)
        assertEquals(ThemePreference.Dark, lastSetPreference)
    }

    @Test
    fun `repository emission updates SettingsUiState theme`() = runTest {
        val vm = viewModel()
        subscribeAndCollect(vm, backgroundScope)

        fakeThemeFlow.value = ThemePreference.Light

        assertEquals(ThemePreference.Light, vm.uiState.value.theme)
    }

    @Test
    fun `recreating ViewModel with same fake repository restores Dark`() = runTest {
        fakeThemeFlow.value = ThemePreference.Dark
        val vm = viewModel()
        subscribeAndCollect(vm, backgroundScope)

        assertEquals(ThemePreference.Dark, vm.uiState.value.theme)
    }

    @Test
    fun `dismissDialog does not write to repository`() = runTest {
        val vm = viewModel()
        subscribeAndCollect(vm, backgroundScope)
        vm.openThemeSelection()

        vm.dismissDialog()

        assertEquals(SettingsDialog.None, vm.uiState.value.activeDialog)
        assertNull(lastSetPreference)
    }

    @Test
    fun `openThemeSelection sets ThemeSelection dialog`() = runTest {
        val vm = viewModel()
        subscribeAndCollect(vm, backgroundScope)

        vm.openThemeSelection()

        assertEquals(SettingsDialog.ThemeSelection, vm.uiState.value.activeDialog)
    }

    @Test
    fun `openLanguageSelection sets LanguageSelection dialog`() = runTest {
        val vm = viewModel()
        subscribeAndCollect(vm, backgroundScope)

        vm.openLanguageSelection()

        assertEquals(SettingsDialog.LanguageSelection, vm.uiState.value.activeDialog)
    }

    @Test
    fun `selectLanguage Czech updates language and closes dialog`() = runTest {
        val vm = viewModel()
        subscribeAndCollect(vm, backgroundScope)
        vm.openLanguageSelection()

        vm.selectLanguage(AppLanguage.Czech)

        val state = vm.uiState.value
        assertEquals(AppLanguage.Czech, state.language)
        assertEquals(SettingsDialog.None, state.activeDialog)
    }

    @Test
    fun `selectLanguage does not write to repository`() = runTest {
        val vm = viewModel()
        subscribeAndCollect(vm, backgroundScope)

        vm.selectLanguage(AppLanguage.English)

        assertNull(lastSetPreference)
    }

    @Test
    fun `openChangelog then dismissDialog closes dialog without changing theme or language`() = runTest {
        fakeThemeFlow.value = ThemePreference.Light
        val vm = viewModel()
        subscribeAndCollect(vm, backgroundScope)
        vm.selectLanguage(AppLanguage.English)
        vm.openChangelog()
        assertEquals(SettingsDialog.Changelog, vm.uiState.value.activeDialog)

        vm.dismissDialog()

        val state = vm.uiState.value
        assertEquals(SettingsDialog.None, state.activeDialog)
        assertEquals(ThemePreference.Light, state.theme)
        assertEquals(AppLanguage.English, state.language)
    }

    @Test
    fun `version info defaults are present in state`() = runTest {
        val vm = viewModel()
        subscribeAndCollect(vm, backgroundScope)

        val state = vm.uiState.value
        assertEquals("1.0", state.versionName)
        assertEquals(1, state.versionCode)
    }
}

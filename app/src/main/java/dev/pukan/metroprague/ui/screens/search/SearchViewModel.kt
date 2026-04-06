package dev.pukan.metroprague.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.pukan.metroprague.MetroPragueApplication
import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.Station
import dev.pukan.metroprague.domain.repository.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(
    private val repository: StationRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedLine = MutableStateFlow<Line?>(null)
    val selectedLine: StateFlow<Line?> = _selectedLine

    val filteredStations: StateFlow<List<Station>> = combine(
        repository.getStations(),
        _searchQuery,
        _selectedLine
    ) { stations, query, line ->
        stations.filter { station ->
            val matchesLine = line == null || station.line == line
            val matchesQuery = station.name.contains(query, ignoreCase = true)
            matchesLine && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onLineFilterChange(line: Line?) {
        if (_selectedLine.value == line) {
            _selectedLine.value = null // Toggle off if already selected
        } else {
            _selectedLine.value = line
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MetroPragueApplication)
                val stationRepository = application.container.stationRepository
                SearchViewModel(repository = stationRepository)
            }
        }
    }
}
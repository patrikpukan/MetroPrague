package dev.pukan.metroprague.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.Station
import dev.pukan.metroprague.domain.repository.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
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
            _selectedLine.value = null
        } else {
            _selectedLine.value = line
        }
    }
}

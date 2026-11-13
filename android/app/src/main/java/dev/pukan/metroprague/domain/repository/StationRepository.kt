package dev.pukan.metroprague.domain.repository

import dev.pukan.metroprague.domain.model.Station
import kotlinx.coroutines.flow.Flow

interface StationRepository {
    fun getStations(): Flow<List<Station>>
}
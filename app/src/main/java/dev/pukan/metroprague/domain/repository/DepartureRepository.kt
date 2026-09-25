package dev.pukan.metroprague.domain.repository

import dev.pukan.metroprague.domain.model.DepartureBoard
import kotlinx.coroutines.flow.Flow

interface DepartureRepository {
    fun getDepartureBoard(stationId: String): Flow<DepartureBoard>
}

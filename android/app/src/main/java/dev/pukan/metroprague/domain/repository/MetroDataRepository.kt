package dev.pukan.metroprague.domain.repository

import kotlinx.coroutines.flow.Flow

enum class MetroReloadError { Authentication, RateLimited, NetworkOrData }

data class MetroDataStatus(
    val canReload: Boolean = false,
    val isReloading: Boolean = false,
    val completedStations: Int = 0,
    val totalStations: Int = 0,
    val stationCount: Int = 0,
    val departureCount: Int = 0,
    val reloadedAtMillis: Long? = null,
    val error: MetroReloadError? = null,
)

interface MetroDataRepository {
    val status: Flow<MetroDataStatus>
    suspend fun reload()
}

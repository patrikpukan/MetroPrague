package dev.pukan.metroprague.di

import dev.pukan.metroprague.data.repository.MockStationRepository
import dev.pukan.metroprague.domain.repository.StationRepository

interface AppContainer {
    val stationRepository: StationRepository
}

class DefaultAppContainer : AppContainer {
    override val stationRepository: StationRepository by lazy {
        MockStationRepository()
    }
}

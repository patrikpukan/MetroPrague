package dev.pukan.metroprague.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.pukan.metroprague.data.repository.MockDepartureRepository
import dev.pukan.metroprague.data.repository.MockStationRepository
import dev.pukan.metroprague.domain.repository.DepartureRepository
import dev.pukan.metroprague.domain.repository.StationRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDepartureRepository(impl: MockDepartureRepository): DepartureRepository

    @Binds
    @Singleton
    abstract fun bindStationRepository(impl: MockStationRepository): StationRepository
}

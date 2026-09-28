package dev.pukan.metroprague.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.pukan.metroprague.BuildConfig
import dev.pukan.metroprague.data.golemio.GolemioBoardService
import dev.pukan.metroprague.data.golemio.HttpGolemioBoardService
import dev.pukan.metroprague.data.repository.GolemioDepartureRepository
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
    abstract fun bindStationRepository(impl: MockStationRepository): StationRepository

    companion object {
        @Provides
        @Singleton
        internal fun provideGolemioBoardService(): GolemioBoardService =
            HttpGolemioBoardService(BuildConfig.GOLEMIO_API_KEY)

        @Provides
        @Singleton
        internal fun provideDepartureRepository(
            mock: MockDepartureRepository,
            live: GolemioDepartureRepository,
        ): DepartureRepository = if (BuildConfig.DEBUG && BuildConfig.GOLEMIO_API_KEY.isNotBlank()) {
            live
        } else {
            mock
        }
    }
}

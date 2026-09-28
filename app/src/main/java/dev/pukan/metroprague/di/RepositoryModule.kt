package dev.pukan.metroprague.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Base64
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pukan.metroprague.R
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
        internal fun provideLiveDeparturesConfig(
            @ApplicationContext context: Context,
        ): LiveDeparturesConfig {
            val encodedKey = context.getString(R.string.golemio_api_key_base64)
            val apiKey = String(Base64.decode(encodedKey, Base64.DEFAULT), Charsets.UTF_8)
            val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            return LiveDeparturesConfig(apiKey, isDebuggable)
        }

        @Provides
        @Singleton
        internal fun provideGolemioBoardService(config: LiveDeparturesConfig): GolemioBoardService =
            HttpGolemioBoardService(config.apiKey)

        @Provides
        @Singleton
        internal fun provideDepartureRepository(
            mock: MockDepartureRepository,
            live: GolemioDepartureRepository,
            config: LiveDeparturesConfig,
        ): DepartureRepository = if (config.useLiveDepartures) {
            live
        } else {
            mock
        }
    }
}

internal data class LiveDeparturesConfig(
    val apiKey: String,
    val isDebuggable: Boolean,
) {
    val useLiveDepartures: Boolean get() = isDebuggable && apiKey.isNotBlank()
}

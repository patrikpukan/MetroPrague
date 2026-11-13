package dev.pukan.metroprague.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pukan.metroprague.R
import dev.pukan.metroprague.data.golemio.GolemioBoardService
import dev.pukan.metroprague.data.golemio.HttpGolemioBoardService
import dev.pukan.metroprague.data.golemio.GolemioHttpClient
import dev.pukan.metroprague.data.golemio.GolemioCatalogueService
import dev.pukan.metroprague.data.golemio.HttpGolemioCatalogueService
import dev.pukan.metroprague.data.golemio.MetroDataSnapshot
import dev.pukan.metroprague.data.golemio.MetroSnapshotSerializer
import dev.pukan.metroprague.data.golemio.MetroSnapshotStore
import dev.pukan.metroprague.data.golemio.DataStoreMetroSnapshotStore
import dev.pukan.metroprague.data.repository.GolemioDepartureRepository
import dev.pukan.metroprague.data.repository.GolemioStationRepository
import dev.pukan.metroprague.data.repository.GolemioMetroDataRepository
import dev.pukan.metroprague.data.repository.MockDepartureRepository
import dev.pukan.metroprague.data.repository.MockStationRepository
import dev.pukan.metroprague.domain.repository.DepartureRepository
import dev.pukan.metroprague.domain.repository.StationRepository
import dev.pukan.metroprague.domain.repository.MetroDataRepository
import javax.inject.Singleton

// The context delegate survives Hilt component recreation and owns one store per process.
private val Context.metroSnapshotDataStore: DataStore<MetroDataSnapshot> by dataStore(
    fileName = "metro-data.json",
    serializer = MetroSnapshotSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { MetroDataSnapshot() },
)

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    internal abstract fun bindSnapshotStore(impl: DataStoreMetroSnapshotStore): MetroSnapshotStore

    @Binds
    @Singleton
    internal abstract fun bindMetroDataRepository(impl: GolemioMetroDataRepository): MetroDataRepository

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
        internal fun provideGolemioClient(config: LiveDeparturesConfig): GolemioHttpClient =
            GolemioHttpClient(config.apiKey)

        @Provides
        @Singleton
        internal fun provideGolemioBoardService(client: GolemioHttpClient): GolemioBoardService =
            HttpGolemioBoardService(client)

        @Provides
        @Singleton
        internal fun provideCatalogueService(client: GolemioHttpClient): GolemioCatalogueService =
            HttpGolemioCatalogueService(client)

        @Provides
        @Singleton
        internal fun provideMetroDataStore(@ApplicationContext context: Context): DataStore<MetroDataSnapshot> =
            context.metroSnapshotDataStore

        @Provides
        @Singleton
        internal fun provideStationRepository(
            mock: MockStationRepository,
            saved: GolemioStationRepository,
            config: LiveDeparturesConfig,
        ): StationRepository = if (config.useLiveDepartures) saved else mock

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

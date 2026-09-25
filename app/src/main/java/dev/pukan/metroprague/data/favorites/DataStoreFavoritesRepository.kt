package dev.pukan.metroprague.data.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.pukan.metroprague.domain.model.FavoriteKey
import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.repository.FavoritesRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class DataStoreFavoritesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : FavoritesRepository {

    private val favoritesKey = stringPreferencesKey("favorites_v1")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val favorites: Flow<List<FavoriteKey>> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                decode(preferences[favoritesKey])
            }

    override suspend fun add(key: FavoriteKey) {
        dataStore.edit { preferences ->
            val current = decode(preferences[favoritesKey])
            if (key !in current) {
                preferences[favoritesKey] = encode(current + key)
            }
        }
    }

    override suspend fun remove(key: FavoriteKey) {
        dataStore.edit { preferences ->
            val current = decode(preferences[favoritesKey])
            preferences[favoritesKey] = encode(current.filterNot { it == key })
        }
    }

    override suspend fun toggle(key: FavoriteKey) {
        dataStore.edit { preferences ->
            val current = decode(preferences[favoritesKey])
            preferences[favoritesKey] = encode(
                if (key in current) current.filterNot { it == key } else current + key,
            )
        }
    }

    private fun decode(value: String?): List<FavoriteKey> {
        if (value.isNullOrBlank()) return emptyList()

        val stored = try {
            json.decodeFromString<StoredFavoritesV1>(value)
        } catch (_: SerializationException) {
            return emptyList()
        }

        return stored.favorites.mapNotNull { favorite ->
            val line = Line.entries.firstOrNull { it.name == favorite.line }
                ?: return@mapNotNull null
            FavoriteKey(
                stationId = favorite.stationId,
                line = line,
                terminusStationId = favorite.terminusStationId,
            )
        }
    }

    private fun encode(favorites: List<FavoriteKey>): String = json.encodeToString(
        StoredFavoritesV1(
            favorites = favorites.map { favorite ->
                StoredFavorite(
                    stationId = favorite.stationId,
                    line = favorite.line.name,
                    terminusStationId = favorite.terminusStationId,
                )
            },
        ),
    )
}

@Serializable
private data class StoredFavoritesV1(
    val version: Int = 1,
    val favorites: List<StoredFavorite> = emptyList(),
)

@Serializable
private data class StoredFavorite(
    val stationId: String,
    val line: String,
    val terminusStationId: String,
)

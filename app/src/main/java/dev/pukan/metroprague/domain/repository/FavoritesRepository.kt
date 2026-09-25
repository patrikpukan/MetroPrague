package dev.pukan.metroprague.domain.repository

import dev.pukan.metroprague.domain.model.FavoriteKey
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    val favorites: Flow<List<FavoriteKey>>
    suspend fun add(key: FavoriteKey)
    suspend fun remove(key: FavoriteKey)
}

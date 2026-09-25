package dev.pukan.metroprague.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.pukan.metroprague.data.favorites.DataStoreFavoritesRepository
import dev.pukan.metroprague.domain.repository.FavoritesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FavoritesModule {

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(
        impl: DataStoreFavoritesRepository,
    ): FavoritesRepository
}

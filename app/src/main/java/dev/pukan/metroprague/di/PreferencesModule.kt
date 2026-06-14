package dev.pukan.metroprague.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.pukan.metroprague.data.preferences.DataStoreUserPreferencesRepository
import dev.pukan.metroprague.data.preferences.UserPreferencesRepository
import javax.inject.Singleton

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: DataStoreUserPreferencesRepository,
    ): UserPreferencesRepository

    companion object {
        @Provides
        @Singleton
        @JvmStatic
        fun provideDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = context.userPreferencesDataStore
    }
}

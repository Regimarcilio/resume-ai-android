package com.resumeapp.di.modules

import android.content.Context
import androidx.datastore.preferences.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.security.crypto.MasterKey
import com.resumeapp.data.prefs.UserPrefs
import com.resumeapp.data.prefs.UserPrefsImpl
import com.resumeapp.data.prefs.ConsentRepositoryImpl
import com.resumeapp.domain.repository.ConsentRepository
import com.resumeapp.data.prefs.SubscriptionPrefs
import com.resumeapp.data.prefs.SubscriptionPrefsImpl
import dagger.hilt.android.InstallIn
import dagger.hilt.android.components.SingletonComponent
import dagger.hilt.android.scopes.ApplicationContext
import dagger.hilt.android.scopes.Singleton
import dagger.Module
import dagger.Provides

/**
 * Preferences Module - DataStore backed preferences
 */
@InstallIn(SingletonComponent::class)
@Module
object PreferencesModule {

    @Provides
    @Singleton
    fun provideUserPrefs(
        @ApplicationContext context: Context,
        dataStore: DataStore<Preferences>,
        masterKey: MasterKey
    ): UserPrefs = UserPrefsImpl(context, dataStore, masterKey)

    @Provides
    @Singleton
    fun provideConsentRepository(userPrefs: UserPrefs): ConsentRepository = ConsentRepositoryImpl(userPrefs)

    @Provides
    @Singleton
    fun provideSubscriptionPrefs(userPrefs: UserPrefs): SubscriptionPrefs = SubscriptionPrefsImpl(userPrefs)
}
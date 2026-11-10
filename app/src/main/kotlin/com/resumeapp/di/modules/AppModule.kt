package com.resumeapp.di.modules

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.work.HiltWorkerFactory
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.Configuration
import com.resumeapp.data.db.AppDatabase
import dagger.hilt.android.InstallIn
import dagger.hilt.android.components.SingletonComponent
import dagger.hilt.android.scopes.ApplicationContext
import dagger.hilt.android.scopes.Singleton
import dagger.Module
import dagger.Provides
import jakarta.inject.Inject

/**
 * Main Application Module - Provides application-level dependencies
 * All singletons that live for the entire app lifecycle
 */
@InstallIn(SingletonComponent::class)
@Module(includes = [SyncModule::class])
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideMasterKey(@ApplicationContext context: Context): MasterKey {
        val spec = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false) // Device credential optional for better UX
            .build()
        return spec
    }

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context,
        masterKey: MasterKey
    ) = EncryptedSharedPreferences.create(
        "secure_prefs",
        masterKey,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context) = context.preferencesDataStore("user_prefs")

    @Provides
    @Singleton
    fun provideRoomDatabase(
        @ApplicationContext context: Context,
        masterKey: MasterKey
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "resume_db"
    )
        .fallbackToDestructiveMigration() // Only for dev, use proper migrations in prod
        .build()

    @Provides
    @Singleton
    fun provideHiltWorkerFactory(): HiltWorkerFactory = HiltWorkerFactory()

    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(): Configuration = Configuration.Builder()
        .setWorkerFactory(HiltWorkerFactory())
        .build()
}
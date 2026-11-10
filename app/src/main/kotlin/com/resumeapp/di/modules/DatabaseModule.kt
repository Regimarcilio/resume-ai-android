package com.resumeapp.di.modules

import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.db.ResumeDao
import com.resumeapp.data.db.TemplateDao
import com.resumeapp.data.db.ATSScoreDao
import com.resumeapp.data.db.AICacheDao
import com.resumeapp.data.db.TraceDao
import dagger.hilt.android.InstallIn
import dagger.hilt.android.components.SingletonComponent
import dagger.hilt.android.scopes.Singleton
import dagger.Module
import dagger.Provides

/**
 * Database Module - Room DAOs
 */
@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideResumeDao(database: AppDatabase): ResumeDao = database.resumeDao()

    @Provides
    @Singleton
    fun provideTemplateDao(database: AppDatabase): TemplateDao = database.templateDao()

    @Provides
    @Singleton
    fun provideATSScoreDao(database: AppDatabase): ATSScoreDao = database.atsScoreDao()

    @Provides
    @Singleton
    fun provideAICacheDao(database: AppDatabase): AICacheDao = database.aiCacheDao()

    @Provides
    @Singleton
    fun provideTraceDao(database: AppDatabase): TraceDao = database.traceDao()
}
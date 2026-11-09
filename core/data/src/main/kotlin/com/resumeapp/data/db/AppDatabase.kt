package com.resumeapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.resumeapp.data.db.converters.Converters

@Database(
    entities = [
        ResumeEntity::class,
        ResumeSectionEntity::class,
        TemplateEntity::class,
        ATSScoreEntity::class,
        AICacheEntity::class,
        TraceEntity::class,
        SubscriptionStatusEntity::class,
        PdfCacheEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun resumeDao(): ResumeDao
    abstract fun resumeSectionDao(): ResumeSectionDao
    abstract fun templateDao(): TemplateDao
    abstract fun atsScoreDao(): ATSScoreDao
    abstract fun aiCacheDao(): AICacheDao
    abstract fun traceDao(): TraceDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun pdfCacheDao(): PdfCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, factory: androidx.sqlite.db.SupportSQLiteOpenHelper.Factory): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "resume_db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration() // Only for dev - use proper migrations in prod
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
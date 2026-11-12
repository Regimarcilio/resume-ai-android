package com.resumeapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.resumeapp.data.db.converters.Converters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        /**
         * Templates semeados na criacao do banco.
         * Sem isso a galeria de templates abre vazia.
         */
        val DEFAULT_TEMPLATES = listOf(
            TemplateEntity(
                id = "modern_professional",
                name = "Moderno Profissional",
                description = "Layout limpo, otimizado para ATS, ideal para areas de tecnologia.",
                category = "MODERN",
                previewImageUrl = "",
                configJson = "{}",
                isPremium = false,
                isActive = true
            ),
            TemplateEntity(
                id = "classic",
                name = "Classico",
                description = "Formatacao tradicional, adequada para areas conservadoras.",
                category = "CLASSIC",
                previewImageUrl = "",
                configJson = "{}",
                isPremium = false,
                isActive = true
            ),
            TemplateEntity(
                id = "creative",
                name = "Criativo",
                description = "Destaques visuais para areas de design e marketing.",
                category = "CREATIVE",
                previewImageUrl = "",
                configJson = "{}",
                isPremium = true,
                isActive = true
            ),
            TemplateEntity(
                id = "ats_optimized",
                name = "Otimizado ATS",
                description = "Estrutura recomendada por sistemas de triagem automatica.",
                category = "ATS_OPTIMIZED",
                previewImageUrl = "",
                configJson = "{}",
                isPremium = false,
                isActive = true
            )
        )

        /**
         * Semeia os templates padrao na criacao do banco.
         * [dbProvider] resolve a instancia do banco: o callback `onCreate` roda
         * na primeira abertura (depois do build), quando a instancia ja existe.
         */
        fun seedCallback(dbProvider: () -> AppDatabase): RoomDatabase.Callback =
            object : RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        dbProvider().templateDao().insertAll(DEFAULT_TEMPLATES)
                    }
                }
            }

        fun getInstance(context: Context, factory: androidx.sqlite.db.SupportSQLiteOpenHelper.Factory): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                lateinit var instance: AppDatabase
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "resume_db"
                )
                    .openHelperFactory(factory)
                    .addCallback(seedCallback { instance })
                    .fallbackToDestructiveMigration() // Only for dev - use proper migrations in prod
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
package com.resumeapp.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.db.converters.Converters
import com.resumeapp.data.db.resumeDao
import com.resumeapp.data.prefs.UserPrefsImpl
import com.resumeapp.data.repository.ResumeRepositoryImpl
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.SectionType
import com.resumeapp.domain.model.ResumeSection
import com.resumeapp.domain.model.SectionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ResumeRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var resumeDao: com.resumeapp.data.db.ResumeDao
    private lateinit var repository: ResumeRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        resumeDao = database.resumeDao()
        
        // Use a test UserPrefs implementation
        val userPrefs = TestUserPrefs()
        repository = ResumeRepositoryImpl(resumeDao, userPrefs)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun createAndGetResume() = runBlocking(Dispatchers.Main) {
        val resume = Resume(
            userId = "test-user",
            templateId = "modern",
            personalInfo = com.resumeapp.domain.model.PersonalInfo(
                fullName = "João Silva",
                email = "joao@test.com"
            )
        )

        val created = repository.createResume(resume)
        assert(created.isSuccess)

        val fetched = repository.getResume(created.getOrThrow().id)
        assert(fetched.isSuccess)
        assert(fetched.getOrNull()?.personalInfo?.fullName == "João Silva")
    }

    @Test
    fun updateResume() = runBlocking(Dispatchers.Main) {
        val resume = Resume(
            userId = "test-user",
            templateId = "modern",
            personalInfo = com.resumeapp.domain.model.PersonalInfo(
                fullName = "João Silva",
                email = "joao@test.com"
            )
        )

        val created = repository.createResume(resume).getOrThrow()
        
        val updated = created.copyWithUpdatedSections(listOf(
            ResumeSection(
                resumeId = created.id,
                type = SectionType.EXPERIENCE,
                title = "Experience",
                order = 0,
                itemsJson = Json.encodeToString(listOf(
                    SectionItem(
                        title = "Software Engineer",
                        subtitle = "Tech Corp",
                        highlights = listOf("Kotlin", "Spring Boot")
                    )
                ))
            )
        ))

        val result = repository.updateResume(updated)
        assert(result.isSuccess)

        val fetched = repository.getResume(created.id).getOrNull()
        assert(fetched?.sections?.size == 1)
        assert(fetched?.sections?.first()?.type == SectionType.EXPERIENCE)
    }

    @Test
    fun deleteResume() = runBlocking(Dispatchers.Main) {
        val resume = Resume(
            userId = "test-user",
            templateId = "modern"
        )

        val created = repository.createResume(resume).getOrThrow()
        val deleted = repository.deleteResume(created.id)
        assert(deleted.isSuccess)

        val fetched = repository.getResume(created.id)
        assert(fetched.isFailure)
    }

    @Test
    fun listResumes() = runBlocking(Dispatchers.Main) {
        // Create multiple resumes
        repeat(3) { i ->
            val resume = Resume(
                userId = "test-user",
                templateId = "modern",
                personalInfo = com.resumeapp.domain.model.PersonalInfo(fullName = "Resume $i")
            )
            repository.createResume(resume)
        }

        val result = repository.getResumes("test-user")
        assert(result.isSuccess)
        assert(result.getOrNull()?.size == 3)
    }

    // Test UserPrefs implementation for testing
    private class TestUserPrefs : com.resumeapp.data.prefs.UserPrefs {
        override val consentFlow = kotlinx.coroutines.flow.flowOf(false)
        override val subscriptionStatusFlow = kotlinx.coroutines.flow.flowOf(com.resumeapp.domain.model.SubscriptionStatus())
        override val analyticsEnabledFlow = kotlinx.coroutines.flow.flowOf(false)
        override val crashReportingEnabledFlow = kotlinx.coroutines.flow.flowOf(true)
        override val aiEnabledFlow = kotlinx.coroutines.flow.flowOf(true)
        
        override suspend fun setConsentGiven(accepted: Boolean, version: String, policyHash: String) {}
        override suspend fun setAnalyticsEnabled(enabled: Boolean) {}
        override suspend fun setCrashReportingEnabled(enabled: Boolean) {}
        override suspend fun setAiEnabled(enabled: Boolean) {}
        override suspend fun updateSubscription(tier: com.resumeapp.domain.model.SubscriptionTier, expiryDate: Long?, purchaseToken: String?) {}
    }
}
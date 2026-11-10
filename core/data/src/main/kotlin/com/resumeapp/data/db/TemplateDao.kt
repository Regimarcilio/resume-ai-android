package com.resumeapp.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<TemplateEntity>)

    @Update
    suspend fun update(template: TemplateEntity): Int

    @Delete
    suspend fun delete(template: TemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getById(id: String): TemplateEntity?

    @Query("SELECT * FROM templates WHERE isActive = 1 ORDER BY category, name")
    suspend fun getAllActive(): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE isActive = 1 ORDER BY category, name")
    fun observeAllActive(): LiveData<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE category = :category AND isActive = 1 ORDER BY name")
    suspend fun getByCategory(category: String): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE isPremium = 0 AND isActive = 1 ORDER BY name")
    suspend fun getFreeTemplates(): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE isPremium = 1 AND isActive = 1 ORDER BY name")
    suspend fun getPremiumTemplates(): List<TemplateEntity>

    @Query("SELECT COUNT(*) FROM templates WHERE isActive = 1")
    suspend fun countActive(): Int
}

@Dao
interface ATSScoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: ATSScoreEntity): Long

    @Update
    suspend fun update(score: ATSScoreEntity): Int

    @Delete
    suspend fun delete(score: ATSScoreEntity)

    @Query("DELETE FROM ats_scores WHERE resumeId = :resumeId")
    suspend fun deleteByResumeId(resumeId: String): Int

    @Query("SELECT * FROM ats_scores WHERE resumeId = :resumeId")
    suspend fun getByResumeId(resumeId: String): ATSScoreEntity?

    @Query("SELECT * FROM ats_scores WHERE resumeId = :resumeId")
    fun observeByResumeId(resumeId: String): LiveData<ATSScoreEntity?>

    @Query("SELECT * FROM ats_scores WHERE overallScore >= :minScore")
    suspend fun getScoresAbove(minScore: Int): List<ATSScoreEntity>

    @Query("SELECT * FROM ats_scores WHERE resumeId IN (SELECT id FROM resumes WHERE userId = :userId)")
    suspend fun getAllByUserId(userId: String): List<ATSScoreEntity>
}
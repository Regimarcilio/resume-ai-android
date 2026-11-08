package com.resumeapp.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.resumeapp.domain.model.*

@Dao
interface TemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: Template): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<Template>)

    @Update
    suspend fun update(template: Template): Int

    @Delete
    suspend fun delete(template: Template)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getById(id: String): Template?

    @Query("SELECT * FROM templates WHERE isActive = 1 ORDER BY category, name")
    suspend fun getAllActive(): List<Template>

    @Query("SELECT * FROM templates WHERE isActive = 1 ORDER BY category, name")
    fun observeAllActive(): LiveData<List<Template>>

    @Query("SELECT * FROM templates WHERE category = :category AND isActive = 1 ORDER BY name")
    suspend fun getByCategory(category: com.resumeapp.domain.model.TemplateCategory): List<Template>

    @Query("SELECT * FROM templates WHERE isPremium = 0 AND isActive = 1 ORDER BY name")
    suspend fun getFreeTemplates(): List<Template>

    @Query("SELECT * FROM templates WHERE isPremium = 1 AND isActive = 1 ORDER BY name")
    suspend fun getPremiumTemplates(): List<Template>

    @Query("SELECT COUNT(*) FROM templates WHERE isActive = 1")
    suspend fun countActive(): Int
}

@Dao
interface ATSScoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: ATSScore): Long

    @Update
    suspend fun update(score: ATSScore): Int

    @Delete
    suspend fun delete(score: ATSScore)

    @Query("DELETE FROM ats_scores WHERE resumeId = :resumeId")
    suspend fun deleteByResumeId(resumeId: String): Int

    @Query("SELECT * FROM ats_scores WHERE resumeId = :resumeId")
    suspend fun getByResumeId(resumeId: String): ATSScore?

    @Query("SELECT * FROM ats_scores WHERE resumeId = :resumeId")
    fun observeByResumeId(resumeId: String): LiveData<ATSScore?>

    @Query("SELECT * FROM ats_scores WHERE overallScore >= :minScore")
    suspend fun getScoresAbove(minScore: Int): List<ATSScore>
}
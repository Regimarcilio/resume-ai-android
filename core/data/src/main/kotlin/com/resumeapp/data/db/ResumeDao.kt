package com.resumeapp.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ResumeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resume: ResumeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(resumes: List<ResumeEntity>)

    @Update
    suspend fun update(resume: ResumeEntity): Int

    @Delete
    suspend fun delete(resume: ResumeEntity)

    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM resumes")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM resumes WHERE id = :id")
    suspend fun getById(id: String): ResumeEntity?

    @Query("SELECT * FROM resumes WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getAllByUserId(userId: String): List<ResumeEntity>

    @Query("SELECT * FROM resumes WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observeAllByUserId(userId: String): LiveData<List<ResumeEntity>>

    @Query("SELECT * FROM resumes WHERE userId = :userId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestByUserId(userId: String): ResumeEntity?

    @Query("SELECT COUNT(*) FROM resumes WHERE userId = :userId")
    suspend fun countByUserId(userId: String): Int

    @Query("SELECT * FROM resumes WHERE templateId = :templateId")
    suspend fun getByTemplateId(templateId: String): List<ResumeEntity>

    @Query("SELECT * FROM resumes WHERE userId = :userId AND (title LIKE :query OR summary LIKE :query) ORDER BY updatedAt DESC")
    suspend fun search(userId: String, query: String, templateId: String?): List<ResumeEntity>
}

@Dao
interface ResumeSectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(section: ResumeSectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sections: List<ResumeSectionEntity>)

    @Update
    suspend fun update(section: ResumeSectionEntity): Int

    @Delete
    suspend fun delete(section: ResumeSectionEntity)

    @Query("DELETE FROM resume_sections WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM resume_sections WHERE resumeId = :resumeId")
    suspend fun deleteByResumeId(resumeId: String): Int

    @Query("SELECT * FROM resume_sections WHERE resumeId = :resumeId ORDER BY `order` ASC")
    suspend fun getByResumeId(resumeId: String): List<ResumeSectionEntity>

    @Query("SELECT * FROM resume_sections WHERE resumeId = :resumeId ORDER BY `order` ASC")
    fun observeByResumeId(resumeId: String): LiveData<List<ResumeSectionEntity>>

    @Query("SELECT * FROM resume_sections WHERE id = :id")
    suspend fun getById(id: String): ResumeSectionEntity?

    @Query("UPDATE resume_sections SET `order` = :newOrder WHERE id = :id")
    suspend fun updateOrder(id: String, newOrder: Int): Int

    @Query("UPDATE resume_sections SET isVisible = :visible WHERE id = :id")
    suspend fun setVisibility(id: String, visible: Boolean): Int
}
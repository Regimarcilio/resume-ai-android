package com.resumeapp.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.ResumeSection

@Dao
interface ResumeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resume: Resume): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(resumes: List<Resume>)

    @Update
    suspend fun update(resume: Resume): Int

    @Delete
    suspend fun delete(resume: Resume)

    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM resumes")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM resumes WHERE id = :id")
    suspend fun getById(id: String): Resume?

    @Query("SELECT * FROM resumes WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getAllByUserId(userId: String): List<Resume>

    @Query("SELECT * FROM resumes WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observeAllByUserId(userId: String): LiveData<List<Resume>>

    @Query("SELECT * FROM resumes WHERE userId = :userId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestByUserId(userId: String): Resume?

    @Query("SELECT COUNT(*) FROM resumes WHERE userId = :userId")
    suspend fun countByUserId(userId: String): Int

    @Query("SELECT * FROM resumes WHERE templateId = :templateId")
    suspend fun getByTemplateId(templateId: String): List<Resume>

    // Sections are stored as separate entities with resumeId FK
    // Use ResumeSectionDao for section operations
}

@Dao
interface ResumeSectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(section: ResumeSection): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sections: List<ResumeSection>)

    @Update
    suspend fun update(section: ResumeSection): Int

    @Delete
    suspend fun delete(section: ResumeSection)

    @Query("DELETE FROM resume_sections WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM resume_sections WHERE resumeId = :resumeId")
    suspend fun deleteByResumeId(resumeId: String): Int

    @Query("SELECT * FROM resume_sections WHERE resumeId = :resumeId ORDER BY `order` ASC")
    suspend fun getByResumeId(resumeId: String): List<ResumeSection>

    @Query("SELECT * FROM resume_sections WHERE resumeId = :resumeId ORDER BY `order` ASC")
    fun observeByResumeId(resumeId: String): androidx.lifecycle.LiveData<List<ResumeSection>>

    @Query("SELECT * FROM resume_sections WHERE id = :id")
    suspend fun getById(id: String): ResumeSection?

    @Query("UPDATE resume_sections SET `order` = :newOrder WHERE id = :id")
    suspend fun updateOrder(id: String, newOrder: Int): Int

    @Query("UPDATE resume_sections SET isVisible = :visible WHERE id = :id")
    suspend fun setVisibility(id: String, visible: Boolean): Int
}
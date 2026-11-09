package com.resumeapp.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AICacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AICacheEntity): Long

    @Update
    suspend fun update(entry: AICacheEntity): Int

    @Delete
    suspend fun delete(entry: AICacheEntity)

    @Query("DELETE FROM ai_cache WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM ai_cache WHERE createdAt < :cutoff")
    suspend fun deleteOldEntries(cutoff: Long): Int

    @Query("DELETE FROM ai_cache")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM ai_cache WHERE promptHash = :promptHash AND promptType = :promptType")
    suspend fun getByPromptHash(promptHash: String, promptType: String): AICacheEntity?

    @Query("SELECT * FROM ai_cache WHERE promptType = :promptType ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getByPromptType(promptType: String, limit: Int): List<AICacheEntity>

    @Query("SELECT * FROM ai_cache ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<AICacheEntity>

    // For semantic similarity search (requires custom function or post-processing)
    @RawQuery(observedEntities = [AICacheEntity::class])
    fun findSimilar(query: androidx.room.RawQuery): AICacheEntity?

    @Query("SELECT COUNT(*) FROM ai_cache")
    suspend fun count(): Int
}

@Dao
interface TraceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TraceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TraceEntity>)

    @Query("DELETE FROM traces WHERE timestamp < :cutoff")
    suspend fun deleteOldEntries(cutoff: String): Int

    @Query("DELETE FROM traces")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM traces WHERE promptType = :promptType ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getByPromptType(promptType: String, limit: Int): List<TraceEntity>

    @Query("SELECT * FROM traces ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<TraceEntity>

    @Query("SELECT COUNT(*) FROM traces WHERE cacheHit = 1")
    suspend fun countCacheHits(): Int

    @Query("SELECT COUNT(*) FROM traces WHERE cacheHit = 0")
    suspend fun countCacheMisses(): Int

    @Query("SELECT AVG(latencyMs) FROM traces WHERE cacheHit = 0")
    suspend fun getAvgLatencyCacheMiss(): Double?

    @Query("SELECT SUM(costUsd) FROM traces WHERE date(timestamp/1000, 'unixepoch') = date('now')")
    suspend fun getTodayCostUsd(): Double?

    @Query("SELECT COUNT(*) FROM traces")
    suspend fun count(): Int
}

@Dao
interface SubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(status: SubscriptionStatusEntity): Long

    @Update
    suspend fun update(status: SubscriptionStatusEntity): Int

    @Delete
    suspend fun delete(status: SubscriptionStatusEntity)

    @Query("SELECT * FROM subscription_status ORDER BY tier DESC LIMIT 1")
    suspend fun getCurrent(): SubscriptionStatusEntity?

    @Query("SELECT * FROM subscription_status ORDER BY tier DESC LIMIT 1")
    fun observeCurrent(): LiveData<SubscriptionStatusEntity?>

    @Query("DELETE FROM subscription_status")
    suspend fun clear(): Int
}

@Dao
interface PdfCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PdfCacheEntity): Long

    @Query("SELECT * FROM pdf_cache WHERE resumeId = :resumeId AND template = :template AND watermark = :watermark")
    suspend fun getCached(resumeId: String, template: String, watermark: Boolean): PdfCacheEntity?

    @Query("DELETE FROM pdf_cache WHERE createdAt < :cutoff")
    suspend fun deleteOldEntries(cutoff: Long): Int

    @Query("DELETE FROM pdf_cache")
    suspend fun deleteAll(): Int
}
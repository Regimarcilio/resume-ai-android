package com.resumeapp.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.resumeapp.domain.model.*

@Dao
interface AICacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AICacheEntry): Long

    @Update
    suspend fun update(entry: AICacheEntry): Int

    @Delete
    suspend fun delete(entry: AICacheEntry)

    @Query("DELETE FROM ai_cache WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM ai_cache WHERE createdAt < :cutoff")
    suspend fun deleteOldEntries(cutoff: Long): Int

    @Query("DELETE FROM ai_cache")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM ai_cache WHERE promptHash = :promptHash AND promptType = :promptType")
    suspend fun getByPromptHash(promptHash: String, promptType: String): AICacheEntry?

    @Query("SELECT * FROM ai_cache WHERE promptType = :promptType ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getByPromptType(promptType: String, limit: Int): List<AICacheEntry>

    @Query("SELECT * FROM ai_cache ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<AICacheEntry>

    // For semantic similarity search (requires custom function or post-processing)
    @RawQuery(observedEntities = [AICacheEntry::class])
    fun findSimilar(query: androidx.room.RawQuery): AICacheEntry?

    @Query("SELECT COUNT(*) FROM ai_cache")
    suspend fun count(): Int
}

@Dao
interface TraceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TraceEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TraceEntry>)

    @Query("DELETE FROM traces WHERE createdAt < :cutoff")
    suspend fun deleteOldEntries(cutoff: Long): Int

    @Query("DELETE FROM traces")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM traces WHERE promptType = :promptType ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getByPromptType(promptType: String, limit: Int): List<TraceEntry>

    @Query("SELECT * FROM traces ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<TraceEntry>

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
    suspend fun insert(status: SubscriptionStatus): Long

    @Update
    suspend fun update(status: SubscriptionStatus): Int

    @Delete
    suspend fun delete(status: SubscriptionStatus)

    @Query("SELECT * FROM subscription_status ORDER BY tier DESC LIMIT 1")
    suspend fun getCurrent(): SubscriptionStatus?

    @Query("SELECT * FROM subscription_status ORDER BY tier DESC LIMIT 1")
    fun observeCurrent(): LiveData<SubscriptionStatus?>

    @Query("DELETE FROM subscription_status")
    suspend fun clear(): Int
}
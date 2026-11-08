package com.resumeapp.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.resumeapp.data.db.AppDatabase
import com.resumeapp.pdf.repository.PdfCacheDao
import com.resumeapp.pdf.repository.PdfCacheEntry
import com.resumeapp.pdf.repository.RoomPdfCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfCacheTest {

    private lateinit var database: AppDatabase
    private lateinit var pdfCacheDao: PdfCacheDao
    private lateinit var pdfCache: RoomPdfCache

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pdfCacheDao = database.pdfCacheDao()
        pdfCache = RoomPdfCache(pdfCacheDao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun putAndGetCache() = runBlocking(Dispatchers.Main) {
        val key = "test_key_123"
        val pdfBytes = "PDF content".toByteArray()

        pdfCache.put(key, pdfBytes)
        
        val retrieved = pdfCache.get(key)
        assertNotNull(retrieved)
        assert(retrieved?.contentEquals(pdfBytes) == true)
    }

    @Test
    fun cacheMissReturnsNull() = runBlocking(Dispatchers.Main) {
        val retrieved = pdfCache.get("non_existent_key")
        assertNull(retrieved)
    }

    @Test
    fun clearCache() = runBlocking(Dispatchers.Main) {
        val key = "test_key_456"
        val pdfBytes = "PDF content".toByteArray()

        pdfCache.put(key, pdfBytes)
        pdfCache.clear()
        
        val retrieved = pdfCache.get(key)
        assertNull(retrieved)
    }

    @Test
    fun expiredCacheEntryReturnsNull() = runBlocking(Dispatchers.Main) {
        val key = "expired_key"
        val pdfBytes = "PDF content".toByteArray()
        val expiredEntry = PdfCacheEntry(
            cacheKey = key,
            pdfBytes = pdfBytes,
            createdAt = System.currentTimeMillis() - 1000,
            expiresAt = System.currentTimeMillis() - 100 // Already expired
        )

        pdfCacheDao.insert(expiredEntry)
        
        val retrieved = pdfCache.get(key)
        assertNull(retrieved)
    }
}
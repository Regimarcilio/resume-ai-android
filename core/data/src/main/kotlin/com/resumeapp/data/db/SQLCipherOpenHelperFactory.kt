package com.resumeapp.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper

/**
 * Standard Room OpenHelperFactory (no SQLCipher for CI compatibility)
 * SQLCipher can be added later when build environment supports it
 */
class SQLCipherOpenHelperFactory(
    private val passphrase: String
) : RoomDatabase.OpenHelperFactory() {

    override fun create(configuration: RoomDatabase.Configuration): SupportSQLiteOpenHelper {
        return configuration.callback?.let { 
            // Use default Room factory
            androidx.room.RoomDatabase.OpenHelperFactory().create(configuration)
        } ?: throw IllegalStateException("Room configuration callback is null")
    }
}
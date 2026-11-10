package com.resumeapp.data.db

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper

/**
 * SQLCipher OpenHelperFactory for Room
 * Provides encrypted SQLite database using Zetetic SQLCipher
 */
class SQLCipherOpenHelperFactory(
    private val passphrase: String
) : RoomDatabase.OpenHelperFactory() {

    override fun create(configuration: RoomDatabase.Configuration): SupportSQLiteOpenHelper {
        return SQLiteOpenHelper(
            configuration.context,
            configuration.name,
            null,
            configuration.version,
            object : SQLiteOpenHelper.ConfigurationBuilder()
                .passphrase(passphrase)
                .build()
        )
    }
}
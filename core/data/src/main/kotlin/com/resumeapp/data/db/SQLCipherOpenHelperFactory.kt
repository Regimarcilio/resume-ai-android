package com.resumeapp.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SQLiteOpenHelper

/**
 * SQLCipher OpenHelperFactory for Room
 * Provides encrypted SQLite database using Zetetic SQLCipher
 */
class SQLCipherOpenHelperFactory(
    private val passphrase: String
) : RoomDatabase.OpenHelperFactory() {

    override fun create(configuration: RoomDatabase.Configuration): SupportSQLiteOpenHelper {
        val builder = SQLiteOpenHelper.ConfigurationBuilder()
            .passphrase(passphrase)
        return SQLiteOpenHelper(
            configuration.context,
            configuration.name,
            null,
            configuration.version,
            builder.build()
        )
    }
}
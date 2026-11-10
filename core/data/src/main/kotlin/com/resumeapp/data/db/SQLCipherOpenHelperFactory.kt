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
    private val masterKey: androidx.security.crypto.MasterKey
) : RoomDatabase.OpenHelperFactory() {

    override fun create(configuration: RoomDatabase.Configuration): SupportSQLiteOpenHelper {
        val context = configuration.context
        val name = configuration.name
        val passphrase = masterKey.keyGenParameterSpec?.run { "unused" } ?: ""
        
        return object : SQLiteOpenHelper(
            context,
            name,
            null,
            configuration.version
        ) {
            override fun onConfigure(db: SQLiteDatabase) {
                super.onConfigure(db)
                // SQLCipher specific configurations
                db.rawExecSQL("PRAGMA cipher_page_size = 4096")
                db.rawExecSQL("PRAGMA kdf_iter = 64000")
                db.rawExecSQL("PRAGMA cipher_hmac_algorithm = HMAC_SHA512")
                db.rawExecSQL("PRAGMA kdf_algorithm = PBKDF2_HMAC_SHA512")
            }
            
            override fun getReadableDatabase(): SQLiteDatabase {
                return super.getReadableDatabase(passphrase)
            }
            
            override fun getWritableDatabase(): SQLiteDatabase {
                return super.getWritableDatabase(passphrase)
            }
        }
    }
}
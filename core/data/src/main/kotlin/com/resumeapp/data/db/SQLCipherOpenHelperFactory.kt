package com.resumeapp.data.db

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper

class SQLCipherOpenHelperFactory(
    private val masterKey: androidx.security.crypto.MasterKey
) : RoomDatabase.OpenHelperFactory() {

    override fun create(configuration: RoomDatabase.Configuration): SupportSQLiteOpenHelper {
        val key = masterKey.keyGenParameterSpec?.run { "unused" } ?: ""
        
        return object : SupportSQLiteOpenHelper.Configuration(configuration) {
            override fun createOpenHelper(): SupportSQLiteOpenHelper {
                return object : SupportSQLiteOpenHelper(this) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Room handles schema creation
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        // Handle migrations
                    }

                    override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        // Handle downgrades
                    }

                    override fun onConfigure(db: SupportSQLiteDatabase) {
                        super.onConfigure(db)
                        if (db is SQLiteDatabase) {
                            // SQLCipher specific configurations
                            db.rawExecSQL("PRAGMA cipher_page_size = 4096")
                            db.rawExecSQL("PRAGMA kdf_iter = 64000")
                            db.rawExecSQL("PRAGMA cipher_hmac_algorithm = HMAC_SHA512")
                            db.rawExecSQL("PRAGMA kdf_algorithm = PBKDF2_HMAC_SHA512")
                        }
                    }
                }
            }
        }.createOpenHelper()
    }
}
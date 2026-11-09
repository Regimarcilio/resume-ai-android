package com.resumeapp.data.db

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SQLCipherDatabase

class SQLCipherOpenHelperFactory(
    private val masterKey: androidx.security.crypto.MasterKey
) : RoomDatabase.OpenHelperFactory() {

    override fun create(
        configuration: RoomDatabase.Configuration
    ): SupportSQLiteOpenHelper {
        return object : SupportSQLiteOpenHelper.Configuration(configuration) {
            override fun createOpenHelper(): SupportSQLiteOpenHelper {
                return object : SupportSQLiteOpenHelper(this) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Room will call this when creating the database
                        // SQLCipher will handle encryption automatically
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        // Handle migrations here if needed
                    }

                    override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        // Handle downgrades if needed
                    }

                    override fun onConfigure(db: SupportSQLiteDatabase) {
                        super.onConfigure(db)
                        // SQLCipher specific configurations
                        if (db is net.zetetic.database.sqlcipher.SQLCipherDatabase) {
                            // Set cipher page size for better performance
                            db.rawExecSQL("PRAGMA cipher_page_size = 4096")
                            // Set KDF iterations for security vs performance balance
                            db.rawExecSQL("PRAGMA kdf_iter = 64000")
                            // Use HMAC for integrity
                            db.rawExecSQL("PRAGMA cipher_hmac_algorithm = HMAC_SHA512")
                            // Use PBKDF2 for key derivation
                            db.rawExecSQL("PRAGMA kdf_algorithm = PBKDF2_HMAC_SHA512")
                        }
                    }
                }.createOpenHelper()
            }
        }
    }
}
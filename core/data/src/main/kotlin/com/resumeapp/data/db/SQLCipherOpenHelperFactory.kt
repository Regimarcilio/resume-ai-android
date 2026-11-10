package com.resumeapp.data.db

/**
 * No-op OpenHelperFactory - uses Room's default
 * SQLCipher can be added later when build environment supports it
 */
class SQLCipherOpenHelperFactory(
    private val passphrase: String
)
package com.resumeapp.data.db.converters

import androidx.room.TypeConverter
import java.util.UUID

/**
 * Minimal Converters - only for types used directly in Room entities
 * Cross-module types (SectionItem, FeatureFlag) are handled as JSON Strings
 * and converted at the repository layer using Gson
 */
class Converters {

    // ===== UUID =====
    @TypeConverter
    fun fromUuidString(uuid: String): UUID = UUID.fromString(uuid)

    @TypeConverter
    fun toUuidString(uuid: UUID): String = uuid.toString()
}
package com.resumeapp.data.db.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.resumeapp.domain.model.SectionItem
import com.resumeapp.domain.model.FeatureFlag
import java.lang.reflect.Type
import java.util.UUID

class Converters {

    private val gson = Gson()
    private val sectionItemsType: Type = object : TypeToken<List<SectionItem>>() {}.type
    private val featureFlagSetType: Type = object : TypeToken<Set<FeatureFlag>>() {}.type

    // ===== ResumeSectionEntity.itemsJson =====
    @TypeConverter
    fun fromSectionItemsJson(json: String): List<SectionItem> {
        return gson.fromJson(json, sectionItemsType)
    }

    @TypeConverter
    fun toSectionItemsJson(items: List<SectionItem>): String {
        return gson.toJson(items, sectionItemsType)
    }

    // ===== SubscriptionStatusEntity.featuresJson =====
    @TypeConverter
    fun fromFeatureFlagSetJson(json: String): Set<FeatureFlag> {
        return gson.fromJson(json, featureFlagSetType)
    }

    @TypeConverter
    fun toFeatureFlagSetJson(set: Set<FeatureFlag>): String {
        return gson.toJson(set, featureFlagSetType)
    }

    // ===== UUID =====
    @TypeConverter
    fun fromUuidString(uuid: String): UUID = UUID.fromString(uuid)

    @TypeConverter
    fun toUuidString(uuid: UUID): String = uuid.toString()
}
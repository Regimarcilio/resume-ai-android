package com.resumeapp.data.db.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.resumeapp.domain.model.*
import java.lang.reflect.Type
import java.util.UUID

class Converters {

    private val gson = Gson()
    private val sectionItemsType: Type = object : TypeToken<List<SectionItem>>() {}.type
    private val stringListType: Type = object : TypeToken<List<String>>() {}.type
    private val floatArrayType: Type = object : TypeToken<FloatArray>() {}.type
    private val suggestionListType: Type = object : TypeToken<List<ATSSuggestion>>() {}.type
    private val consentPurposeSetType: Type = object : TypeToken<Set<ConsentPurpose>>() {}.type
    private val featureFlagSetType: Type = object : TypeToken<Set<FeatureFlag>>() {}.type

    // ===== ResumeSection.itemsJson =====
    @TypeConverter
    fun fromSectionItemsJson(json: String): List<SectionItem> {
        return gson.fromJson(json, sectionItemsType)
    }

    @TypeConverter
    fun toSectionItemsJson(items: List<SectionItem>): String {
        return gson.toJson(items, sectionItemsType)
    }

    // ===== SectionItem.highlights & keywords =====
    @TypeConverter
    fun fromStringListJson(json: String): List<String> {
        return gson.fromJson(json, stringListType)
    }

    @TypeConverter
    fun toStringListJson(list: List<String>): String {
        return gson.toJson(list, stringListType)
    }

    // ===== AICacheEntry.embeddingJson =====
    @TypeConverter
    fun fromFloatArrayJson(json: String): FloatArray {
        val list = gson.fromJson(json, floatArrayType)
        return list.toFloatArray()
    }

    @TypeConverter
    fun toFloatArrayJson(array: FloatArray): String {
        return gson.toJson(array.toList(), floatArrayType)
    }

    // ===== ATSScore.missingKeywordsJson & suggestionsJson =====
    @TypeConverter
    fun fromSuggestionsJson(json: String): List<ATSSuggestion> {
        return gson.fromJson(json, suggestionListType)
    }

    @TypeConverter
    fun toSuggestionsJson(suggestions: List<ATSSuggestion>): String {
        return gson.toJson(suggestions, suggestionListType)
    }

    // ===== ConsentRecord.purposes =====
    @TypeConverter
    fun fromConsentPurposeSetJson(json: String): Set<ConsentPurpose> {
        return gson.fromJson(json, consentPurposeSetType)
    }

    @TypeConverter
    fun toConsentPurposeSetJson(set: Set<ConsentPurpose>): String {
        return gson.toJson(set, consentPurposeSetType)
    }

    // ===== SubscriptionStatus.features =====
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
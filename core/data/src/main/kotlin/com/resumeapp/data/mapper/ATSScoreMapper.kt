package com.resumeapp.data.mapper

import com.resumeapp.domain.model.ATSScore
import com.resumeapp.domain.model.ATSSuggestion
import com.resumeapp.domain.model.SuggestionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ATSScoreMapper {

    private val gson = Gson()
    private val suggestionListType = object : TypeToken<List<ATSSuggestion>>() {}.type
    private val stringListType = object : TypeToken<List<String>>() {}.type

    fun toDomain(entity: com.resumeapp.data.db.ATSScoreEntity): ATSScore {
        val missingKeywords = gson.fromJson(
            entity.missingKeywordsJson,
            stringListType
        ) ?: emptyList()

        val suggestions = gson.fromJson(
            entity.suggestionsJson,
            TypeToken.getParameterized(List::class.java, ATSSuggestion::class.java).type
        ) ?: emptyList()

        return ATSScore(
            resumeId = entity.resumeId,
            overallScore = entity.overallScore,
            keywordMatch = entity.keywordMatch,
            formattingScore = entity.formattingScore,
            sectionCompleteness = entity.sectionCompleteness,
            missingKeywords = missingKeywords,
            suggestions = suggestions,
            analyzedAt = entity.analyzedAt
        )
    }

    fun toEntity(score: ATSScore): com.resumeapp.data.db.ATSScoreEntity {
        return com.resumeapp.data.db.ATSScoreEntity(
            resumeId = score.resumeId,
            overallScore = score.overallScore,
            keywordMatch = score.keywordMatch,
            formattingScore = score.formattingScore,
            sectionCompleteness = score.sectionCompleteness,
            missingKeywordsJson = com.google.gson.Gson().toJson(score.missingKeywords),
            suggestionsJson = com.google.gson.Gson().toJson(score.suggestions),
            analyzedAt = score.analyzedAt
        )
    }
}
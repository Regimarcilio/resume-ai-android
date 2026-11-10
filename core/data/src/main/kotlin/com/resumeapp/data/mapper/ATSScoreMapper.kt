package com.resumeapp.data.mapper

import com.resumeapp.domain.model.ATSScore
import com.resumeapp.domain.model.ATSSuggestion
import com.resumeapp.domain.model.SuggestionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class ATSScoreMapper {

    private val gson = Gson()
    private val suggestionListType: Type = object : TypeToken<List<ATSSuggestion>>() {}.type
    private val stringListType: Type = object : TypeToken<List<String>>() {}.type

    fun toDomain(entity: com.resumeapp.data.db.ATSScoreEntity): ATSScore {
        return ATSScore(
            resumeId = entity.resumeId,
            overallScore = entity.overallScore,
            keywordMatch = entity.keywordMatch,
            formattingScore = entity.formattingScore,
            sectionCompleteness = entity.sectionCompleteness,
            missingKeywordsJson = entity.missingKeywordsJson,
            suggestionsJson = entity.suggestionsJson,
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
            missingKeywordsJson = score.missingKeywordsJson,
            suggestionsJson = score.suggestionsJson,
            analyzedAt = score.analyzedAt
        )
    }
}
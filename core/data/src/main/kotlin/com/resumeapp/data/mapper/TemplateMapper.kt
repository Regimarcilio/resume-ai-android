package com.resumeapp.data.mapper

import com.resumeapp.domain.model.Template
import com.resumeapp.domain.model.TemplateCategory

class TemplateMapper {

    fun toDomain(entity: com.resumeapp.data.db.TemplateEntity): Template {
        return Template(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            category = TemplateCategory.valueOf(entity.category),
            previewImageUrl = entity.previewImageUrl,
            configJson = entity.configJson,
            isPremium = entity.isPremium,
            isActive = entity.isActive
        )
    }

    fun toEntity(template: Template): com.resumeapp.data.db.TemplateEntity {
        return com.resumeapp.data.db.TemplateEntity(
            id = template.id,
            name = template.name,
            description = template.description,
            category = template.category.name,
            previewImageUrl = template.previewImageUrl,
            configJson = template.configJson,
            isPremium = template.isPremium,
            isActive = template.isActive
        )
    }
}
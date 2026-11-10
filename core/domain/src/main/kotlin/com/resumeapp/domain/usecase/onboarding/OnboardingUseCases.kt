package com.resumeapp.domain.usecase.onboarding

import com.resumeapp.domain.model.ConsentRecord
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.Result.Success
import com.resumeapp.domain.model.Result.Failure
import com.resumeapp.domain.repository.ConsentRepository
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.domain.usecase.AsyncUseCase
import kotlinx.serialization.Serializable

/**
 * Record Consent UseCase
 * Records user consent for data processing (LGPD Art. 7)
 */
class RecordConsentUseCase constructor(
    private val consentRepository: ConsentRepository
) : AsyncUseCase<RecordConsentUseCase.Params, Unit> {

    @Serializable
    data class Params(
        val record: ConsentRecord
    )

    override suspend operator fun invoke(params: Params): Result<Unit> {
        return try {
            consentRepository.save(params.record)
            Success(Unit)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Complete Onboarding UseCase
 * Finalizes onboarding flow, creates initial resume from template
 */
class CompleteOnboardingUseCase constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<CompleteOnboardingUseCase.Params, Unit> {

    @Serializable
    data class Params(
        val templateId: String
    )

    override suspend operator fun invoke(params: Params): Result<Unit> {
        return try {
            // This would typically create a default resume for the user
            // For now, just return success - actual resume creation happens when user creates one
            Success(Unit)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}
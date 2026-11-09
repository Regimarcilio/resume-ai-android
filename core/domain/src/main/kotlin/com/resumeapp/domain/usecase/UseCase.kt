package com.resumeapp.domain.usecase

import com.resumeapp.domain.model.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Base UseCase interface - Single responsibility, testable, composable
 * 
 * All UseCases should:
 * - Be single-purpose (one business operation)
 * - Return Result<T> for explicit error handling
 * - Be suspendable for async operations
 * - Be stateless and thread-safe
 * - Be easily testable with mock repositories
 */
interface UseCase<in Params, out ResultType> {
    operator fun invoke(params: Params): Result<ResultType>
}

/**
 * Async UseCase for operations that need coroutine scope
 */
interface AsyncUseCase<in Params, out ResultType> {
    suspend operator fun invoke(params: Params): Result<ResultType>
}

/**
 * Base UseCase implementation with common functionality
 */
abstract class BaseUseCase<in Params, out ResultType>(protected val coroutineScope: CoroutineScope) : UseCase<Params, ResultType> {
    override abstract fun invoke(params: Params): Result<ResultType>
}

/**
 * No-params UseCase for operations without input
 */
interface NoParamsUseCase<out ResultType> {
    operator fun invoke(): Result<ResultType>
}

/**
 * UseCase with flow output for reactive streams
 */
interface FlowUseCase<in Params, out ResultType> {
    operator fun invoke(params: Params): Flow<Result<ResultType>>
}

/**
 * Common parameter objects
 */
@Serializable
data class NoParams(val placeholder: Unit = Unit)

@Serializable
data class IdParams(val id: String)

@Serializable
data class PaginationParams(
    val offset: Int = 0,
    val limit: Int = 20
)

@Serializable
data class SearchParams(
    val query: String = "",
    val filters: Map<String, String> = emptyMap(),
    val pagination: PaginationParams = PaginationParams()
)
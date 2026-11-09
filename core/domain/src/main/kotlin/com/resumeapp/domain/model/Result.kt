package com.resumeapp.domain.model

import kotlinx.serialization.Serializable

/**
 * Result wrapper for explicit error handling in UseCases
 * Replaces throwing exceptions for expected business logic errors
 */
sealed interface Result<out T> {
    /**
     * Successful result with value
     */
    @Serializable
    data class Success<out T>(val value: T) : Result<T>

    /**
     * Failure result with error message
     */
    @Serializable
    data class Failure(val errorMessage: String) : Result<Nothing>

    companion object {
        inline fun <T> success(value: T): Result<T> = Success(value)
        inline fun <T> failure(errorMessage: String): Result<T> = Failure(errorMessage)
        inline fun <T> failure(exception: Exception): Result<T> = Failure(exception.message ?: exception.javaClass.simpleName)
        inline fun <T> runCatching(block: () -> T): Result<T> =
            try { Success(block()) } catch (e: Exception) { Failure(e.message ?: e.javaClass.simpleName) }
    }
}
package com.resumeapp.domain.model

/**
 * Freemium rules of the product.
 *
 * Evaluation mode: the user gets **1 resume for free**. Creating a second one
 * requires the paid plan (R$ 4,99 via PayMaster).
 *
 * These constants live in the domain so the rule is enforced identically by
 * the use cases, the UI and the tests.
 */
object FreemiumLimits {

    /** Resumes allowed on the free tier. */
    const val FREE_RESUME_LIMIT = 1

    /** Price of the unlock plan, in cents (R$ 4,99). */
    const val UNLOCK_PRICE_CENTS = 499

    const val CURRENCY = "BRL"

    /** Id of the plan sold through PayMaster. */
    const val PLAN_ID_UNLOCK = "resume_ai_unlock"

    /** Product name shown to the user. */
    const val PLAN_TITLE = "Desbloquear currículos ilimitados"

    const val PLAN_DESCRIPTION =
        "Crie currículos ilimitados, exporte PDFs sem marca d'água e use todos os templates."

    /** Preco formatado no padrao pt-BR: "R$ 4,99" (nao "R$ 4.99"). */
    fun formatPrice(cents: Int = UNLOCK_PRICE_CENTS): String {
        val value = cents / 100.0
        val whole = cents / 100
        val centsPart = cents % 100
        return "R$ $whole,${centsPart.toString().padStart(2, '0')}"
    }
}

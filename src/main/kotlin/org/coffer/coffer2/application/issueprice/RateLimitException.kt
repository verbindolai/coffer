package org.coffer.coffer2.application.issueprice

/**
 * Exception thrown when Numista API rate limit is exceeded.
 * This exception is used to signal that batch processing should stop
 * and resume in the next scheduled run.
 */
class RateLimitException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

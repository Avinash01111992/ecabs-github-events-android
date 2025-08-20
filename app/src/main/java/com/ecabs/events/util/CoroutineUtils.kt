package com.ecabs.events.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Utility class for advanced coroutine operations
 */
object CoroutineUtils {

    /**
     * Launches a coroutine with debounced execution
     * 
     * @param scope Coroutine scope
     * @param delayMillis Delay in milliseconds
     * @param action Action to execute after delay
     * @return Job for cancellation
     */
    fun launchDebounced(
        scope: CoroutineScope,
        delayMillis: Long,
        action: suspend () -> Unit
    ): Job {
        return scope.launch {
            delay(delayMillis)
            action()
        }
    }

    /**
     * Creates a debounced function that delays execution
     * 
     * @param scope Coroutine scope
     * @param delayMillis Delay in milliseconds
     * @param action Action to execute
     * @return Debounced function
     */
    fun <T> createDebouncedFunction(
        scope: CoroutineScope,
        delayMillis: Long,
        action: suspend (T) -> Unit
    ): (T) -> Unit {
        var job: Job? = null
        return { value ->
            job?.cancel()
            job = scope.launch {
                delay(delayMillis)
                action(value)
            }
        }
    }

    /**
     * Retry mechanism with exponential backoff
     * 
     * @param maxAttempts Maximum number of retry attempts
     * @param initialDelay Initial delay in milliseconds
     * @param maxDelay Maximum delay in milliseconds
     * @param factor Backoff factor
     * @param action Action to retry
     * @return Result of the action
     */
    suspend fun <T> retryWithBackoff(
        maxAttempts: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 10000L,
        factor: Double = 2.0,
        action: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(maxAttempts) { attempt ->
            try {
                return action()
            } catch (e: Exception) {
                if (attempt == maxAttempts - 1) throw e
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        throw IllegalStateException("Retry failed after $maxAttempts attempts")
    }

    /**
     * Timeout wrapper for suspend functions
     * 
     * @param timeoutMillis Timeout in milliseconds
     * @param action Action to execute
     * @return Result of the action
     */
    suspend fun <T> withTimeout(
        timeoutMillis: Long,
        action: suspend () -> T
    ): T {
        return kotlinx.coroutines.withTimeout(timeoutMillis) {
            action()
        }
    }
}

package com.ecabs.events

import com.ecabs.events.util.CoroutineUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineUtilsTest {
    @Test
    fun `launchDebounced should delay execution`() = runTest {
        var executed = false
        
        val job = CoroutineUtils.launchDebounced(
            scope = this,
            delayMillis = 100L
        ) {
            executed = true
        }
        
        assertFalse(executed)
        delay(50L)
        assertFalse(executed)
        delay(100L)
        assertTrue(executed)
        
        job.join()
    }

    @Test
    fun `createDebouncedFunction should cancel previous calls`() = runTest {
        var executionCount = 0
        var lastValue = -1
        
        val debouncedFunction = CoroutineUtils.createDebouncedFunction(
            scope = this,
            delayMillis = 100L
        ) { value: Int ->
            executionCount++
            lastValue = value
        }
        
        // Call multiple times quickly
        debouncedFunction(1)
        debouncedFunction(2)
        debouncedFunction(3)
        
        delay(150L)
        
        assertEquals(1, executionCount) // Only last call should execute
        assertEquals(3, lastValue)
    }

    @Test
    fun `retryWithBackoff should retry on failure`() = runTest {
        var attemptCount = 0
        
        val result = CoroutineUtils.retryWithBackoff(
            maxAttempts = 3,
            initialDelay = 10L,
            maxDelay = 100L,
            factor = 2.0
        ) {
            attemptCount++
            if (attemptCount < 3) {
                throw RuntimeException("Attempt $attemptCount failed")
            }
            "Success"
        }
        
        assertEquals("Success", result)
        assertEquals(3, attemptCount)
    }

    @Test
    fun `retryWithBackoff should fail after max attempts`() = runTest {
        var attemptCount = 0
        
        assertFailsWith<RuntimeException> {
            CoroutineUtils.retryWithBackoff(
                maxAttempts = 3,
                initialDelay = 10L
            ) {
                attemptCount++
                throw RuntimeException("Always fails")
            }
        }
        
        assertEquals(3, attemptCount)
    }

    @Test
    fun `withTimeout should succeed within timeout`() = runTest {
        val result = CoroutineUtils.withTimeout(1000L) {
            delay(100L)
            "Success"
        }
        
        assertEquals("Success", result)
    }

    @Test
    fun `withTimeout should fail on timeout`() = runTest {
        assertFailsWith<TimeoutCancellationException> {
            CoroutineUtils.withTimeout(100L) {
                delay(200L)
                "Success"
            }
        }
    }

    @Test
    fun `debounced function should handle rapid calls correctly`() = runTest {
        var executionCount = 0
        var lastValue = -1
        
        val debouncedFunction = CoroutineUtils.createDebouncedFunction(
            scope = this,
            delayMillis = 50L
        ) { value: Int ->
            executionCount++
            lastValue = value
        }
        
        // Rapid calls
        repeat(10) { i ->
            debouncedFunction(i)
            delay(10L)
        }
        
        delay(100L) // Wait for debounce
        
        assertEquals(1, executionCount)
        assertEquals(9, lastValue)
    }

    @Test
    fun `retry with exponential backoff should use correct delays`() = runTest {
        val startTime = System.currentTimeMillis()
        var attemptCount = 0
        
        try {
            CoroutineUtils.retryWithBackoff(
                maxAttempts = 4,
                initialDelay = 50L,
                maxDelay = 500L,
                factor = 2.0
            ) {
                attemptCount++
                throw RuntimeException("Always fails")
            }
        } catch (e: RuntimeException) {
            // Expected to fail
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        assertEquals(4, attemptCount)
        
        // Should have delays: 50ms + 100ms + 200ms = 350ms minimum
        assertTrue(totalTime >= 300)
    }
}

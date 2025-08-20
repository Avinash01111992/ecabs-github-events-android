package com.ecabs.events.util

import java.time.Duration
import java.time.Instant

/**
 * Utility class for time-related operations
 */
object TimeUtils {
    
    /**
     * Formats ISO UTC timestamp to relative time (e.g., "3h ago", "2d ago")
     * 
     * @param isoUtc ISO 8601 formatted UTC timestamp
     * @return Human-readable relative time string
     */
    fun formatRelativeTime(isoUtc: String): String {
        return try {
            val then = Instant.parse(isoUtc)
            val now = Instant.now()
            val seconds = Duration.between(then, now).seconds.coerceAtLeast(0)
            
            when {
                seconds < 60 -> "${seconds}s ago"
                seconds < 60 * 60 -> "${seconds / 60}m ago"
                seconds < 60 * 60 * 24 -> "${seconds / 3600}h ago"
                else -> "${seconds / 86400}d ago"
            }
        } catch (_: Exception) {
            isoUtc
        }
    }
    
    /**
     * Formats duration in seconds to human-readable format
     * 
     * @param seconds Duration in seconds
     * @return Formatted duration string
     */
    fun formatDuration(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 60 * 60 -> "${seconds / 60}m"
            seconds < 60 * 60 * 24 -> "${seconds / 3600}h"
            else -> "${seconds / 86400}d"
        }
    }
}

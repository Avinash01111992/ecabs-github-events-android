package com.ecabs.events.util

object Constants {
    object Network {
        const val BASE_URL = "https://api.github.com/"
        const val AUTHORIZATION_HEADER = "Authorization"
        const val TOKEN_PREFIX = "token "
        const val ACCEPT_HEADER = "Accept"
        const val GITHUB_API_VERSION = "application/vnd.github.v3+json"
        const val USER_AGENT_HEADER = "User-Agent"
        const val USER_AGENT_VALUE = "GitHub-Events-Android"
        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 30L
        const val WRITE_TIMEOUT = 30L
    }
    
    object Headers {
        const val ETAG = "ETag"
        const val POLL_INTERVAL = "X-Poll-Interval"
    }
    
    object HttpStatus {
        const val NOT_MODIFIED = 304
    }
    
    object Timeouts {
    const val DEFAULT_POLL_INTERVAL = 10
    const val ERROR_RETRY_DELAY = 5000L
    const val SEARCH_DEBOUNCE_DELAY = 300L
    const val ERROR_AUTO_CLEAR_DELAY = 5000L
    const val RETRY_MAX_ATTEMPTS = 3
    const val RETRY_INITIAL_DELAY = 1000L
    const val RETRY_MAX_DELAY = 10000L
    const val RETRY_BACKOFF_FACTOR = 2.0
}
    
    object UI {
        const val EVENT_INFO_ICON = "📋"
        const val ACTOR_INFO_ICON = "👤"
        const val EVENT_DETAILS_ICON = "🔍"
        const val ACTIONS_ICON = "🚀"
        const val VIEW_PROFILE_ICON = "👤"
        const val OPEN_REPO_ICON = "📁"
        
        const val APP_TITLE = "GitHub Events"
        const val NO_EVENTS_MESSAGE = "No events yet"
        const val NO_EVENTS_SUBTITLE = "Try again in a few seconds."
        const val RETRY_BUTTON = "Retry"
        const val NO_EVENT_DATA = "No event data"
        const val NEXT_REFRESH_PREFIX = "Next refresh: "
        const val SEARCH_PLACEHOLDER = "Search events"
        const val SCROLL_TO_TOP_DESCRIPTION = "Scroll to top"
        const val BACK_BUTTON_DESCRIPTION = "Back"
        const val SHARE_REPO_DESCRIPTION = "Share repo"
        const val EVENT_DETAILS_TITLE_PREFIX = "Event Details ("
        const val EVENT_DETAILS_TITLE_SUFFIX = ")"
    }
}

# eCabs GitHub Events (Android)

Android app using **Kotlin + Compose + Hilt (Dagger) + Retrofit + Coroutines** to show GitHub public events.

## Goals covered
- Two screens: list + details.
- List reacts to scroll (shows FAB to scroll to top) and item click navigates to details.
- Polls for new events; merges into list without duplicates.
- Filters to 5 event types: **PushEvent, PullRequestEvent, IssuesEvent, ForkEvent, WatchEvent**.
- Uses **ETag** and **X-Poll-Interval** per GitHub Docs.

## Prerequisites
- **Android Studio** Koala or newer (AGP 8.5+)
- **JDK 17** (Gradle/AGP require it)
- Gradle wrapper 8.7 is expected in the repo; if missing locally, generate it via:
  - `gradle wrapper --gradle-version 8.7`

## How polling works
- The ViewModel runs a loop with `delay()`.
- After each fetch, it reads `X-Poll-Interval` (if present) and updates the next interval.
- The loop sleeps for `max(10, X-Poll-Interval)` seconds so we satisfy the challenge (10s) while **never polling faster than recommended**.
- If API returns **304 Not Modified**, we keep the list as-is and don't consume rate limit.

## Run it
1. Open in Android Studio → set Gradle JDK to JDK 17 if prompted.
2. (Optional) Add `GITHUB_TOKEN=ghp_xxx` to `~/.gradle/gradle.properties` (or project `gradle.properties`) to increase rate limits.
3. Sync and run on an emulator/device.

## API and event types
- Endpoint: **GET `/events`** (List public events) with ETag-based polling. See GitHub docs: https://docs.github.com/en/rest/activity/events?apiVersion=2022-11-28
- Shown event types (5 of ~50): **PushEvent, PullRequestEvent, IssuesEvent, ForkEvent, WatchEvent**
  - You can change them in `app/src/main/java/com/ecabs/events/data/model/GitHubEvent.kt` (`TrackedEventType`).

## Notes / Future work
- Add paging (per_page + page) to browse more events.
- Persist cache with Room for offline.
- Render event-specific `payload` fields in details.
- Add more unit tests and UI tests.

## Troubleshooting
- Toolchain/JDK: If Gradle cannot find Java 17, point it to your JDK 17 (e.g. `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`).
- Resource linking: Ensure Material 3 theme parent exists and launcher icons are present.
- Rate limiting: Use a `GITHUB_TOKEN` to reduce 403s.

## AI usage
Some scaffolding was generated with assistance and then curated manually.
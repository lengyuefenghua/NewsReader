# AGENTS.md

## Purpose

This file is for agentic coding agents working in `D:\Code\AndroidApp\NewsReader`.
It summarizes how to build, test, inspect, and modify this Android project safely.

## Development Guide

Read `docs/DevelopmentGuide.md` before starting feature work, debugging, or architectural analysis.

That guide is the primary technical handoff document for AI-assisted development and covers:

- tech stack
- system architecture
- feature overview
- core call relationships
- feature-to-file mapping
- task-oriented file entry points

## Repository Snapshot

- App name: `NewsReader`
- Platform: Android
- Language: Kotlin
- UI: Jetpack Compose Material3
- Architecture: Compose UI -> ViewModel -> Repository -> Room/network
- Dependency injection: Koin
- Persistence: Room + DataStore + SharedPreferences
- Networking/parsing: OkHttp, Jsoup, custom RSS/HTML parsers
- Module layout: single app module `:app`
- Application ID: `com.lengyuefenghua.newsreader`
- SDKs: `compileSdk 36`, `targetSdk 36`, `minSdk 28`

## Critical Workflow Rules

These rules are repository-specific and must be followed before editing code or config.

1. Read the target files first.
2. Present a concrete change plan.
3. Ask for explicit user confirmation.
4. Only edit after the user confirms.
5. If the user changes scope, re-confirm before editing.
6. For UI or layout adjustments, provide a text-based UI sketch or layout diagram before editing to avoid alignment misunderstandings.

Allowed without extra confirmation:

- Reading files
- Searching the codebase
- Explaining architecture or behavior
- Investigating issues

Do not run builds or tests unless the user asked for them.
Do not change code, dependencies, database schema, or git state without confirmation.

After making code changes and completing the relevant tests, automatically run `./run-debug.sh` as the final verification script.
This repository treats `./run-debug.sh` as the default end-to-end verification step for ordinary code changes.

## Rule Files Present

- Existing `AGENTS.md`: this file
- `.cursorrules`: not present
- `.cursor/rules/`: not present
- `.github/copilot-instructions.md`: not present

There are currently no Cursor or Copilot rule files to merge into this document.

## Project Structure

- `app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt`: Compose app shell and navigation
- `app/src/main/java/com/lengyuefenghua/newsreader/NewsReaderApplication.kt`: app startup and initialization
- `app/src/main/java/com/lengyuefenghua/newsreader/core/di/`: Koin modules
- `app/src/main/java/com/lengyuefenghua/newsreader/data/`: Room entities, DAO, repository, sync logic
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/`: screen-level state and actions
- `app/src/main/java/com/lengyuefenghua/newsreader/ui/`: screens, components, UI state
- `app/src/main/java/com/lengyuefenghua/newsreader/utils/`: parsers, WebView helpers, logging helpers
- `app/src/main/assets/algorithm/`: extraction scripts used by article parsing
- `app/src/test/`: JVM unit tests
- `app/src/androidTest/`: instrumentation tests

## Build Commands

Run from repository root.

```bash
./gradlew build
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
```

Notes:

- Debug and release are both signed with the debug keystore.
- Release enables R8 minification and resource shrinking.
- APK output file name is forced to `NewsReader.apk`.

## Lint Commands

```bash
./gradlew lint
./gradlew :app:lint
./gradlew lintDebug
./gradlew lintRelease
```

Notes:

- Lint is configured as non-blocking: `abortOnError = false`.
- Do not assume a successful lint run means there are no warnings.

## Test Commands

### All JVM unit tests

```bash
./gradlew testDebugUnitTest
./gradlew :app:testDebugUnitTest
```

### Run a single test class

```bash
./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ExampleUnitTest"
./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.data.RefreshPlannerTest"
```

### Run a single test method

```bash
./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ExampleUnitTest.addition_isCorrect"
./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.data.RefreshPlannerTest.fast lane should prioritize first batch and split remaining"
```

### All instrumentation tests

```bash
./gradlew connectedDebugAndroidTest
```

### Run a single instrumentation test class

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lengyuefenghua.newsreader.ExampleInstrumentedTest
```

### Run a single instrumentation test method

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lengyuefenghua.newsreader.ExampleInstrumentedTest#useAppContext
```

### Install and launch after build

```bash
./gradlew installDebug
adb shell am start -n com.lengyuefenghua.newsreader/.MainActivity
```

## Verification Guidance

- For logic-only changes, prefer targeted JVM unit tests first.
- For Compose/UI/navigation changes, consider `./run-debug.sh` plus relevant manual or instrumentation validation.
- For parser/repository changes, run the most specific unit tests that cover the changed path.
- After code changes and test verification, run `./run-debug.sh` before reporting completion.
- If no automated test exists, state that clearly in your final summary.

## Architecture Notes

- `NewsReaderApplication` sets up Koin, a global exception handler, `WebViewManager`, and settings repositories.
- `MainActivity` owns the root Compose navigation graph.
- ViewModels expose long-lived screen state via `StateFlow`.
- One-off UI actions use `Channel` plus `UiEvent` flows.
- `NewsRepository` orchestrates syncing and database access.
- Room entities are `Source` and `Article`.
- `SettingsManager` stores shared preference values like refresh concurrency.
- `UserPreferencesRepository` uses DataStore for user preferences.

## Code Style Guidelines

The repository generally follows Kotlin official style (`kotlin.code.style=official`), but some files are inconsistent. Prefer the cleaner existing patterns below when making new edits.

## Imports

- Keep imports explicit; avoid wildcard imports.
- Group imports in the default Kotlin style used by the formatter.
- Remove unused imports when touching a file.
- Prefer stable import ordering from the IDE/formatter rather than hand-crafted grouping.

## Formatting

- Use 4 spaces for indentation.
- Keep line breaks readable rather than aggressively compact.
- Prefer trailing commas only if they match the surrounding file style.
- Keep chained builder calls and long Compose calls split across lines.
- Preserve the dominant style in the file you are editing instead of reformatting unrelated code.

## Naming

- Types and objects: `PascalCase`
- Functions, properties, local variables, parameters: `camelCase`
- Constants: `UPPER_SNAKE_CASE` when truly constant, as in repository user-agent strings
- Enums/sealed variants: `PascalCase`
- Test names may use backtick sentences for readability

Examples from the codebase:

- `TimelineViewModel`
- `syncAllTwoPhase`
- `currentSourceName`
- `UA_ANDROID`

## Types And APIs

- Prefer concrete Kotlin types at public boundaries.
- Use nullable types only when `null` is a real domain state.
- Prefer `data class` for immutable state holders.
- Prefer sealed classes for finite result/event/state models, such as `Result` and `UiEvent`.
- Avoid unnecessary abstraction layers in this codebase; most logic lives directly in repositories and viewmodels.

## State Management

- UI state should be exposed from ViewModels via `StateFlow`.
- One-shot events should use `Channel` and `receiveAsFlow()`.
- Prefer deriving state with Flow operators like `map`, `combine`, and `flatMapLatest`.
- Keep Compose screens mostly declarative and push business logic into ViewModels or repository classes.

## Coroutines

- Keep database and network work off the main thread.
- Repository methods that touch Room or network should use `withContext(Dispatchers.IO)`.
- Launch UI-driven async work in `viewModelScope`.
- Avoid blocking calls in composables.
- When adding concurrency, keep it bounded and explicit; existing sync code uses `Semaphore`.

## Compose Guidelines

- Screens are top-level composables under `ui/screens/`.
- Reusable UI pieces go under `ui/components/`.
- Prefer state hoisting: pass callbacks and state into composables instead of hiding behavior in the UI layer.
- Use `collectAsState()` for `StateFlow` consumption in screens.
- Keep navigation decisions near the app shell or route owner.
- Use `stringResource()` or Android resources for reusable content descriptions where practical.

## ViewModel Guidelines

- ViewModels should coordinate UI state, repository calls, and UI events.
- Do not put Android UI widget code in ViewModels.
- Prefer small public methods named after user actions, such as `refresh()`, `showSource()`, or `toggleFavorite()`.
- Expose immutable state with `asStateFlow()` where applicable.

## Repository And Data Layer Guidelines

- `NewsRepository` is the main orchestration layer for sync and article/source access.
- Keep parsing and persistence concerns together only when already established by the file structure.
- Prefer returning domain-friendly results instead of throwing unchecked exceptions across layers.
- Map exceptional states into `Result.Error` with a user-facing message when possible.

## Error Handling

- Catch and classify network failures where user-facing messaging matters.
- Existing code maps `UnknownHostException` and `SocketTimeoutException` into `NetworkError` variants.
- Use `Result.Success` / `Result.Error` for recoverable operations.
- Only throw when the caller is expected to handle or aggregate the failure.
- Log meaningful context on failures, but avoid swallowing errors silently unless returning an intentional fallback like `emptyList()`.

## Room And Persistence

- Keep Room entities in `data/`.
- `Article.id` is URL-based and used for deduplication semantics.
- Database version is currently `10`.
- `fallbackToDestructiveMigration()` is enabled.

This is important:

- A schema version bump can wipe local data.
- Do not change schema casually.
- If schema changes are required, call out migration and destructive-migration risk explicitly in the plan.

## Settings And Preferences

- `UserPreferencesRepository` handles DataStore-backed preferences.
- `SettingsManager` handles SharedPreferences-backed settings.
- Refresh concurrency is clamped to `1..5`; keep that constraint intact unless intentionally changing behavior.

## Logging

- Follow existing logging utilities where a file already uses them, such as `LogUtils`.
- Prefer concise logs with useful context like source name, phase, or failing operation.
- Do not add noisy logging in hot paths unless debugging a specific issue.

## Testing Style

- Unit tests use JUnit4.
- Backtick test names are acceptable and already used.
- Keep tests deterministic and focused on one behavior.
- For planner and repository-style logic, prefer plain JVM tests over instrumentation where possible.
- When testing event order or multi-phase workflows, assert on sequence and summary values.

## What To Avoid

- Do not perform broad refactors without user approval.
- Do not reformat unrelated files.
- Do not replace existing architecture with new frameworks or patterns unnecessarily.
- Do not introduce wildcard imports.
- Do not move business logic into composables when a ViewModel or repository is the correct place.
- Do not assume lint is blocking.

## Agent Working Style In This Repo

- Read first, then plan, then ask for confirmation, then edit.
- Prefer minimal diffs.
- Respect existing dirty worktrees; do not revert unrelated changes.
- If you see surprising local changes, work around them unless they conflict directly with the requested task.
- When summarizing work, mention files changed, verification performed, and any unverified risk.

## Useful Paths

- `CLAUDE.md`
- `app/build.gradle.kts`
- `app/src/main/java/com/lengyuefenghua/newsreader/MainActivity.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/NewsReaderApplication.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/data/NewsRepository.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/viewmodel/TimelineViewModel.kt`
- `app/src/main/java/com/lengyuefenghua/newsreader/data/AppDatabase.kt`

## Bottom Line

This is a single-module Kotlin Android app with Compose, Room, coroutines, and Koin.
Work conservatively, preserve layering, keep IO off the main thread, and never edit before explicit user confirmation.

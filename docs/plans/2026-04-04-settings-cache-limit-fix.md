# Settings Cache Limit Fix Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix the Settings screen so the article retention value reflects the persisted value after save and after screen re-entry.

**Architecture:** Keep the current `BasicTextField` interaction model, but explicitly sync the local draft text with the persisted `cacheLimit` flow whenever the stored value changes. Do not change the concurrent refresh setting flow; only verify that it still feeds `TimelineViewModel` and `NewsRepository.syncAll()`.

**Tech Stack:** Kotlin, Jetpack Compose, Android DataStore, JUnit4

---

### Task 1: Cache Limit Draft Sync

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/SettingsScreen.kt`
- Test: `app/src/test/java/com/lengyuefenghua/newsreader/ui/screens/SettingsScreenStateTest.kt`

**Step 1: Write the failing test**

```kotlin
@Test
fun `stale cache limit draft should sync to persisted value`() {
    assertEquals("200", syncCacheLimitDraft(currentDraft = "1000", persistedLimit = 200))
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.SettingsScreenStateTest"`
Expected: FAIL with unresolved reference for `syncCacheLimitDraft`

**Step 3: Write minimal implementation**

```kotlin
internal fun syncCacheLimitDraft(currentDraft: String, persistedLimit: Int): String {
    val persistedText = persistedLimit.toString()
    return if (currentDraft == persistedText) currentDraft else persistedText
}
```

Also add a `LaunchedEffect(cacheLimit)` in `SettingsScreen` to apply the synced draft text.

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.SettingsScreenStateTest"`
Expected: PASS

**Step 5: Verify related behavior**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.ArticleScreenModeTest"`
Expected: PASS

**Step 6: Final verification**

Run: `./run-debug.sh`
Expected: build, install, and launch complete successfully on the connected device/emulator

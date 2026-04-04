# Settings Cache Limit Slider Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the article retention text input with a discrete slider that snaps to `10`, `50`, `100`, `500`, and `1000`.

**Architecture:** Keep the existing settings screen structure and replace only the cache-limit editor row. Add small pure helper functions to map persisted values to slider anchor indices and back so the behavior is testable without Compose UI tests.

**Tech Stack:** Kotlin, Jetpack Compose Material3, JUnit4

---

### Task 1: Cache Limit Slider State Helpers

**Files:**
- Modify: `app/src/test/java/com/lengyuefenghua/newsreader/ui/screens/SettingsScreenStateTest.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/SettingsScreen.kt`

**Step 1: Write the failing test**

```kotlin
@Test
fun `cache limit should snap to nearest slider anchor`() {
    assertEquals(100, snapCacheLimitToAnchor(120))
    assertEquals(1000, snapCacheLimitToAnchor(999))
}

@Test
fun `article retention field should use slider instead of outlined text field`() {
    assertFalse(shouldUseOutlinedCacheLimitField())
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.SettingsScreenStateTest"`
Expected: FAIL because `snapCacheLimitToAnchor` is missing and the outlined text field assertion no longer matches the desired behavior.

**Step 3: Write minimal implementation**

Add pure helpers for:
- slider anchors list
- nearest-anchor snapping
- slider index to cache-limit value mapping
- cache-limit value to slider index mapping

Flip the UI mode helper to indicate the text field is no longer used.

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.SettingsScreenStateTest"`
Expected: PASS

### Task 2: Replace Text Input With Slider UI

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/SettingsScreen.kt`

**Step 1: Update the settings row**

Replace the article retention `OutlinedTextField` with:
- current snapped value text
- `Slider` using anchor index range `0..4`
- `steps = 3` for five discrete positions

**Step 2: Keep persisted state in sync**

Use the persisted `cacheLimit` flow to derive the current slider position. On slider change, write back the anchor value through `viewModel.setCacheLimit(...)`.

**Step 3: Run the focused test suite**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.SettingsScreenStateTest"`
Expected: PASS

**Step 4: Final verification**

Run: `./run-debug.sh`
Expected: build, install, and launch complete successfully on the connected device/emulator

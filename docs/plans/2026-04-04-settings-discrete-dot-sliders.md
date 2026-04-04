# Settings Discrete Dot Sliders Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Use the same discrete dot slider style for article retention and concurrent refresh, and document a rule requiring text UI sketches before future layout changes.

**Architecture:** Keep the existing settings screen layout and introduce one reusable discrete slider composable for small fixed anchor sets. Back the composable with pure helper functions that snap persisted values to anchor lists so both controls remain testable without Compose UI tests.

**Tech Stack:** Kotlin, Jetpack Compose Material3, JUnit4

---

### Task 1: Add state helper coverage

**Files:**
- Modify: `app/src/test/java/com/lengyuefenghua/newsreader/ui/screens/SettingsScreenStateTest.kt`
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/SettingsScreen.kt`

**Step 1: Write the failing test**

```kotlin
@Test
fun `concurrent refresh should snap to nearest slider anchor`() {
    assertEquals(1, snapConcurrentCountToAnchor(0))
    assertEquals(5, snapConcurrentCountToAnchor(7))
}

@Test
fun `concurrent refresh slider index should map back to anchor value`() {
    assertEquals(4, concurrentCountFromSliderIndex(3f))
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.SettingsScreenStateTest"`
Expected: FAIL because the new concurrent slider helpers do not exist yet.

**Step 3: Write minimal implementation**

Add pure helpers for concurrent refresh anchors `1..5`, plus a generic discrete-dot slider composable that consumes anchor indices.

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.SettingsScreenStateTest"`
Expected: PASS

### Task 2: Apply the discrete dot slider UI

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/SettingsScreen.kt`

**Step 1: Replace the concurrent refresh plus/minus UI**

Show the current count first, then a discrete slider with five large circular anchor dots and a larger thumb.

**Step 2: Upgrade the article retention slider to the same style**

Keep the current value text first, remove anchor labels, and render the same five-dot presentation for `10 / 50 / 100 / 500 / 1000`.

**Step 3: Verify focused tests**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.SettingsScreenStateTest"`
Expected: PASS

### Task 3: Update repo rule for UI changes

**Files:**
- Modify: `AGENTS.md`

**Step 1: Add the workflow rule**

Add a rule requiring a text-based UI sketch or layout diagram before editing files for UI/layout changes.

**Step 2: Final verification**

Run: `./run-debug.sh`
Expected: build, install, and launch complete successfully on the connected device/emulator

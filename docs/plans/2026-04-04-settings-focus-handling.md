# Settings Focus Handling Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the article retention input lose focus when the user taps blank space, taps other settings controls, or presses the IME done action.

**Architecture:** Keep the existing `BasicTextField`, add Compose focus management around it, and centralize focus-clearing triggers in the settings screen. Use a small pure helper for the IME action policy so the regression test stays lightweight.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit4

---

### Task 1: Settings Focus Clearing

**Files:**
- Modify: `app/src/main/java/com/lengyuefenghua/newsreader/ui/screens/SettingsScreen.kt`
- Test: `app/src/test/java/com/lengyuefenghua/newsreader/ui/screens/SettingsScreenStateTest.kt`

**Step 1: Write the failing test**

```kotlin
@Test
fun `ime done should request clearing cache limit focus`() {
    assertTrue(shouldClearCacheLimitFocusOnImeDone())
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.SettingsScreenStateTest"`
Expected: FAIL with unresolved reference for `shouldClearCacheLimitFocusOnImeDone`

**Step 3: Write minimal implementation**

Add:
- `LocalFocusManager`
- outer tap handling for blank space
- focus clearing in non-textfield controls
- IME done action that clears focus

**Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.lengyuefenghua.newsreader.ui.screens.SettingsScreenStateTest"`

**Step 5: Final verification**

Run: `./run-debug.sh`
Expected: build, install, and launch complete successfully on the connected device/emulator

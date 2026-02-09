## ADDED Requirements

### Requirement: Smart scroll to top on new articles
The system SHALL automatically scroll the article list to top position when a subscription source refresh completes with new articles, but remain at current position when no new articles are found.

#### Scenario: Scroll to top when new articles arrive
- **WHEN** subscription source "TechNews" completes refresh with 5 new articles
- **THEN** system triggers scroll to top of article list
- **AND** user sees newly fetched articles at top of screen
- **AND** scroll animation is smooth (300ms duration)

#### Scenario: No scroll when no new articles
- **WHEN** subscription source "OldBlog" completes refresh with 0 new articles
- **THEN** system does NOT trigger scroll to top
- **AND** user remains at current scroll position
- **AND** user continues reading without interruption

#### Scenario: Multiple sources with mixed results
- **WHEN** source A completes with 0 new articles
- **AND** source B completes with 3 new articles
- **THEN** system does NOT scroll after source A
- **AND** system DOES scroll to top after source B

### Requirement: Scroll throttling to prevent jitter
The system SHALL throttle scroll-to-top actions to prevent rapid repeated scrolling when multiple sources complete in quick succession.

#### Scenario: Multiple sources complete simultaneously
- **WHEN** 3 sources complete within 500ms window
- **AND** all have new articles
- **THEN** system triggers scroll to top only once
- **AND** scroll occurs after all 3 sources have completed

#### Scenario: Staggered source completion
- **WHEN** source A completes at t=0ms with new articles
- **AND** source B completes at t=600ms with new articles
- **THEN** system scrolls to top after source A (t=0ms)
- **AND** system scrolls to top again after source B (t=600ms)

### Requirement: Scroll only during active refresh
The system SHALL only trigger automatic scroll during user-initiated refresh, not during background sync or manual filter changes.

#### Scenario: Auto-refresh in background
- **WHEN** application performs scheduled background refresh
- **AND** new articles are fetched
- **THEN** system does NOT scroll to top
- **AND** user's current reading position is preserved

#### Scenario: User manually triggers refresh
- **WHEN** user taps refresh button
- **AND** new articles arrive
- **THEN** system scrolls to top

## ADDED Requirements

### Requirement: Incremental feed refresh
The system SHALL update the timeline UI immediately after each individual feed source is refreshed, rather than waiting for all feeds to complete.

#### Scenario: Timeline updates after each feed refresh
- **WHEN** user triggers refresh for multiple subscription sources
- **THEN** system refreshes feeds sequentially
- **AND** timeline updates immediately after each feed completes
- **AND** user sees new articles appear progressively

#### Scenario: Partial refresh visibility during network errors
- **WHEN** user refreshes 5 subscription sources
- **AND** the 3rd source fails due to network error
- **THEN** timeline shows articles from sources 1, 2, 4, and 5
- **AND** error is reported for source 3
- **AND** refresh completes without blocking on failed source

### Requirement: Refresh progress tracking
The system SHALL track which sources have been refreshed and provide callbacks for incremental UI updates.

#### Scenario: Progress callback invocation
- **WHEN** a feed source refresh completes
- **THEN** system invokes progress callback with source name and article count
- **AND** callback includes success/failure status
- **AND** callback includes number of new articles added

#### Scenario: All refreshes complete notification
- **WHEN** all feed sources have been refreshed
- **THEN** system invokes final completion callback
- **AND** callback includes total new articles across all sources
- **AND** callback includes total sources refreshed
- **AND** callback includes number of failed sources (if any)

### Requirement: Refresh state management
The system SHALL maintain refresh state to prevent duplicate refresh operations and handle concurrent refresh requests.

#### Scenario: Prevent duplicate refresh
- **WHEN** user clicks refresh button while refresh is in progress
- **THEN** system ignores the duplicate request
- **AND** existing refresh operation continues
- **AND** refresh indicator remains active

#### Scenario: Concurrent refresh handling
- **WHEN** automatic refresh triggers while manual refresh is in progress
- **THEN** system queues the automatic refresh
- **AND** automatic refresh starts after manual refresh completes

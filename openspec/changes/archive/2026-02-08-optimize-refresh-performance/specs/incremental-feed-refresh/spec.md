## ADDED Requirements

### Requirement: Concurrent feed refresh execution
The system SHALL refresh subscription sources concurrently using multiple coroutines, controlled by user-configured concurrent count.

#### Scenario: Concurrent refresh with 3 workers
- **WHEN** user has 9 subscription sources and concurrent count set to 3
- **THEN** system launches 3 concurrent refresh coroutines
- **AND** each coroutine independently fetches and saves articles
- **AND** when any coroutine completes, next source starts immediately
- **AND** total refresh time is approximately 1/3 of sequential time

#### Scenario: Concurrent refresh progress tracking
- **WHEN** multiple sources refresh concurrently
- **THEN** system tracks completion count separately for each source
- **AND** system invokes progress callback when each source completes
- **AND** progress callback includes source name, success status, and new article count

## MODIFIED Requirements

### Requirement: Incremental refresh with concurrent execution
The system SHALL update the timeline UI incrementally as each concurrent refresh completes, while maintaining user-configured concurrency limit.

#### Scenario: Timeline updates after concurrent source refresh
- **WHEN** 3 sources refresh concurrently
- **AND** source A completes first with 2 new articles
- **THEN** system updates article list immediately with source A's articles
- **AND** system does NOT wait for sources B and C to complete
- **AND** user sees new articles appear progressively

#### Scenario: Concurrent refresh with smart scrolling
- **WHEN** source A completes with 3 new articles while source B is still refreshing
- **THEN** system triggers scroll to top after source A completes
- **AND** scroll behavior respects throttling rules (see smart-scroll-on-refresh spec)

### Requirement: Refresh state management with concurrency
The system SHALL manage concurrent refresh state to prevent exceeding configured worker limit and handle partial failures gracefully.

#### Scenario: Worker pool management
- **WHEN** concurrent count is set to 3
- **THEN** system maintains maximum 3 active refresh workers
- **AND** when worker completes, next source starts immediately
- **AND** system never exceeds 3 concurrent workers

#### Scenario: Partial failure during concurrent refresh
- **WHEN** 5 sources refresh concurrently
- **AND** source 3 fails due to network error
- **THEN** other sources (1, 2, 4, 5) continue refreshing
- **AND** timeline shows articles from successful sources
- **AND** final summary includes 1 failed source

## REMOVED Requirements

### Requirement: Sequential refresh execution
**Reason**: Replaced by concurrent refresh to improve performance and reduce total refresh time.

**Migration**: Replace sequential `for` loop with `async`/`awaitAll` pattern limited by semaphore or custom dispatcher. Maintain progress callback mechanism.

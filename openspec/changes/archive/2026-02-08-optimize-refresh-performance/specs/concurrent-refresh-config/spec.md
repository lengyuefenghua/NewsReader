## ADDED Requirements

### Requirement: Concurrent refresh configuration
The system SHALL provide a configurable concurrent refresh setting that allows users to specify the number of subscription sources to refresh simultaneously.

#### Scenario: Default concurrent count
- **WHEN** user installs the application for the first time
- **THEN** system sets default concurrent refresh count to 3
- **AND** setting is persisted in SharedPreferences

#### Scenario: User configures concurrent count
- **WHEN** user selects concurrent count of 5 in settings
- **THEN** system saves the value to SharedPreferences
- **AND** system validates the value is within range 1-5
- **AND** next refresh operation uses 5 concurrent workers

#### Scenario: Invalid concurrent count handling
- **WHEN** system reads invalid concurrent count from storage
- **THEN** system falls back to default value of 3
- **AND** system does not crash or throw exception

### Requirement: Concurrent refresh range validation
The system SHALL restrict concurrent refresh count to range 1-5 to prevent excessive resource usage.

#### Scenario: Minimum value of 1 (sequential refresh)
- **WHEN** user sets concurrent count to 1
- **THEN** system refreshes sources sequentially (one at a time)
- **AND** this is equivalent to previous serial refresh behavior

#### Scenario: Maximum value of 5
- **WHEN** user attempts to set concurrent count greater than 5
- **THEN** system clamps the value to 5
- **AND** system displays warning about potential resource usage

### Requirement: Concurrent refresh execution
The system SHALL refresh subscription sources concurrently using the configured number of workers.

#### Scenario: Concurrent refresh with 3 workers
- **WHEN** user has 9 subscription sources and concurrent count set to 3
- **THEN** system refreshes 3 sources simultaneously
- **AND** when any source completes, next source starts immediately
- **AND** total refresh time is approximately 1/3 of sequential time

#### Scenario: Concurrent refresh with fewer sources than workers
- **WHEN** user has 2 subscription sources and concurrent count set to 5
- **THEN** system only refreshes 2 sources concurrently
- **AND** idle workers are not created

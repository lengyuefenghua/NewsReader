## ADDED Requirements

### Requirement: Default filter shows unread articles
The system SHALL initialize the timeline filter state based on user's default filter setting when the timeline screen is first displayed.

#### Scenario: First launch shows unread articles
- **WHEN** user opens the application for the first time
- **AND** default filter setting is not configured (uses factory default)
- **THEN** timeline displays only unread articles
- **AND** the filter indicator shows "未读" (Unread) as selected

#### Scenario: User configured default filter setting
- **WHEN** user has set default filter to "已读" (Read) in settings
- **AND** user opens the application
- **THEN** timeline displays only read articles
- **AND** the filter indicator shows "已读" (Read) as selected

#### Scenario: User configured default filter to all
- **WHEN** user has set default filter to "全部" (All) in settings
- **AND** user opens the application
- **THEN** timeline displays all articles (both read and unread)
- **AND** the filter indicator shows "全部" (All) as selected

### Requirement: Filter state management
The system SHALL manage filter state using StateFlow and persist user selection across screen rotations and app restarts.

#### Scenario: Filter change persists across configuration changes
- **WHEN** user selects "已读" (Read) filter
- **AND** device screen rotates
- **THEN** timeline continues to show "已读" (Read) articles
- **AND** filter selection remains "已读" (Read)

#### Scenario: Filter change persists across app restarts
- **WHEN** user selects "全部" (All) filter
- **AND** user closes and reopens the application
- **THEN** timeline displays "全部" (All) articles
- **AND** filter selection remains "全部" (All)

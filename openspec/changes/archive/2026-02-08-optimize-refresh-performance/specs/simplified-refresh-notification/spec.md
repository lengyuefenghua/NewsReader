## ADDED Requirements

### Requirement: Single summary notification
The system SHALL display only one Toast notification after all subscription sources have completed refreshing, showing total new article count. Individual per-source notifications SHALL be removed.

#### Scenario: Successful refresh with new articles
- **WHEN** all subscription sources complete refresh
- **AND** total 12 new articles were added
- **THEN** system displays single Toast: "刷新完成,共更新 12 篇新文章"
- **AND** Toast displays for 3 seconds
- **AND** no per-source Toast notifications are shown

#### Scenario: Refresh with no new articles
- **WHEN** all subscription sources complete refresh
- **AND** 0 new articles were found
- **THEN** system displays single Toast: "刷新完成,无新文章"
- **AND** Toast displays for 2 seconds

#### Scenario: Partial failure summary
- **WHEN** 8 sources refreshed successfully
- **AND** 2 sources failed to refresh
- **AND** 15 new articles were added
- **THEN** system displays single Toast: "刷新完成,更新 15 篇新文章,2 个源失败"
- **AND** Toast displays for 3 seconds

### Requirement: Notification message clarity
The system SHALL format summary notification messages to be concise and informative.

#### Scenario: Singular and plural handling
- **WHEN** exactly 1 new article is added
- **THEN** message displays "刷新完成,共更新 1 篇新文章"
- **WHEN** 0 or multiple articles are added
- **THEN** message displays "刷新完成,共更新 N 篇新文章" (plural form)

#### Scenario: All sources failed
- **WHEN** all subscription sources fail to refresh
- **THEN** system displays Toast: "刷新失败,请检查网络连接"
- **AND** Toast displays for 3 seconds

### Requirement: Notification timing
The system SHALL display summary notification immediately after last source completes, without additional delay.

#### Scenario: Immediate summary display
- **WHEN** last subscription source completes refresh
- **THEN** system displays summary Toast within 100ms
- **AND** user sees immediate feedback

### Requirement: No intermediate notifications
The system SHALL NOT display any progress notifications during refresh, only the final summary.

#### Scenario: No per-source Toast during refresh
- **WHEN** subscription source completes refresh with new articles
- **THEN** system does NOT display per-source Toast
- **AND** user only sees final summary after all sources complete

## REMOVED Requirements

### Requirement: Per-source refresh notification
**Reason**: Replaced by single summary notification to reduce user distraction and notification fatigue.

**Migration**: Remove handling of `UiEvent.SourceRefreshed` events in TimelineScreen. Display only `UiEvent.RefreshCompleted` events.

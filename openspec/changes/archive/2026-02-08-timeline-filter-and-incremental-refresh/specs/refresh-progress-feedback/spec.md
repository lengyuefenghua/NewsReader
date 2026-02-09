## ADDED Requirements

### Requirement: Per-source refresh notification
The system SHALL display a Toast message after each individual feed source is refreshed, showing the source name and number of new articles.

#### Scenario: Successful single source refresh notification
- **WHEN** a feed source refresh completes successfully
- **AND** 5 new articles are added
- **THEN** system displays Toast: "{sourceName}: 已更新 5 篇新文章"
- **AND** Toast displays for 2 seconds
- **AND** Toast appears at bottom of screen

#### Scenario: Zero articles notification
- **WHEN** a feed source refresh completes successfully
- **AND** no new articles are found
- **THEN** system displays Toast: "{sourceName}: 无新文章"
- **AND** Toast displays for 1.5 seconds

#### Scenario: Failed source refresh notification
- **WHEN** a feed source refresh fails
- **THEN** system displays Toast: "{sourceName}: 刷新失败"
- **AND** Toast displays for 2 seconds
- **AND** Toast uses error color styling

### Requirement: Final refresh summary notification
The system SHALL display a summary Toast message after all feed sources have been refreshed, showing total new articles and refresh status.

#### Scenario: Successful batch refresh summary
- **WHEN** all feed sources refresh successfully
- **AND** total 12 new articles were added across 5 sources
- **THEN** system displays Toast: "刷新完成,共更新 12 篇新文章"
- **AND** Toast displays for 3 seconds
- **AND** Toast appears after last per-source notification

#### Scenario: Partial failure summary
- **WHEN** 8 sources refreshed with 2 failures
- **AND** 15 new articles were added
- **THEN** system displays Toast: "刷新完成,更新 15 篇新文章,2 个源失败"
- **AND** Toast displays for 3 seconds
- **AND** Toast uses warning color styling

#### Scenario: All sources failed summary
- **WHEN** all feed sources fail to refresh
- **THEN** system displays Toast: "刷新失败,请检查网络连接"
- **AND** Toast displays for 3 seconds
- **AND** Toast uses error color styling

### Requirement: Toast message formatting
The system SHALL format Toast messages consistently using Chinese language and proper grammar.

#### Scenario: Singular and plural handling
- **WHEN** exactly 1 new article is added
- **THEN** message displays "1 篇新文章" (singular)
- **WHEN** 0 or multiple articles are added
- **THEN** message displays "{count} 篇新文章" (plural form)

#### Scenario: Source name truncation
- **WHEN** feed source name exceeds 20 characters
- **THEN** system truncates name to 17 characters
- **AND** appends "..." to truncated name
- **AND** full name remains visible elsewhere in UI

### Requirement: Notification timing and debouncing
The system SHALL manage Toast display timing to prevent message overlap and ensure readability.

#### Scenario: Rapid refresh completion
- **WHEN** multiple sources refresh within 1 second
- **THEN** system displays per-source Toasts sequentially
- **AND** each Toast displays for full duration
- **AND** Toasts do not overlap or replace each other prematurely

#### Scenario: Final summary timing
- **WHEN** final refresh summary is triggered
- **THEN** system waits for last per-source Toast to complete
- **AND** summary Toast displays after per-source Toasts finish
